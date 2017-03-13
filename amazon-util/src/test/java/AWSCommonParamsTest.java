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

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.amazon.AWSCommonParams;
import org.jetbrains.annotations.NotNull;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author vbedrosova
 */
public class AWSCommonParamsTest extends BaseTestCase {
  @Test
  public void mandatory_params() {
    then(validate()).as("Must detect empty params").hasSize(4).
      containsEntry(REGION_NAME_PARAM, "AWS region must not be empty").
      containsEntry(CREDENTIALS_TYPE_PARAM, "Credentials type must not be empty").
      containsEntry(ACCESS_KEY_ID_PARAM, "Access key ID must not be empty").
      containsEntry(SECURE_SECRET_ACCESS_KEY_PARAM, "Secret access key must not be empty");
  }

  @Test
  public void unexpected_region() {
    if (true) {
      throw new SkipException("We do not validate region anymore");
    }
    then(validate(REGION_NAME_PARAM, "abrakadabra")).as("Must detect unexpected region name").
      containsEntry(REGION_NAME_PARAM, "Unsupported region name abrakadabra");
  }

  @Test
  public void unexpected_credentials_type() {
    then(validate(CREDENTIALS_TYPE_PARAM, "abrakadabra")).as("Must detect unexpected credentials type").
      containsEntry(CREDENTIALS_TYPE_PARAM, "Credentials type has unexpected value abrakadabra");
  }

  @Test
  public void default_credentials_provider_chain_true() {
    then(validate(USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM, "true")).as("must not require params").
      doesNotContainKey(ACCESS_KEY_ID_PARAM).
      doesNotContainKey(SECURE_SECRET_ACCESS_KEY_PARAM);
  }

  @Test
  public void temp_credentials_mandatory_params() {
    then(validate(CREDENTIALS_TYPE_PARAM, TEMP_CREDENTIALS_OPTION)).as("Must detect empty params").
      containsEntry(IAM_ROLE_ARN_PARAM, "IAM role ARN must not be empty");
  }

  @NotNull
  private Map<String, String> validate(String... pairs) {
    return AWSCommonParams.validate(CollectionsUtil.<String>asMap(pairs), false);
  }
}
