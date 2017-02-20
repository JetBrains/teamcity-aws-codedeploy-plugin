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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author vbedrosova
 */
public class AWSException extends RuntimeException {

  // "CODEDEPLOY_" prefix is for backward compatibility
  public static String SERVICE_PROBLEM_TYPE = "AWS_SERVICE";
  public static String CLIENT_PROBLEM_TYPE = "AWS_CLIENT";
  public static String EXCEPTION_BUILD_PROBLEM_TYPE = "AWS_EXCEPTION";

  public static Map<String, String> PROBLEM_TYPES = CollectionsUtil.asMap(
    SERVICE_PROBLEM_TYPE, "Amazon service exception",
    CLIENT_PROBLEM_TYPE, "Amazon client exception",
    EXCEPTION_BUILD_PROBLEM_TYPE, "Amazon unexpected exception");

  @Nullable private final String myIdentity;
  @NotNull private final String myType;
  @Nullable private final String myDetails;

  public AWSException(@NotNull String message, @Nullable String identity, @NotNull String type, @Nullable String details) {
    super(message);
    myIdentity = identity;
    myType = type;
    myDetails = details;
  }

  public AWSException(@NotNull Throwable t) {
    super(getMessage(t), t);
    myIdentity = getIdentity(t);
    myType = getType(t);
    myDetails = getDetails(t);
  }

  @NotNull
  public static String getMessage(@NotNull Throwable t) {
    if (t instanceof AWSException) return t.getMessage();
    if (t instanceof AmazonServiceException)  return "AWS error: " + removeTrailingDot(((AmazonServiceException) t).getErrorMessage());
    if (t instanceof AmazonClientException) return "AWS client error: " + removeTrailingDot(t.getMessage());
    return "Unexpected error: " + removeTrailingDot(t.getMessage());
  }

  @Nullable
  public static String getIdentity(@NotNull Throwable t) {
    if (t instanceof AWSException) return ((AWSException) t).getIdentity();
    if (t instanceof AmazonServiceException) {
      final AmazonServiceException ase = (AmazonServiceException) t;
      return ase.getServiceName() + ase.getErrorType().name() + String.valueOf(ase.getStatusCode()) + ase.getErrorCode();
    }
    return null;
  }

  @NotNull
  public static String getType(@NotNull Throwable t) {
    if (t instanceof  AWSException) return ((AWSException) t).getType();
    if (t instanceof AmazonServiceException) return SERVICE_PROBLEM_TYPE;
    if (t instanceof AmazonClientException) return CLIENT_PROBLEM_TYPE;
    return EXCEPTION_BUILD_PROBLEM_TYPE;
  }

  @Nullable
  public static String getDetails(@NotNull Throwable t) {
    if (t instanceof AWSException) return ((AWSException) t).getDetails();
    if (t instanceof AmazonServiceException) {
      final AmazonServiceException ase = (AmazonServiceException) t;
      return "\n" +
        "Service:             " + ase.getServiceName() + "\n" +
        "HTTP Status Code:    " + ase.getStatusCode() + "\n" +
        "AWS Error Code:      " + ase.getErrorCode() + "\n" +
        "Error Type:          " + ase.getErrorType() + "\n" +
        "Request ID:          " + ase.getRequestId();
    }
    return null;
  }

  @Nullable
  private static String removeTrailingDot(@Nullable String msg) {
    return (msg != null && msg.endsWith(".")) ? msg.substring(0, msg.length() - 1) : msg;
  }

  @NotNull
  public String getIdentity() {
    return myIdentity == null ? getMessage() :  myIdentity;
  }

  @NotNull
  public String getType() {
    return myType;
  }

  @Nullable
  public String getDetails() {
    return myDetails;
  }
}
