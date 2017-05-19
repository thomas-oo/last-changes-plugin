package com.github.jenkins.lastchanges;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class SvnLastChangesTest {


    final String svnRepoUrl = "https://subversion.assembla.com/svn/cucumber-json-files/trunk";

//    @Test
//    public void shouldInitRepository() {
//        assertNotNull(SvnLastChanges.repository(svnRepoUrl));
//    }
//
//    @Test
//    public void shouldGetLastChanges() {
//
//            SVNRepository repository = SvnLastChanges.repository(svnRepoUrl);
//            assertNotNull(repository);
//            LastChanges lastChanges = SvnLastChanges.getInstance().getLastChangesOf(repository);
//            assertNotNull(lastChanges);
//            assertThat(lastChanges.getCommitInfo()).isNotNull();
//            assertThat(lastChanges.getDiff()).isNotEmpty();
//            assertThat(lastChanges.getCommitInfo().getCommitMessage()).isEqualTo("updated cukedóctor json");
//
//    }



}