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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author vbedrosova
 */
public final class AWSRegions {
  private static final Map<String, String> REGION_NAMES_FOR_WEB;

  static {
    REGION_NAMES_FOR_WEB = new LinkedHashMap<String, String>();
    REGION_NAMES_FOR_WEB.put("us-east-1", "US East (N. Virginia)");
    REGION_NAMES_FOR_WEB.put("us-west-1", "US West (N. California)");
    REGION_NAMES_FOR_WEB.put("us-west-2", "US West (Oregon)");
    REGION_NAMES_FOR_WEB.put("eu-west-1", "EU West (Dublin)");
    REGION_NAMES_FOR_WEB.put("eu-central-1", "EU Central (Frankfurt)");
    REGION_NAMES_FOR_WEB.put("sa-east-1", "South America (Sao Paulo)");
    REGION_NAMES_FOR_WEB.put("ap-northeast-1", "Asia Pacific (Tokyo)");
    REGION_NAMES_FOR_WEB.put("ap-northeast-2", "Asia Pacific (Seoul)");
    REGION_NAMES_FOR_WEB.put("ap-southeast-1", "Asia Pacific (Singapore)");
    REGION_NAMES_FOR_WEB.put("ap-southeast-2", "Asia Pacific (Sydney)");
    REGION_NAMES_FOR_WEB.put("us-gov-west-1", "AWS GovCloud (US)");
    REGION_NAMES_FOR_WEB.put("cn-north-1", "China (Beijing)");
  }

  @NotNull
  public static String getRegionNameForWeb(@NotNull String regionCode){
    final String niceName = REGION_NAMES_FOR_WEB.get(regionCode);
    return niceName == null ? regionCode : niceName;
  }

  @NotNull
  public static Map<String,String> getAllRegions(){
    return Collections.unmodifiableMap(REGION_NAMES_FOR_WEB);
  }

  @NotNull
  public static Region getRegion(@NotNull String regionName) throws IllegalArgumentException {
    try {
      return Region.getRegion(Regions.fromName(regionName));
    } catch (Exception e) {
      // see below
    }
    throw new IllegalArgumentException("Unsupported region name " + regionName);
  }
}
