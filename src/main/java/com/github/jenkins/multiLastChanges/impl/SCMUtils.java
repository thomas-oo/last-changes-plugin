/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.multiLastChanges.impl;

import com.github.jenkins.multiLastChanges.exception.GitDiffException;
import com.github.jenkins.multiLastChanges.exception.GitTreeNotFoundException;
import com.github.jenkins.multiLastChanges.exception.GitTreeParseException;
import com.github.jenkins.multiLastChanges.exception.RepositoryNotFoundException;
import com.github.jenkins.multiLastChanges.model.CommitInfo;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 5/19/17.
 */
public class SCMUtils {

    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff
     */
    public static MultiLastChanges changesOf(Repository repository) {
        Git git = new Git(repository);
        try {
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            ObjectId head = resolveCurrentRevision(repository);
            ObjectId previousHead = null;
            try {
                previousHead = repository.resolve("HEAD~^{tree}");
                if (previousHead == null) {
                    throw new GitTreeNotFoundException(String.format("Could not find previous head of repository located at %s. Its your first commit?", repositoryLocation));
                }
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve previous head of repository located at " + repositoryLocation, e);
            }

            return changesOf(repository, head, previousHead);
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }
    }



    public static ObjectId resolveCurrentRevision(Repository repository) {
        String repositoryLocation = repository.getDirectory().getAbsolutePath();
        try {
            return repository.resolve("HEAD^{tree}");
        } catch (IOException e) {
            throw new GitTreeNotFoundException("Could not resolve head of repository located at " + repositoryLocation, e);
        }

    }

    /**
     * Creates last changes by "diffing" two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff between revisions
     */
    public static MultiLastChanges changesOf(Repository repository, ObjectId currentRevision, ObjectId previousRevision) {
        Git git = new Git(repository);
        try {
            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            CommitInfo lastCommitInfo;
            CommitInfo oldCommitInfo;
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            DiffFormatter formatter = new DiffFormatter(diffStream);
            formatter.setRepository(repository);
            ObjectReader reader = repository.newObjectReader();

            lastCommitInfo = CommitInfo.Builder.buildFromGit(repository, currentRevision);
            oldCommitInfo = CommitInfo.Builder.buildFromGit(repository, previousRevision);

            // Create the tree iterator for each commit
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            try {
                RevWalk revWalk = new RevWalk(repository);
                if (revWalk.parseAny(previousRevision) instanceof RevCommit) {
                    RevCommit revCommit = revWalk.parseCommit(previousRevision);
                    oldTreeIter.reset(reader, revCommit.getTree().getId());
                } else {
                    oldTreeIter.reset(reader, previousRevision);
                }
            } catch (Exception e) {
                throw new GitTreeParseException("Could not parse previous commit tree.", e);
            }
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            try {
                RevWalk revWalk = new RevWalk(repository);
                if (revWalk.parseAny(currentRevision) instanceof RevCommit) {
                    RevCommit revCommit = revWalk.parseCommit(currentRevision);
                    newTreeIter.reset(reader, revCommit.getTree().getId());
                } else {
                    newTreeIter.reset(reader, currentRevision);
                }

            } catch (IOException e) {
                throw new GitTreeParseException("Could not parse current commit tree.", e);
            }
            try {
                for (DiffEntry change : git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call()) {
                    formatter.format(change);
                }
            } catch (Exception e) {
                throw new GitDiffException("Could not get last changes of repository located at " + repositoryLocation, e);
            }

            return new MultiLastChanges(lastCommitInfo, oldCommitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }
    }

    /**
     * Utility method to find all git folders in path
     * @param path A parent folder path
     * @return A list of the .git file paths found in path
     */
    public static List<String> findPathsOfGitRepos(String path){
        if (path == null || path.isEmpty()) {
            throw new RepositoryNotFoundException("MultiScm repository path cannot be empty.");
        }

        File projectDir = new File(path);

        if(!projectDir.exists()) {
            throw new RepositoryNotFoundException(String.format("MultiScm repository path not found at location %s.", projectDir));
        }

        List<String> repositoryPaths = new ArrayList<>();

        //All subdirectories of repositoryPath
        File[] directories = projectDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        //See if this folder has a .git subfolder
        for(File file : directories){
            if(file.getName().equalsIgnoreCase(".git")){
                repositoryPaths.add(file.getAbsolutePath());
            }
        }

        //There are no .git subfolders, recursively look into subdirectories for git repos
        for(File directory : directories){
            List<String> repoPathsInDirectory = findPathsOfGitRepos(directory.getAbsolutePath());
            if (repoPathsInDirectory!= null){
                //add git repos from subdirectories
                repositoryPaths.addAll(repoPathsInDirectory);
            }
        }
        return repositoryPaths.isEmpty() ? null : repositoryPaths;
    }
}
