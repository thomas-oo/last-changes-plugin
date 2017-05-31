/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.multiLastChanges.impl;

import com.github.jenkins.multiLastChanges.api.VCSChanges;
import com.github.jenkins.multiLastChanges.exception.RepositoryNotFoundException;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;


public class GitLastChanges implements VCSChanges<Repository, ObjectId> {


    private String projectPath;


    public GitLastChanges(String projectPath) {
        this.projectPath = projectPath;
    }

    @Override
    public MultiLastChanges getLastChanges() {
        return getLastChangesOf(repository(this.projectPath));
    }

    /**
     * @param path local git repository path
     * @return underlying git repository from location path
     */
    public static Repository repository(String path) {
        if (path == null || path.isEmpty()) {
            throw new RepositoryNotFoundException("Git repository path cannot be empty.");
        }

        File repositoryPath = new File(path);

        if (!repositoryPath.exists()) {
            throw new RepositoryNotFoundException(String.format("Git repository path not found at location %s.", repositoryPath));
        }

        Repository repository = null;
        try {
            repository = new FileRepository(path);
        } catch (IOException e) {
            throw new RepositoryNotFoundException("Could not find git repository at " + path);
        }
        if (repository.isBare()) {
            throw new RepositoryNotFoundException(String.format("No git repository found at %s.", path));
        }

        return repository;

    }

    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff
     */
    @Override
    public MultiLastChanges getLastChangesOf(Repository repository) {
        return SCMUtils.changesOf(repository);
    }
}
