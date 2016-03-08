package jetbrains.buildServer.runner.codedeploy;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author vbedrosova
 */
public class AWSUtil {
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
}
