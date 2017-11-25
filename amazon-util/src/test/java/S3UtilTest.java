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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.amazon.AWSClients;
import jetbrains.buildServer.util.amazon.S3Util;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;

/**
 * @author vbedrosova
 */

@Test
public class S3UtilTest extends BaseTestCase {

  public static final String BUCKET_NAME = "amazon.util.s3.util.test";

  @BeforeClass
  @Override
  protected void setUpClass() throws Exception {
    super.setUpClass();
    final AmazonS3 s3Client = createS3Client();
    if (s3Client.doesBucketExist(BUCKET_NAME)) {
      deleteBucket(BUCKET_NAME, s3Client);
    }
    s3Client.createBucket(BUCKET_NAME);
  }

  @AfterClass
  protected void tearDownClass() throws Exception {
    deleteBucket(BUCKET_NAME, createS3Client());
  }

  @Test
  public void shutdown_manager() throws Throwable{
    S3Util.withTransferManager(getParameters(), new S3Util.WithTransferManager<Transfer>() {
      @NotNull
      @Override
      public Collection<Transfer> run(@NotNull TransferManager manager) throws Throwable {
        manager.shutdownNow();
        return Collections.emptyList();
      }
    });
  }

  @Test
  public void upload() throws Throwable {
    final File testUpload = createTempFile("This is a test upload");
    S3Util.withTransferManager(getParameters(), new S3Util.WithTransferManager<Upload>() {
      @NotNull
      @Override
      public Collection<Upload> run(@NotNull TransferManager manager) throws Throwable {
        return Collections.singletonList(manager.upload(BUCKET_NAME, "testUpload", testUpload));
      }
    });
  }

  @Test
  public void uploadAndInterrupt() throws Throwable {
    final File testUpload = createTempFile(104857600);
    final AtomicReference<S3Util.TransferManagerInterruptHook> interruptHook = new AtomicReference<S3Util.TransferManagerInterruptHook>();
    new Thread(new Runnable() {
      @Override
      public void run() {
        while (interruptHook.get() == null) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        try {
          interruptHook.get().interrupt();
        } catch (Throwable t) {
          throw new RuntimeException(t);
        }
      }
    }).start();
    S3Util.withTransferManager(getParameters(), new S3Util.InterruptAwareWithTransferManager<Upload>() {
      @NotNull
      @Override
      public Collection<Upload> run(@NotNull TransferManager manager) throws Throwable {
        return Collections.singletonList(manager.upload(BUCKET_NAME, "testUploadInterrupt", testUpload));
      }

      @Override
      public void setInterruptHook(@NotNull S3Util.TransferManagerInterruptHook hook) {
        interruptHook.set(hook);
      }
    });
    try {
      createS3Client().getObject(BUCKET_NAME,"testUploadInterrupt");
    } catch (AmazonS3Exception e) {
      assertContains(e.getMessage(), "The specified key does not exist");
    }
  }

  @Test
  public void download() throws Throwable {
    final File testDownload = createTempFile("This is a test download");
    final AmazonS3 s3Client = createS3Client();
    s3Client.putObject(BUCKET_NAME, "testDownload", testDownload);
    assertEquals(testDownload.length(), createS3Client().getObject(BUCKET_NAME, "testDownload").getObjectMetadata().getContentLength());

    final File result = new File(createTempDir(), "testDownload");
    S3Util.withTransferManager(getParameters(), new S3Util.WithTransferManager<Download>() {
      @NotNull
      @Override
      public Collection<Download> run(@NotNull TransferManager manager) throws Throwable {
        return Collections.singletonList(manager.download(BUCKET_NAME, "testDownload", result));
      }
    });
    assertEquals(testDownload.length(), result.length());
  }

  private void deleteBucket(@NotNull String name, @NotNull AmazonS3 s3Client) throws Exception {
    ObjectListing listing = s3Client.listObjects(name);
    while (true) {
      for (S3ObjectSummary s3ObjectSummary : listing.getObjectSummaries()) {
        s3Client.deleteObject(name, (s3ObjectSummary).getKey());
      }

      // more object_listing to retrieve?
      if (listing.isTruncated()) {
        listing = s3Client.listNextBatchOfObjects(listing);
      } else {
        break;
      }
    }

    s3Client.deleteBucket(name);
  }

  @NotNull
  private AmazonS3 createS3Client() {
    final Map<String, String> params = getParameters();
    return AWSClients.fromBasicCredentials(
            params.get(ACCESS_KEY_ID_PARAM),
            params.get(SECURE_SECRET_ACCESS_KEY_PARAM),
            params.get(REGION_NAME_PARAM)
    ).createS3Client();
  }

  @NotNull
  private Map<String, String> getParameters() {
    return CollectionsUtil.asMap(
      REGION_NAME_PARAM, Regions.DEFAULT_REGION.getName(),
      CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
      ACCESS_KEY_ID_PARAM, System.getProperty(ACCESS_KEY_ID_PARAM),
      SECURE_SECRET_ACCESS_KEY_PARAM, System.getProperty(SECURE_SECRET_ACCESS_KEY_PARAM));
  }
}