package com.github.jenkins.multiLastChanges;

import com.github.jenkins.multiLastChanges.model.FormatType;
import com.github.jenkins.multiLastChanges.model.MatchingType;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.DisableRemotePoll;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.slaves.DumbSlave;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(JUnit4.class)
public class MultiLastChangesIT {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private File sampleRepoDir = new File(GitMultiLastChangesTest.class.getResource("/git-sample-repo").getFile());



    @Test
    public void shouldGetLastChangesOfGitRepository() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        MultiLastChangesPublisher publisher = new MultiLastChangesPublisher(FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500");
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        MultiLastChangesBuildAction action = build.getAction(MultiLastChangesBuildAction.class);
        assertThat(action).isNotNull();
        MultiLastChanges multiLastChanges = action.getBuildChanges();
        assertThat(multiLastChanges).isNotNull();
        assertThat(multiLastChanges).isNotNull();
        assertThat(multiLastChanges.getCommitInfo()).isNotNull();
        assertThat(multiLastChanges.getCommitInfo().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(multiLastChanges.getCommitInfo().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        Assertions.assertThat(multiLastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitMultiLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitMultiLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitMultiLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitMultiLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitMultiLastChangesTest.newLine +
                " /**" + GitMultiLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitMultiLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitMultiLastChangesTest.newLine +
                "+ *" + GitMultiLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitMultiLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitMultiLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitMultiLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitMultiLastChangesTest.newLine +
                "+ *" + GitMultiLastChangesTest.newLine +
                "  * @param entity the entity type" + GitMultiLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitMultiLastChangesTest.newLine +
                "  */" + GitMultiLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitMultiLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitMultiLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitMultiLastChangesTest.newLine +
                "  * @param caption the new caption" + GitMultiLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitMultiLastChangesTest.newLine +
                "  */" + GitMultiLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitMultiLastChangesTest.newLine +
                " " + GitMultiLastChangesTest.newLine +
                "+" + GitMultiLastChangesTest.newLine +
                "+" + GitMultiLastChangesTest.newLine +
                " /**" + GitMultiLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitMultiLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));
        

        jenkins.assertLogContains("Last changes published successfully!", build);

    }


    @Test
    public void shouldGetLastChangesOfGitRepositoryOnSlaveNode() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        DumbSlave slave = jenkins.createSlave();
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test-slave");
        project.setAssignedNode(slave);
        project.setScm(scm);
        MultiLastChangesPublisher publisher = new MultiLastChangesPublisher(FormatType.SIDE,MatchingType.WORD, true, false, null,null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        MultiLastChangesBuildAction action = build.getAction(MultiLastChangesBuildAction.class);
        assertThat(action).isNotNull();
        MultiLastChanges multiLastChanges = action.getBuildChanges();
        assertThat(multiLastChanges).isNotNull();
        assertThat(multiLastChanges).isNotNull();
        assertThat(multiLastChanges.getCommitInfo()).isNotNull();
        assertThat(multiLastChanges.getCommitInfo().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(multiLastChanges.getCommitInfo().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        Assertions.assertThat(multiLastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitMultiLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitMultiLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitMultiLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitMultiLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitMultiLastChangesTest.newLine +
                " /**" + GitMultiLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitMultiLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitMultiLastChangesTest.newLine +
                "+ *" + GitMultiLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitMultiLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitMultiLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitMultiLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitMultiLastChangesTest.newLine +
                "+ *" + GitMultiLastChangesTest.newLine +
                "  * @param entity the entity type" + GitMultiLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitMultiLastChangesTest.newLine +
                "  */" + GitMultiLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitMultiLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitMultiLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitMultiLastChangesTest.newLine +
                "  * @param caption the new caption" + GitMultiLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitMultiLastChangesTest.newLine +
                "  */" + GitMultiLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitMultiLastChangesTest.newLine +
                " " + GitMultiLastChangesTest.newLine +
                "+" + GitMultiLastChangesTest.newLine +
                "+" + GitMultiLastChangesTest.newLine +
                " /**" + GitMultiLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitMultiLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));
        jenkins.assertLogContains("Last changes published successfully!",build);

    }
    
    @Test
    public void shouldGetLastChangesOfSvnRepository() throws Exception {

        // given
    	ModuleLocation location = new ModuleLocation("https://subversion.assembla.com/svn/cucumber-json-files/trunk", ""); 
    	List<ModuleLocation> locations = new ArrayList<>();
    	locations.add(location);
        SvnSCM scm = new SvnSCM(".svn",sampleRepoDir,locations);
        FreeStyleProject project = jenkins.createFreeStyleProject("svn-test");
        project.setScm(scm);
        MultiLastChangesPublisher publisher = new MultiLastChangesPublisher(FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500");
        project.getPublishersList().add(publisher);
        project.save();
        
        
        
        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        MultiLastChangesBuildAction action = build.getAction(MultiLastChangesBuildAction.class);
        assertThat(action).isNotNull();
        assertThat(action.getBuildChanges()).isNotNull();
        assertThat(action.getBuildChanges().getCommitInfo().getCommiterName()).isEqualTo("rmpestano");
        jenkins.assertLogContains("Last changes published successfully!",build);
    }
}
