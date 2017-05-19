package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.MultiScmLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
public class MultiScmLastChangesTest {

    String projectPath;

    @Before
    public void setUp() throws Exception {
        //Fixme: point to a tar file and test on that
        projectPath = "/git/last_changes/last-changes-plugin/src/test/resources/git-multiple-git-repos";
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldGetLastChangesFromGitRepositories() {
        MultiScmLastChanges multiScmLastChanges = new MultiScmLastChanges(projectPath);
        Set<LastChanges> lastChangesSet = multiScmLastChanges.getLastChanges();
        assertFalse(lastChangesSet.isEmpty());
        assertEquals(3, lastChangesSet.size());
    }
}
