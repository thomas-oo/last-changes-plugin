package com.github.jenkins.multiLastChanges.exception;

/**
 * Created by rafael-pestano on 27/06/2016.
 */
public class GitDiffException extends LastChangesException {

    public GitDiffException() {
    }

    public GitDiffException(String message) {
        super(message);
    }

    public GitDiffException(String message, Throwable cause) {
        super(message, cause);
    }
}
