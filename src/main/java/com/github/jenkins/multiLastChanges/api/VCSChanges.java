package com.github.jenkins.multiLastChanges.api;

import com.github.jenkins.multiLastChanges.model.MultiLastChanges;

/**
 * Created by rmpestano on 7/10/16.
 */
public interface VCSChanges<REPOSITORY, REVISION> {


    MultiLastChanges getLastChangesOf(REPOSITORY repository);

    MultiLastChanges getLastChanges();
}
