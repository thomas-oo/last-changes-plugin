package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.MultiScmLastChanges;
import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.MatchingType;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.scm.SCM;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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
        List<SCM> scms = MultiScmLastChanges.getSCMs(projectPath);
        MultiSCM multiSCM = new MultiSCM(scms);
        project.setScm(multiSCM);

        //Hook up the post-build plugin (publisher) and save
        LastChangesPublisher publisher = new LastChangesPublisher(FormatType.LINE, MatchingType.NONE, true, false, "0.50","1500");
        project.getPublishersList().add(publisher);
        project.save();

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        assertNotNull(build);
    }
}
