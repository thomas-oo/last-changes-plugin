package com.github.jenkins.lastchanges.impl;


import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Todo: Create a test suite
public class MultiScmLastChanges {

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

    /**
     * Traverses all folders and find git repos, returning them in a map.
     *
     * @param path Path that holds multiple git repos. May be a remote path
     * @return A map of a the git paths to a repository object
     */
    private static Map<String,Repository> repositories(String path){
        List<String> repoPaths = findPathsOfGitRepos(path);
        Map<String, Repository> repositoryMap = new HashMap<>();
        for(String repoPath : repoPaths){
            File repoFile = new File(repoPath);
            Repository repository;
            try {
                repository = new FileRepository(repoPath);
                repositoryMap.put(repoFile.getParent(),repository);
            } catch (IOException e) {
                throw new RepositoryNotFoundException("Could not find git repository at " + path);
            }
            if (repository.isBare()) {
                throw new RepositoryNotFoundException(String.format("No git repository found at %s.", path));
            }
        }
        return repositoryMap.isEmpty() ? null : repositoryMap;
    }

    /**
     * Generates LastChanges objects for every repository in the map
     * @param repositories A map of a git path to a repository object
     * @return A set of LastChanges objects
     */
    private Set<LastChanges> getChangesOf(Map<String, Repository> repositories) {
        Set<LastChanges> lastChangesSet = new HashSet<>();
        for(String key : repositories.keySet()){
            Repository repository = repositories.get(key);
            LastChanges lastChanges = SCMUtils.changesOf(repository);
            lastChangesSet.add(lastChanges);
        }
        return lastChangesSet;
    }

    /**
     *
     * @param path Parent folder where you want to find all LastChanges objects
     * @return A set of LastChanges for every git repo in path
     */
    public Set<LastChanges> getChangesOf(String path){
        return getChangesOf(repositories(path));
    }
}
