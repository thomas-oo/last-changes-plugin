package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.SCMUtils;
import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.MatchingType;
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

import static org.junit.Assert.assertNotNull;

public class MultiScmLastChangesIT {

    final String projectPath = "/git/last_changes/last-changes-plugin/src/test/resources/git-multiple-git-repos";

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
        //Create a jenkins freestyle project
        FreeStyleProject project = jenkins.createFreeStyleProject("MultiScmJob");
        //gets repos in projectPath, and checks them out to a subdirectory in the workspace named parent eg. <parent>/.git/....
        List<SCM> scms = getSCMs(projectPath);
        MultiSCM multiSCM = new MultiSCM(scms);
        project.setScm(multiSCM);

        //Hook up the post-build plugin (publisher) and save
        LastChangesPublisher publisher = new LastChangesPublisher(FormatType.LINE, MatchingType.NONE, true, false, "0.50","1500");
        project.getPublishersList().add(publisher);
        project.save();

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        assertNotNull(build);
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
