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
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author vbedrosova
 */
public final class S3Util {

  public interface WithTransferManager<T extends Transfer, E extends Throwable> {
    @NotNull
    Collection<T> run(@NotNull TransferManager manager) throws E;
  }

  public static <T extends Transfer, E extends Throwable> void withTransferManager(@NotNull AmazonS3Client s3Client, @NotNull final WithTransferManager<T, E> withTransferManager) throws E {
    withTransferManager(s3Client, null, withTransferManager);
  }

  public static <T extends Transfer, E extends Throwable> void withTransferManager(@NotNull AmazonS3Client s3Client, @Nullable final ExecutorService executorService, @NotNull final WithTransferManager<T, E> withTransferManager) throws E {
    final Collection<Transfer> transfers = new ArrayList<>();
    TransferManager manager = null;
    try {
      manager = TransferManagerBuilder.standard().withS3Client(s3Client).withExecutorFactory(createExecutorFactory(executorService)).build();

      transfers.addAll(withTransferManager.run(manager));

      for (Transfer t : transfers) {
        t.waitForCompletion();
      }

    } catch (InterruptedException e) {
      // noop
    } finally {
      if (manager != null) {
        manager.shutdownNow();
      }
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

  public static <T extends Transfer, E extends Throwable> void withTransferManager(@NotNull Map<String, String> params, @NotNull final WithTransferManager<T, E> withTransferManager) throws E {
    withTransferManager(params, null, withTransferManager);
  }

  public static <T extends Transfer, E extends Throwable> void withTransferManager(@NotNull Map<String, String> params, @Nullable final ExecutorService executorService, @NotNull final WithTransferManager<T, E> withTransferManager) throws E {
    AWSCommonParams.withAWSClients(params, new AWSCommonParams.WithAWSClients<Void, E>() {
      @NotNull
      @Override
      public Void run(@NotNull AWSClients clients) throws E {
        withTransferManager(clients.createS3Client(), executorService, withTransferManager);
        return null;
      }
    });
  }
}
