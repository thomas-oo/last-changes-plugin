package com.github.jenkins.multiLastChanges;

import com.github.jenkins.multiLastChanges.impl.MultiScmLastChanges;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4.class)
public class MultiScmMultiLastChangesTest {

    String projectPath;

    @Before
    public void setUp() throws Exception {
        //Fixme: point to a tar file and test on that
        projectPath = "/git/last_changes/multi-last-changes-plugin/src/test/resources/git-multiple-git-repos";
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldGetLastChangesFromGitRepositories() {
        MultiScmLastChanges multiScmLastChanges = new MultiScmLastChanges(projectPath);
        List<MultiLastChanges> multiLastChangesList = multiScmLastChanges.getLastChanges();
        assertFalse(multiLastChangesList.isEmpty());
        assertEquals(3, multiLastChangesList.size());
    }
}
