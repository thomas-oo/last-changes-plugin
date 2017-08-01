package com.github.jenkins.multiLastChanges;

import hudson.Extension;
import hudson.model.RootAction;

@Extension
public class MultiLastChangesRootAction implements RootAction {
    protected static final String BASE_URL = "multi-last-changes";

    public String getUrlName() {
        return BASE_URL;
    }

    public String getDisplayName() {
        return "View Last Changes";
    }

    public String getIconFileName() {
        return "/plugin/multi-last-changes/git.png";
    }
}
