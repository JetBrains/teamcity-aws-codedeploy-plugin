/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.util.amazon;

import com.amazonaws.client.builder.ExecutorFactory;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.*;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author vbedrosova
 */
public final class S3Util {

  private static final Logger LOG = Logger.getInstance(S3Util.class.getName());

  public static final String S3_THREAD_POOL_SIZE = "amazon.s3.transferManager.threadPool.size";
  public static final int DEFAULT_S3_THREAD_POOL_SIZE = 10;

  public interface WithTransferManager<T extends Transfer> {
    @NotNull
    Collection<T> run(@NotNull TransferManager manager) throws Throwable;
  }

  public interface TransferManagerInterruptHook {
    void interrupt() throws Throwable;
  }

  public interface InterruptAwareWithTransferManager<T extends Transfer> extends WithTransferManager<T> {
    /**
     * This method is executed after {@link WithTransferManager#run(TransferManager)} is completed,
     * aiming to stop current execution
     *
     * @param hook a callback to interrupt
     */
    void setInterruptHook(@NotNull TransferManagerInterruptHook hook);
  }

  @NotNull
  public static <T extends Transfer> Collection<T> withTransferManager(@NotNull AmazonS3Client s3Client, @NotNull final WithTransferManager<T> withTransferManager) throws Throwable {
    return withTransferManager(s3Client, false, withTransferManager);
  }

  @NotNull
  private static <T extends Transfer> Collection<T> withTransferManager(@NotNull final AmazonS3Client s3Client, final boolean shutdownClient,
                                                                        @NotNull final WithTransferManager<T> withTransferManager) throws Throwable {

    final TransferManager manager = TransferManagerBuilder.standard().withS3Client(s3Client).withExecutorFactory(createExecutorFactory(createDefaultExecutorService())).withShutDownThreadPools(true).build();

    try {
      final ArrayList<T> transfers = new ArrayList<T>(withTransferManager.run(manager));

      final AtomicBoolean isInterrupted = new AtomicBoolean(false);

      if (withTransferManager instanceof InterruptAwareWithTransferManager) {
        final TransferManagerInterruptHook hook = new TransferManagerInterruptHook() {
          @Override
          public void interrupt() throws Throwable {
            isInterrupted.set(true);

            for (T transfer : transfers) {
              if (transfer instanceof Download) {
                ((Download) transfer).abort();
                continue;
              }

              if (transfer instanceof Upload) {
                ((Upload) transfer).abort();
                continue;
              }

              if (transfer instanceof MultipleFileDownload) {
                ((MultipleFileDownload) transfer).abort();
                continue;
              }

              LOG.warn("Transfer type " + transfer.getClass().getName() + " does not support interrupt");
            }
          }
        };
        ((InterruptAwareWithTransferManager) withTransferManager).setInterruptHook(hook);
      }

      for (T transfer : transfers) {
        try {
          transfer.waitForCompletion();
        } catch (Throwable t) {
          if (!isInterrupted.get()) {
            throw t;
          }
        }
      }

      return CollectionsUtil.filterCollection(transfers, new Filter<T>() {
        @Override
        public boolean accept(@NotNull T data) {
          return Transfer.TransferState.Completed == data.getState();
        }
      });
    } finally {
      manager.shutdownNow(shutdownClient);
    }
  }

  @NotNull
  private static ExecutorFactory createExecutorFactory(@NotNull final ExecutorService executorService) {
    return new ExecutorFactory() {
      @Override
      public ExecutorService newExecutor() {
        return executorService;
      }
    };
  }

  @NotNull
  public static <T extends Transfer> Collection<T>  withTransferManager(@NotNull Map<String, String> params, @NotNull final WithTransferManager<T> withTransferManager) throws Throwable {
    return AWSCommonParams.withAWSClients(params, new AWSCommonParams.WithAWSClients<Collection<T>, Throwable>() {
      @NotNull
      @Override
      public Collection<T> run(@NotNull AWSClients clients) throws Throwable {
        return withTransferManager(clients.createS3Client(), true, withTransferManager);
      }
    });
  }

  @NotNull
  public static ExecutorService createDefaultExecutorService() {
    final ThreadFactory threadFactory = new ThreadFactory() {
      private final AtomicInteger threadCount = new AtomicInteger(1);

      public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("amazon-util-s3-transfer-manager-worker-" + threadCount.getAndIncrement());
        thread.setContextClassLoader(getClass().getClassLoader());
        return thread;
      }
    };
    return Executors.newFixedThreadPool(TeamCityProperties.getInteger(S3_THREAD_POOL_SIZE, DEFAULT_S3_THREAD_POOL_SIZE), threadFactory);
  }
}
