package com.github.jenkins.lastchanges.impl;


import com.github.jenkins.lastchanges.exception.CommitInfoException;
import com.github.jenkins.lastchanges.exception.GitDiffException;
import com.github.jenkins.lastchanges.exception.GitTreeNotFoundException;
import com.github.jenkins.lastchanges.exception.GitTreeParseException;
import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//Todo: Create a test suite
public class MultiScmLastChanges{

    private static MultiScmLastChanges instance;


    private MultiScmLastChanges() {
    }

    public static MultiScmLastChanges getInstance(){
        if(instance == null){
            instance = new MultiScmLastChanges();
        }

        return instance;
    }

    /**
     * Traverses all folders and find git repos, returning them in a map.
     *
     * @param path Path that holds multiple git repos. May be a remote path
     * @return Git repositories from location path
     */
    public static Map<String,Repository> repositories(String path){
        if (path == null || path.isEmpty()) {
            throw new RepositoryNotFoundException("Git repository path cannot be empty.");
        }

        File projectDir = new File(path);

        if(!projectDir.exists()) {
            throw new RepositoryNotFoundException(String.format("Git repository path not found at location %s.", projectDir));
        }

        Map<String, Repository> repositoryMap = new HashMap<>();

        //All subdirectories of repositoryPath
        File[] directories = projectDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        //Todo: refactor w/ code in publisher
        //See if this folder has a .git subfolder. If so, return a map with the parent folder name and repo.
        for(File file : directories){
            if(file.getName().equalsIgnoreCase(".git")){
                Repository repository = GitLastChanges.repository(file.getAbsolutePath());
                String parentFolder = projectDir.getName();
                repositoryMap.put(parentFolder, repository);
                return repositoryMap;
            }
        }

        //There are no .git subfolders, recursively look into subdirectories for git repos
        for(File directory : directories){
            Map<String, Repository> reposInDirectory = repositories(directory.getAbsolutePath());
            if (reposInDirectory!= null){
                //add git repos from subdirectories
                repositoryMap.putAll(reposInDirectory);
            }
        }
        return repositoryMap.isEmpty() ? null : repositoryMap;
    }

    /**
     * Creates last changes from all repositories' last two revisions
     * @param repositories
     * @return
     */
    public Set<LastChanges> changesOf(Map<String, Repository> repositories) {
        Set<LastChanges> lastChangesSet = new HashSet<>();
        for(String key : repositories.keySet()){
            Repository repository = repositories.get(key);
            LastChanges lastChanges = changesOf(repository);
            lastChangesSet.add(lastChanges);
        }
        return lastChangesSet;
    }

    //Todo: refactor with GitLastChanges, perhaps extend it?
    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff
     */
    private LastChanges changesOf(Repository repository) {
        Git git = new Git(repository);
        try {
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            ObjectId head = null;
            try {
                head = repository.resolve("HEAD^{tree}");
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve head of repository located at " + repositoryLocation, e);
            }
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

    //Todo: refactor with GitLastChanges
    /**
     * Creates last changes by "diffing" two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff between revisions
     */
    private LastChanges changesOf(Repository repository, ObjectId currentRevision, ObjectId previousRevision) {
        Git git = new Git(repository);
        try {
            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            CommitInfo lastCommitInfo;
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            DiffFormatter formatter = new DiffFormatter(diffStream);
            formatter.setRepository(repository);
            ObjectReader reader = repository.newObjectReader();
            try {
                lastCommitInfo = CommitInfo.Builder.buildFromGit(repository, currentRevision);
            } catch (Exception e) {
                throw new CommitInfoException("Could not get last commit information", e);
            }

            // Create the tree iterator for each commit
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            try {
                oldTreeIter.reset(reader, previousRevision);
            } catch (Exception e) {
                throw new GitTreeParseException("Could not parse previous commit tree.", e);
            }
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            try {
                newTreeIter.reset(reader, currentRevision);
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

            return new LastChanges(lastCommitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }

    }
}
