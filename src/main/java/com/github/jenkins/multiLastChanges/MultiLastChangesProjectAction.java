package com.github.jenkins.multiLastChanges;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultiLastChangesProjectAction extends MultiLastChangesBaseAction implements ProminentProjectAction {

    private final AbstractProject<?, ?> project;

    private String jobName;

    public MultiLastChangesProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public String job() {
        if (jobName == null) {
            jobName = project.getName();
        }
        return jobName;
    }

    public Job<?, ?> getProject() {
        return project;
    }

    @Override
    protected File dir() {
        Run<?, ?> run = this.project.getLastCompletedBuild();
        if (run != null) {
            File archiveDir = getBuildArchiveDir(run);

            if (archiveDir.exists()) {
                return archiveDir;
            }
        }

        return getProjectArchiveDir();
    }

    private File getProjectArchiveDir() {
        return new File(project.getRootDir(), MultiLastChangesBaseAction.BASE_URL);
    }

    /**
     * Gets the directory where the HTML report is stored for the given build.
     */
    private File getBuildArchiveDir(Run<?, ?> run) {
        return new File(run.getRootDir(), MultiLastChangesBaseAction.BASE_URL);
    }

    @Override
    protected String getTitle() {
        return this.project.getDisplayName();
    }

    public List<Run<?, ?>> getLastChangesBuilds() {
        List<Run<?, ?>> builds = new ArrayList<>();

        for (Run<?, ?> build : project.getBuilds()) {
            MultiLastChangesBuildAction action = build.getAction(MultiLastChangesBuildAction.class);
            if (action != null) {
                builds.add(build);
            }
        }

        return builds;
    }

}