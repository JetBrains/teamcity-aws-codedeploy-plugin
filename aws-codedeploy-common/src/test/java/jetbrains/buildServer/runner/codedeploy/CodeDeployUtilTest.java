

package jetbrains.buildServer.runner.codedeploy;

import org.testng.annotations.Test;

import static jetbrains.buildServer.runner.codedeploy.CodeDeployUtil.getReadyRevision;
import static jetbrains.buildServer.runner.codedeploy.CodeDeployUtil.getRevisionPathMappings;
import static org.assertj.core.api.BDDAssertions.*;

/**
 * @author vbedrosova
 */
public class CodeDeployUtilTest {
  @Test
  public void ready_revision() {
    then(getReadyRevision("ready_revision.zip")).isEqualTo("ready_revision.zip");
    then(getReadyRevision("ready_revision.tar")).isEqualTo("ready_revision.tar");
    then(getReadyRevision("ready_revision.tar.gz")).isEqualTo("ready_revision.tar.gz");

    then(getReadyRevision("ready_revision.jar")).isNull();
    then(getReadyRevision("**/ready_revision.zip")).isNull();
    then(getReadyRevision("ready_revision.tar\nready_revision.zip")).isNull();
    then(getReadyRevision("ready_revision.tar,ready_revision.zip")).isNull();
    then(getReadyRevision("ready_revision/")).isNull();
    then(getReadyRevision("ready_revision\\")).isNull();
  }

  @Test
  public void revision_path_mappings() {
    then(getRevisionPathMappings("ready_revision/")).hasSize(1).containsEntry("ready_revision/", "");
    then(getRevisionPathMappings("ready_revision\\")).hasSize(1).containsEntry("ready_revision/", "");
    then(getRevisionPathMappings("ready_revision.zip")).isEmpty();
    then(getRevisionPathMappings("ready_revision.tar")).isEmpty();
    then(getRevisionPathMappings("ready_revision.tar.gz")).isEmpty();

    then(getRevisionPathMappings("ready_revision.jar")).hasSize(1).containsEntry("ready_revision.jar", "");
    then(getRevisionPathMappings("**/ready_revision.zip")).hasSize(1).containsEntry("**/ready_revision.zip", "");
    then(getRevisionPathMappings("*.zip")).hasSize(1).containsEntry("*.zip", "");
    then(getRevisionPathMappings("abc?.zip")).hasSize(1).containsEntry("abc?.zip", "");
    then(getRevisionPathMappings("ready_revision.tar\nready_revision.zip")).hasSize(2).containsEntry("ready_revision.tar", "").containsEntry("ready_revision.zip", "");
    then(getRevisionPathMappings("ready_revision.tar,ready_revision.zip")).hasSize(2).containsEntry("ready_revision.tar", "").containsEntry("ready_revision.zip", "");
    then(getRevisionPathMappings("ready_revision.zip=>")).hasSize(1).containsEntry("ready_revision.zip", "");
    then(getRevisionPathMappings("ready_revision.zip=>.")).hasSize(1).containsEntry("ready_revision.zip", "");
    then(getRevisionPathMappings("foo\\bar\\baz\\*.html=>x\\y\\z")).hasSize(1).containsEntry("foo/bar/baz/*.html", "x/y/z");
    then(getRevisionPathMappings("foo/bar/baz/=>x/y/z/")).hasSize(1).containsEntry("foo/bar/baz/", "x/y/z");
    then(getRevisionPathMappings("foo\\bar\\baz\\=>x\\y\\z\\")).hasSize(1).containsEntry("foo/bar/baz/", "x/y/z");
    then(getRevisionPathMappings("./foo/bar/baz/../../bar/baz/=>./x/y/z/../../y/z/")).hasSize(1).containsEntry("foo/bar/baz/", "x/y/z");
    then(getRevisionPathMappings("")).hasSize(1).containsEntry("**", "");
    then(getRevisionPathMappings(".")).hasSize(1).containsEntry("**", "");
    then(getRevisionPathMappings("=>.")).hasSize(1).containsEntry("**", "");
    then(getRevisionPathMappings(".=>")).hasSize(1).containsEntry("**", "");
    then(getRevisionPathMappings(".=>.")).hasSize(1).containsEntry("**", "");
//    then(getRevisionPathMappings("=>")).hasSize(1).containsEntry("**", "");
  }
}