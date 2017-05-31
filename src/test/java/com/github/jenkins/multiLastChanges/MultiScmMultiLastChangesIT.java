package com.github.jenkins.multiLastChanges;

import com.github.jenkins.multiLastChanges.impl.SCMUtils;
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
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.scm.SCM;
import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MultiScmMultiLastChangesIT {

    final String projectPath = "/git/last_changes/multi-last-changes-plugin/src/test/resources/git-multiple-git-repos";
    final int expectedNumberOfBuildChanges = 3;
    final String successfulPublish = "Last changes published successfully!";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldGetLastChangesOfMultiScmRepository() throws Exception {
        //Given
        //Create a jenkins freestyle project
        FreeStyleProject project = jenkins.createFreeStyleProject("MultiScmJob");
        //gets repos in projectPath, and checks them out to a subdirectory in the workspace named parent eg. <parent>/.git/....
        List<SCM> scms = getSCMs(projectPath);
        MultiSCM multiSCM = new MultiSCM(scms);
        project.setScm(multiSCM);
        //Hook up the post-build plugin (publisher) and save
        MultiLastChangesPublisher publisher = new MultiLastChangesPublisher(FormatType.LINE, MatchingType.NONE, true, false, "0.50","1500");
        project.getPublishersList().add(publisher);
        project.save();

        //when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        assertNotNull(build);

        // then
        MultiLastChangesBuildAction action = build.getAction(MultiLastChangesBuildAction.class);
        assertThat(action).isNotNull();
        Set<MultiLastChanges> multiLastChanges = action.getAllBuildChanges();
        assertThat(multiLastChanges).isNotNull();
        assertThat(multiLastChanges).isNotNull();
        assertEquals(expectedNumberOfBuildChanges, multiLastChanges.size());
        jenkins.assertLogContains(successfulPublish, build);
    }

    @Test
    public void shouldGetLastChangesOfMultiScmRepositoryOnSlaveNode() throws Exception {
        //Given
        //Create a jenkins freestyle project
        FreeStyleProject project = jenkins.createFreeStyleProject("MultiScmJob-slave");
        //gets repos in projectPath, and checks them out to a subdirectory in the workspace named parent eg. <parent>/.git/....
        List<SCM> scms = getSCMs(projectPath);
        MultiSCM multiSCM = new MultiSCM(scms);
        project.setScm(multiSCM);
        //Setup a slave node
        DumbSlave slave = jenkins.createSlave();
        project.setAssignedNode(slave);
        //Hook up the post-build plugin (publisher) and save
        MultiLastChangesPublisher publisher = new MultiLastChangesPublisher(FormatType.LINE, MatchingType.NONE, true, false, "0.50","1500");
        project.getPublishersList().add(publisher);
        project.save();

        //when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        assertNotNull(build);

        // then
        MultiLastChangesBuildAction action = build.getAction(MultiLastChangesBuildAction.class);
        assertThat(action).isNotNull();
        Set<MultiLastChanges> multiLastChanges = action.getAllBuildChanges();
        assertThat(multiLastChanges).isNotNull();
        assertThat(multiLastChanges).isNotNull();
        assertEquals(expectedNumberOfBuildChanges, multiLastChanges.size());
        jenkins.assertLogContains(successfulPublish, build);
    }

    /**
     *
     * @param path
     * @return
     */
    public static List<SCM> getSCMs(String path){
        List<String> repoPaths = SCMUtils.findPathsOfGitRepos(path);
        List<SCM> scms = new ArrayList<>();

        for(String repoPath : repoPaths) {
            File repoFile = new File(repoPath);
            List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
            remoteConfigs.add(new UserRemoteConfig(repoPath, "origin", "", null));
            List<BranchSpec> branches = new ArrayList<>();
            branches.add(new BranchSpec("master"));
            List<GitSCMExtension> extensions = new ArrayList<>();
            extensions.add(new DisableRemotePoll());
            //create and checkout git repos to a subfolder named: git folder's parent folder
            extensions.add(new RelativeTargetDirectory(repoFile.getParentFile().getName()));
            GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                    Collections.<SubmoduleConfig>emptyList(), null, null,
                    extensions);
            scms.add(scm);
        }
        return scms;
    }
}
