/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.runner.codedeploy;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import static org.assertj.core.api.BDDAssertions.*;
import static jetbrains.buildServer.runner.codedeploy.CodeDeployConstants.*;
import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;

/**
 * @author vbedrosova
 */
public class ParametersValidatorTest extends BaseTestCase {
  @Test
  public void mandatory_params() {
    then(validate()).as("Must detect empty params").hasSize(5).
      containsEntry(DEPLOYMENT_STEPS_PARAM, "Deployment steps mustn't be empty").
      containsEntry(REGION_NAME_PARAM, "AWS region mustn't be empty").
      containsEntry(CREDENTIALS_TYPE_PARAM, "Credentials type mustn't be empty").
      containsEntry(ACCESS_KEY_ID_PARAM, "Access key ID mustn't be empty").
      containsEntry(SECURE_SECRET_ACCESS_KEY_PARAM, "Secret access key mustn't be empty");
  }

  @Test
  public void unexpected_deployment_steps() {
    then(validate(DEPLOYMENT_STEPS_PARAM, "abrakadabra")).as("Must detect unexpected deployment steps").
      containsEntry(DEPLOYMENT_STEPS_PARAM, "Deployment steps has unexpected value abrakadabra");
  }

  @Test
  public void upload_mandatory_params() {
    then(validate(DEPLOYMENT_STEPS_PARAM, UPLOAD_STEP)).as("Must detect empty params").hasSize(6).
      containsEntry(REGION_NAME_PARAM, "AWS region mustn't be empty").
      containsEntry(CREDENTIALS_TYPE_PARAM, "Credentials type mustn't be empty").
      containsEntry(ACCESS_KEY_ID_PARAM, "Access key ID mustn't be empty").
      containsEntry(SECURE_SECRET_ACCESS_KEY_PARAM, "Secret access key mustn't be empty").
      containsEntry(REVISION_PATHS_PARAM, "Application revision mustn't be empty").
      containsEntry(S3_BUCKET_NAME_PARAM, "S3 bucket mustn't be empty");
  }

  @Test
  public void register_mandatory_params() {
    then(validate(DEPLOYMENT_STEPS_PARAM, REGISTER_STEP)).as("Must detect empty params").hasSize(7).
      containsEntry(REGION_NAME_PARAM, "AWS region mustn't be empty").
      containsEntry(CREDENTIALS_TYPE_PARAM, "Credentials type mustn't be empty").
      containsEntry(ACCESS_KEY_ID_PARAM, "Access key ID mustn't be empty").
      containsEntry(SECURE_SECRET_ACCESS_KEY_PARAM, "Secret access key mustn't be empty").
      containsEntry(S3_BUCKET_NAME_PARAM, "S3 bucket mustn't be empty").
      containsEntry(S3_OBJECT_KEY_PARAM, "S3 object key mustn't be empty").
      containsEntry(APP_NAME_PARAM, "Application name mustn't be empty");
  }

  @Test
  public void deploy_mandatory_params() {
    then(validate(DEPLOYMENT_STEPS_PARAM, DEPLOY_STEP)).as("Must detect empty params").hasSize(8).
      containsEntry(REGION_NAME_PARAM, "AWS region mustn't be empty").
      containsEntry(CREDENTIALS_TYPE_PARAM, "Credentials type mustn't be empty").
      containsEntry(ACCESS_KEY_ID_PARAM, "Access key ID mustn't be empty").
      containsEntry(SECURE_SECRET_ACCESS_KEY_PARAM, "Secret access key mustn't be empty").
      containsEntry(S3_BUCKET_NAME_PARAM, "S3 bucket mustn't be empty").
      containsEntry(S3_OBJECT_KEY_PARAM, "S3 object key mustn't be empty").
      containsEntry(APP_NAME_PARAM, "Application name mustn't be empty").
      containsEntry(DEPLOYMENT_GROUP_NAME_PARAM, "Deployment group mustn't be empty");
  }

  @Test
  public void deploy_params_with_references() {
    then(validate(
      DEPLOYMENT_STEPS_PARAM, DEPLOY_STEP,
      REGION_NAME_PARAM, "us-east-1",
      CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
      ACCESS_KEY_ID_PARAM, "%access.key.id%",
      SECURE_SECRET_ACCESS_KEY_PARAM, "%secret.access.key.id%",
      S3_BUCKET_NAME_PARAM, "%s3.bucket.name%",
      S3_OBJECT_KEY_PARAM, "%s3.object.key%",
      APP_NAME_PARAM, "%app.name%",
      DEPLOYMENT_GROUP_NAME_PARAM, "%deployment.group%"
    )).as("Must respect param refs").isEmpty();
  }

