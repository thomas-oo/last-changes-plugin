package com.github.jenkins.multiLastChanges.impl;


import com.github.jenkins.multiLastChanges.exception.RepositoryNotFoundException;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Todo: Create a test suite
public class MultiScmLastChanges {

    private String projectPath;

    public MultiScmLastChanges(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    /**
     *
     * @return A set of LastChanges for every git repo in path
     */
    public List<MultiLastChanges> getLastChanges(){
        return getLastChangesOf(repositories(this.projectPath));
    }

    /**
     * Traverses all folders and find git repos, returning them in a map.
     *
     * @param path Path that holds multiple git repos. May be a remote path
     * @return A map of a the git paths to a repository object
     */
    private static Map<String,Repository> repositories(String path){
        List<String> repoPaths = SCMUtils.findPathsOfGitRepos(path);
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
        return repositoryMap;
    }

    /**
     * Generates LastChanges objects for every repository in the map
     * @param repositories A map of a git path to a repository object
     * @return A set of LastChanges objects
     */
    private List<MultiLastChanges> getLastChangesOf(Map<String, Repository> repositories) {
        List<MultiLastChanges> multiLastChangesList = new ArrayList<>();
        for(String key : repositories.keySet()){
            //Todo: include the key in the LastChanges object so we can use it
            Repository repository = repositories.get(key);
            MultiLastChanges multiLastChanges = SCMUtils.changesOf(repository);
            multiLastChangesList.add(multiLastChanges);
        }
        return multiLastChangesList;
    }
}
