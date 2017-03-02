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

package jetbrains.buildServer.util.amazon;

import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jetbrains.buildServer.util.amazon.AWSClients.*;

/**
 * @author vbedrosova
 */
public final class AWSCommonParams {

  // "codedeploy_" prefix is for backward compatibility

  public static final String REGION_NAME_PARAM = "codedeploy_region_name";
  public static final String REGION_NAME_LABEL = "AWS region";

  public static final String CREDENTIALS_TYPE_PARAM = "codedeploy_credentials_type";
  public static final String CREDENTIALS_TYPE_LABEL = "Credentials type";
  public static final String TEMP_CREDENTIALS_OPTION = "codedeploy_temp_credentials";
  public static final String TEMP_CREDENTIALS_LABEL = "Temporary credentials";
  public static final String ACCESS_KEYS_OPTION = "codedeploy_access_keys";
  public static final String ACCESS_KEYS_LABEL = "Access keys";

  public static final String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM = "use_default_credential_provider_chain";
  public static final String USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_LABEL = "Use default credential provider chain";

  public static final String ACCESS_KEY_ID_PARAM = "codedeploy_access_key_id";
  public static final String ACCESS_KEY_ID_LABEL = "Access key ID";
  public static final String SECURE_SECRET_ACCESS_KEY_PARAM = "secure:codedeploy_secret_access_key";
  public static final String SECRET_ACCESS_KEY_PARAM = "codedeploy_secret_access_key";
  public static final String SECRET_ACCESS_KEY_LABEL = "Secret access key";

  public static final String IAM_ROLE_ARN_PARAM = "codedeploy_iam_role_arn";
  public static final String IAM_ROLE_ARN_LABEL = "IAM role ARN";
  public static final String EXTERNAL_ID_PARAM = "codedeploy_external_id";
  public static final String EXTERNAL_ID_LABEL = "External ID";

  public static final Map<String, String> DEFAULTS = Collections.unmodifiableMap(CollectionsUtil.asMap(
    CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
    EXTERNAL_ID_PARAM, UUID.randomUUID().toString(),
    USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM, "false"
  ));

  public static final String TEMP_CREDENTIALS_SESSION_NAME_PARAM = "temp_credentials_session_name";
  public static final String TEMP_CREDENTIALS_SESSION_NAME_DEFAULT_PREFIX = "TeamCity_AWS_support_";
  public static final String TEMP_CREDENTIALS_DURATION_SEC_PARAM = "temp_credentials_duration_sec";
  public static final int TEMP_CREDENTIALS_DURATION_SEC_DEFAULT = 1800;

  @NotNull
  private final ServerSettings myServerSettings;

  public AWSCommonParams(@NotNull ServerSettings serverSettings) {
    myServerSettings = serverSettings;
  }

  @NotNull
  public Map<String, String> getDefaults() {
    final Map<String, String> defaults = new HashMap<String, String>(DEFAULTS);
    final String serverUUID = myServerSettings.getServerUUID();
    if (StringUtil.isNotEmpty(serverUUID)) {
      defaults.put(EXTERNAL_ID_PARAM, "TeamCity-server-" + serverUUID);
    }
    return defaults;
  }

  @NotNull
  public static Map<String, String> validate(@NotNull Map<String, String> params, boolean acceptReferences) {
    final Map<String, String> invalids = new HashMap<String, String>();

    if (StringUtil.isEmptyOrSpaces(getRegionName(params))) {
      invalids.put(REGION_NAME_PARAM, REGION_NAME_LABEL + " mustn't be empty");
    }

    if (!Boolean.parseBoolean(params.get(USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM))) {
      if (StringUtil.isEmptyOrSpaces(params.get(ACCESS_KEY_ID_PARAM))) {
        invalids.put(ACCESS_KEY_ID_PARAM, ACCESS_KEY_ID_LABEL + " mustn't be empty");
      }
      if (StringUtil.isEmptyOrSpaces(getSecretAccessKey(params))) {
        invalids.put(SECURE_SECRET_ACCESS_KEY_PARAM, SECRET_ACCESS_KEY_LABEL + " mustn't be empty");
      }
    }

    final String credentialsType = params.get(CREDENTIALS_TYPE_PARAM);
    if (TEMP_CREDENTIALS_OPTION.equals(credentialsType)) {
      if (StringUtil.isEmptyOrSpaces(params.get(IAM_ROLE_ARN_PARAM))) {
        invalids.put(IAM_ROLE_ARN_PARAM, IAM_ROLE_ARN_LABEL + " mustn't be empty");
      }
    } else if (StringUtil.isEmptyOrSpaces(credentialsType)) {
      invalids.put(CREDENTIALS_TYPE_PARAM, CREDENTIALS_TYPE_LABEL + " mustn't be empty");
    } else if (!ACCESS_KEYS_OPTION.equals(credentialsType)) {
      invalids.put(CREDENTIALS_TYPE_PARAM, CREDENTIALS_TYPE_LABEL + " has unexpected value " + credentialsType);
    }

    return invalids;
  }