  @Test
  public void deploy_params_with_tricky_references() {
    then(validate(
      DEPLOYMENT_STEPS_PARAM, DEPLOY_STEP,
      REGION_NAME_PARAM, "us-east-1",
      CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
      ACCESS_KEY_ID_PARAM, "%access.key.id%.suff",
      SECURE_SECRET_ACCESS_KEY_PARAM, "pref.%secret.access.key.id%.suff",
      S3_BUCKET_NAME_PARAM, "pref.%s3.bucket.name%",
      S3_OBJECT_KEY_PARAM, "%s3 object key%.zip",
      APP_NAME_PARAM, "%app name%",
      DEPLOYMENT_GROUP_NAME_PARAM, "%deployment group%"
    )).as("Must respect param refs").isEmpty();
  }

  @Test
  public void unexpected_revision_paths() {
    then(validate(DEPLOYMENT_STEPS_PARAM, UPLOAD_STEP, REVISION_PATHS_PARAM, "=>")).as("Must detect unexpected revision paths").
      containsEntry(REVISION_PATHS_PARAM, "Application revision has unexpected value, Ant-style wildcards as well as target directories like out/**/*.zip => dist supported");
  }

  @Test
  public void s3_bucket_slashes() {
    then(validate(DEPLOYMENT_STEPS_PARAM, UPLOAD_STEP, S3_BUCKET_NAME_PARAM, "abra/kadabra")).as("Must detect slashes in s3 bucket name").
      containsEntry(S3_BUCKET_NAME_PARAM, "S3 bucket mustn't contain / characters. For addressing folders use S3 object key parameter");
  }

  @Test
  public void s3_object_key_unsafe_chars() {
    then(validate(DEPLOYMENT_STEPS_PARAM, UPLOAD_STEP, S3_OBJECT_KEY_PARAM, "abra~kadabra")).as("Must detect unsafe characters in s3 object key").
      containsEntry(S3_OBJECT_KEY_PARAM, "S3 object key must contain only safe characters");
  }

  @Test
  public void s3_object_key_unexpected_bundle_type() {
    then(validate(DEPLOYMENT_STEPS_PARAM, DEPLOY_STEP, S3_OBJECT_KEY_PARAM, "abrakadabra.jar")).as("Must detect unexpected bundle type in s3 object key").
      containsEntry(S3_OBJECT_KEY_PARAM, "S3 object key provides invalid bundle type, supported bundle types are .zip, .tar and .tar.gz");
  }

  @Test
  public void unexpected_wait_timeout() {
    then(validate(DEPLOYMENT_STEPS_PARAM, DEPLOY_STEP, WAIT_FLAG_PARAM, "true", WAIT_TIMEOUT_SEC_PARAM, "10min")).as("Must detect unexpected wait timeout").
      containsEntry(WAIT_TIMEOUT_SEC_PARAM, "Timeout (seconds) must be a positive integer value");
  }

  @Test
  public void revision_not_fount() throws Exception {
    then(validateRuntime(
      params(DEPLOYMENT_STEPS_PARAM, UPLOAD_STEP, REVISION_PATHS_PARAM, "ready_revision.zip"),
      params())).
      as("Must detect no revision").
      containsEntry(REVISION_PATHS_PARAM, "Application revision ready_revision.zip doesn't exist");
  }

  @Test
  public void unexpected_wait_poll_interval() throws Exception {
    then(validateRuntime(
      params(DEPLOYMENT_STEPS_PARAM, DEPLOY_STEP, WAIT_FLAG_PARAM, "true"),
      params(WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM, "50sec"))).
      as("Must detect unexpected wait poll interval").
      containsEntry(WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM, "codedeploy.wait.poll.interval.sec must be a positive integer value");
  }

  @NotNull
  private Map<String, String> validate(String... pairs) {
    return ParametersValidator.validateSettings(params(pairs));
  }

  @NotNull
  private Map<String, String> validateRuntime(@NotNull Map<String, String> runnerParams, @NotNull Map<String, String> configParams) throws IOException {
    return ParametersValidator.validateRuntime(runnerParams, configParams, createTempDir());
  }

  @NotNull
  private Map<String, String> params(String... pairs) {
    return CollectionsUtil.<String>asMap(pairs);
  }
}
