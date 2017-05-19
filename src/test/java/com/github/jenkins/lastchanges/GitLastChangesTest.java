package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.exception.GitTreeNotFoundException;
import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class GitLastChangesTest {
    public static final String newLine = System.getProperty("line.separator");


    String gitRepoPath;


    @Before
    public void before() {
        gitRepoPath = GitLastChangesTest.class.getResource("/git-sample-repo").getFile();

        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    @Test
    public void shouldGetLastChangesFromGitRepository() throws FileNotFoundException {
        GitLastChanges gitLastChanges = new GitLastChanges(gitRepoPath);
        LastChanges lastChanges = gitLastChanges.getLastChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCommitInfo()).isNotNull();
        assertThat(lastChanges.getCommitInfo().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCommitInfo().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");

        assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "index 6d28c9b..bcc2ac0 100644" + newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "@@ -31,6 +31,12 @@" + newLine +
                " /**" + newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + newLine +
                "+ *" + newLine +
                "+ * Containers produced by this method have the following properties:" + newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + newLine +
                "+ *" + newLine +
                "  * @param entity the entity type" + newLine +
                "  * @return the new container which can be assigned to a [Grid]" + newLine +
                "  */" + newLine +
                "@@ -279,9 +285,12 @@" + newLine +
                "  * An utility method which adds an item and sets item's caption." + newLine +
                "  * @param the Identification of the item to be created." + newLine +
                "  * @param caption the new caption" + newLine +
                "+ * @return the newly created item ID." + newLine +
                "  */" + newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + newLine +
                " " + newLine +
                "+" + newLine +
                "+" + newLine +
                " /**" + newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));
    }


    @Test
    public void shouldGetLastChangesFromInitialCommitGitRepo() throws FileNotFoundException {
        String repositoryLocation = GitLastChangesTest.class.getResource("/git-initial-commit-repo").getFile();
        File file = new File(repositoryLocation);
        try {
            GitLastChanges gitLastChanges = new GitLastChanges(repositoryLocation);
            LastChanges lastChanges = gitLastChanges.getLastChanges();
            fail("Should not get here");
        }catch (GitTreeNotFoundException e){
            assertThat(e.getMessage()).isEqualTo(String.format("Could not find previous head of repository located at %s. Its your first commit?",file.getAbsolutePath()));
        }
    }


}