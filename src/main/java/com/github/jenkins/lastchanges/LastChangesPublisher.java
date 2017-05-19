/*
 * The MIT License
 *
 * Copyright 2016 rmpestano.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.impl.MultiScmLastChanges;
import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import com.github.jenkins.lastchanges.model.MatchingType;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;

/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep {

    private static final short RECURSION_DEPTH = 50;

    private FormatType format;

    private MatchingType matching;

    private Boolean showFiles;

    private Boolean synchronisedScroll;

    private String matchWordsThreshold;

    private String matchingMaxComparisons;

    private LastChangesProjectAction lastChangesProjectAction;

    private static final String GIT_DIR = ".git";

    @DataBoundConstructor
    public LastChangesPublisher(FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold,
                                String matchingMaxComparisons) {
        this.format = format;
        this.matching = matching;
        this.showFiles = showFiles;
        this.synchronisedScroll = synchronisedScroll;
        this.matchWordsThreshold = matchWordsThreshold;
        this.matchingMaxComparisons = matchingMaxComparisons;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        if (lastChangesProjectAction == null) {
            lastChangesProjectAction = new LastChangesProjectAction(project);
        }
        return lastChangesProjectAction;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        boolean isGit = ((AbstractProject) lastChangesProjectAction.getProject()).getScm() instanceof GitSCM;
        boolean isSvn = ((AbstractProject) lastChangesProjectAction.getProject()).getScm() instanceof SubversionSCM;
        boolean isMultiScm = ((AbstractProject) lastChangesProjectAction.getProject()).getScm() instanceof MultiSCM;

        if (!isGit && !isSvn && !isMultiScm) {
            throw new RuntimeException("Git/Svn/MultiSCM must be configured on your job to publish Last Changes.");
        }

        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);//always on master

        try {
            LastChanges lastChanges = null;
            Set<LastChanges> lastChangesSet = new HashSet<>();
            listener.getLogger().println("Publishing build last changes...");
            if (isGit) {
                //gitDir is the path to the .git file
                FilePath gitDir = workspace.child(GIT_DIR).exists() ? workspace.child(GIT_DIR) : findGitDir(workspace);
                // workspace can be on slave so copy resources to master
                // we are only copying when on git because in svn we are reading
                // the revision from remote repository
                gitDir.copyRecursiveTo("**/*", new FilePath(new File(workspaceTargetDir.getRemote() + "/.git")));
                lastChanges = GitLastChanges.getInstance().changesOf(repository(workspaceTargetDir.getRemote() + "/.git"));
            } else if (isSvn){
                AbstractProject<?, ?> rootProject = (AbstractProject<?, ?>) lastChangesProjectAction.getProject();
                SubversionSCM scm = SubversionSCM.class.cast(rootProject.getScm());
                lastChanges = SvnLastChanges.getInstance().changesOf(SvnLastChanges.repository(scm, rootProject));
            } else {
                MultiScmLastChanges multiScmLastChanges = new MultiScmLastChanges(workspace.getRemote());
                lastChangesSet = multiScmLastChanges.getLastChanges();
            }

            listener.hyperlink("../" + build.getNumber() + "/" + LastChangesBaseAction.BASE_URL, "Last changes published successfully!");
            listener.getLogger().println("");
            if(!isMultiScm){
                build.addAction(new LastChangesBuildAction(build, lastChanges,
                        new LastChangesConfig(format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons)));
            }else{
                build.addAction(new LastChangesBuildAction(build, lastChangesSet,
                        new LastChangesConfig(format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons)));
            }
        } catch (Exception e) {
            listener.error("Last Changes NOT published due to the following error: " + e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""));
            e.printStackTrace();
        }
        // always success (only warn when no diff was generated)

        build.setResult(Result.SUCCESS);

    }

    /**
     * .git directory can be on a workspace sub dir, see JENKINS-36971
     */
    private FilePath findGitDir(FilePath workspace) throws IOException, InterruptedException {
        FilePath gitDir = null;
        int recusursionDepth = RECURSION_DEPTH;
        while ((gitDir = findGitDirInSubDirectories(workspace)) == null && recusursionDepth > 0){
            recusursionDepth --;
        }
        if(gitDir == null){
            throw new RuntimeException("No .git directory found in workspace.");
        }
        return gitDir;
    }

    private FilePath findGitDirInSubDirectories(FilePath sourceDir) throws IOException, InterruptedException {
        for (FilePath filePath : sourceDir.listDirectories()) {
            if(filePath.getName().equalsIgnoreCase(GIT_DIR)){
                return filePath;
            } else{
                return findGitDirInSubDirectories(filePath);
            }
        }
        return null;
    }

    /**
     * mainly for findbugs be happy
     *
     * @param build
     * @return
     */
    private FilePath getMasterWorkspaceDir(Run<?, ?> build) {
        if (build != null && build.getRootDir() != null) {
            return new FilePath(build.getRootDir());
        } else {
            return new FilePath(Paths.get("").toFile());
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Publish Last Changes";
        }

        public ListBoxModel doFillFormatItems() {
            ListBoxModel items = new ListBoxModel();
            for (FormatType formatType : FormatType.values()) {
                items.add(formatType.getFormat(), formatType.name());
            }
            return items;
        }

        public ListBoxModel doFillMatchingItems() {
            ListBoxModel items = new ListBoxModel();
            for (MatchingType matchingType : MatchingType.values()) {
                items.add(matchingType.getMatching(), matchingType.name());
            }
            return items;
        }

    }

    public FormatType getFormat() {
        return format;
    }

    public MatchingType getMatching() {
        return matching;
    }

    public String getMatchWordsThreshold() {
        return matchWordsThreshold;
    }

    public String getMatchingMaxComparisons() {
        return matchingMaxComparisons;
    }

    public Boolean getShowFiles() {
        return showFiles;
    }

    public Boolean getSynchronisedScroll() {
        return synchronisedScroll;
    }

}