  @Nullable
  private static String getSecretAccessKey(@NotNull Map<String, String> params) {
    final String secretAccessKeyParam = params.get(SECURE_SECRET_ACCESS_KEY_PARAM);
    return StringUtil.isNotEmpty(secretAccessKeyParam) ? secretAccessKeyParam : params.get(SECRET_ACCESS_KEY_PARAM);
  }

  @Nullable
  public static String getRegionName(@NotNull Map<String, String> params) {
    return params.get(REGION_NAME_PARAM);
  }

  private static boolean isReference(@NotNull String param, boolean acceptReferences) {
    return acceptReferences && ReferencesResolverUtil.containsReference(param);
  }

  public interface WithAWSClients<T, E extends Throwable> {
    @Nullable T run(@NotNull AWSClients clients) throws E;
  }

  public static <T, E extends Throwable> T withAWSClients(@NotNull Map<String, String> params, @NotNull WithAWSClients<T, E> withAWSClients) throws E {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(AWSCommonParams.class.getClassLoader());
    try {
      return withAWSClients.run(createAWSClients(params));
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  @NotNull
  private static AWSClients createAWSClients(@NotNull Map<String, String> params) {
    final String regionName = getRegionName(params);

    final String accessKeyId = params.get(ACCESS_KEY_ID_PARAM);
    final String secretAccessKey = getSecretAccessKey(params);

    final boolean useDefaultCredProvChain = Boolean.parseBoolean(params.get(USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM));

    if (TEMP_CREDENTIALS_OPTION.equals(params.get(CREDENTIALS_TYPE_PARAM))) {
      final String iamRoleARN = params.get(IAM_ROLE_ARN_PARAM);
      final String externalID = params.get(EXTERNAL_ID_PARAM);
      final String sessionName = getStringOrDefault(params.get(TEMP_CREDENTIALS_SESSION_NAME_PARAM), TEMP_CREDENTIALS_SESSION_NAME_DEFAULT_PREFIX + new Date().getTime());
      final int sessionDuration = getIntegerOrDefault(params.get(TEMP_CREDENTIALS_DURATION_SEC_PARAM), TEMP_CREDENTIALS_DURATION_SEC_DEFAULT);

      return
        useDefaultCredProvChain ?
          fromSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration, regionName) :
          fromSessionCredentials(accessKeyId, secretAccessKey, iamRoleARN, externalID, sessionName, sessionDuration, regionName);
    }

    return
      useDefaultCredProvChain ?
        fromDefaultCredentialProviderChain(regionName) :
        fromBasicCredentials(accessKeyId, secretAccessKey, regionName);
  }

  @NotNull
  public static String getStringOrDefault(@Nullable String val, @NotNull String defaultVal) {
    return StringUtil.isEmptyOrSpaces(val) ? defaultVal : val;
  }

  public static int getIntegerOrDefault(@Nullable String val, int defaultVal) {
    try {
      if (StringUtil.isNotEmpty(val)) return Integer.parseInt(val);
    } catch (NumberFormatException e) { /* see below */ }
    return defaultVal;
  }

  public static int calculateIdentity(@NotNull String baseDir, @NotNull Map<String, String> params, @NotNull Collection<String> otherParts) {
    return calculateIdentity(baseDir, params, CollectionsUtil.toStringArray(otherParts));
  }

  public static int calculateIdentity(@NotNull String baseDir, @NotNull Map<String, String> params, String... otherParts) {
    List<String> allParts = new ArrayList<String>(CollectionsUtil.join(getIdentityFormingParams(params), Arrays.asList(otherParts)));
    allParts = CollectionsUtil.filterNulls(allParts);
    Collections.sort(allParts);

    baseDir = FileUtil.toSystemIndependentName(baseDir);
    final StringBuilder sb = new StringBuilder();
    for (String p : allParts) {
      if (StringUtil.isEmptyOrSpaces(p)) continue;

      p = FileUtil.toSystemIndependentName(p);
      p = p.replace(baseDir, "");
      sb.append(p);
    }

    return sb.toString().replace(" ", "").toLowerCase().hashCode();
  }

  @NotNull
  private static Collection<String> getIdentityFormingParams(@NotNull Map<String, String> params) {
    return Arrays.asList(getRegionName(params), params.get(ACCESS_KEY_ID_PARAM), params.get(IAM_ROLE_ARN_PARAM));
  }
}
