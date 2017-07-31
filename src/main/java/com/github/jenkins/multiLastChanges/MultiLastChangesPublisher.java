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
package com.github.jenkins.multiLastChanges;


import com.github.jenkins.multiLastChanges.impl.GitLastChanges;
import com.github.jenkins.multiLastChanges.impl.MultiScmLastChanges;
import com.github.jenkins.multiLastChanges.impl.SCMUtils;
import com.github.jenkins.multiLastChanges.impl.SvnLastChanges;
import com.github.jenkins.multiLastChanges.model.FormatType;
import com.github.jenkins.multiLastChanges.model.MatchingType;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import com.github.jenkins.multiLastChanges.model.MultiLastChangesConfig;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.remoting.Callable;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DirScanner;
import hudson.util.ListBoxModel;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;
import jenkins.triggers.SCMTriggerItem;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rmpestano
 */
public class MultiLastChangesPublisher extends Recorder implements SimpleBuildStep, Serializable {

    private static final short RECURSION_DEPTH = 50;

    private String endRevision;

    private FormatType format;

    private MatchingType matching;

    private Boolean showFiles;

    private Boolean synchronisedScroll;

    private String matchWordsThreshold;

    private String matchingMaxComparisons;

    private static final String GIT_DIR = ".git";

    @DataBoundConstructor
    public MultiLastChangesPublisher(FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold,
                                String matchingMaxComparisons, String endRevision) {
        this.endRevision = endRevision;
        this.format = format;
        this.matching = matching;
        this.showFiles = showFiles;
        this.synchronisedScroll = synchronisedScroll;
        this.matchWordsThreshold = matchWordsThreshold;
        this.matchingMaxComparisons = matchingMaxComparisons;
    }


    //TODO:configure so it generates reports for all git repos in workflow
    //Assumes that in a workflow, all the SCM types are the same.
    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        MultiLastChangesProjectAction projectAction = new MultiLastChangesProjectAction(build.getParent());
        boolean isGit = false;
        boolean isSvn = false;
        boolean isMultiScm = false;
        boolean isPipeline = projectAction.isRunningInPipelineWorkflow();
        if (isPipeline) {
            WorkflowJob workflowJob = (WorkflowJob) projectAction.getProject();
            Collection<? extends SCM> scms = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(projectAction.getProject()).getSCMs();
            for (SCM scm : scms) {
                if (scm instanceof GitSCM) {
                    isGit = true;
                    break;
                }

                if (scm instanceof SubversionSCM) {
                    isSvn = true;
                    break;
                }
            }
        } else if (projectAction.getProject() instanceof AbstractProject) { // non pipeline build
            isGit = ((AbstractProject) projectAction.getProject()).getScm() instanceof GitSCM;
            isSvn = ((AbstractProject) projectAction.getProject()).getScm() instanceof SubversionSCM;
            isMultiScm = ((AbstractProject) projectAction.getProject()).getScm() instanceof MultiSCM;
        }
        if (!isGit && !isSvn && !isMultiScm) {
            throw new RuntimeException("Git/Svn/MultiSCM must be configured on your job to publish Last Changes.");
        }
        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);
        listener.getLogger().println("Publishing build last changes...");
        List<MultiLastChanges> multiLastChangesList = new ArrayList<>();
        if(isPipeline){
            if(isGit){
                DirScanner.Glob dirScanner = new DirScanner.Glob("**/.git/**", null, false);
                FilePath gitFolderOnSlave = new FilePath(workspace, "gitFolders");
                gitFolderOnSlave.mkdirs();
                workspace.copyRecursiveTo(dirScanner, gitFolderOnSlave, "Git folders");

                Callable<List<MultiLastChanges>, IOException> getMultiLastChangesOnSlave = new MasterToSlaveCallable<List<MultiLastChanges>, IOException>() {
                    public List<MultiLastChanges> call() throws IOException{
                        List<MultiLastChanges> multiLastChangesList = new ArrayList<>();
                        List<String> gitDirPaths = SCMUtils.findPathsOfGitRepos(gitFolderOnSlave.getRemote());
                        //Supplying a revisionID will be ignored if there are more than one gitDir as it's unlikely all of them have the same change id
                        if(endRevision != null && !"".equalsIgnoreCase(endRevision.trim()) && gitDirPaths.size()>1){
                            listener.getLogger().println("End revision ignored as multiple repos detected.");
                        }
                        for(String gitDirPath : gitDirPaths){
                            GitLastChanges gitLastChanges = new GitLastChanges(gitDirPath);
                            MultiLastChanges multiLastChanges = gitLastChanges.getLastChanges();
                            multiLastChangesList.add(multiLastChanges);
                        }
                        return multiLastChangesList;
                    }
                };

                List<MultiLastChanges> multiLastChangesFromSlave = launcher.getChannel().call(getMultiLastChangesOnSlave);
                multiLastChangesList.addAll(multiLastChangesFromSlave);
            }else{
                //TODO: implement way to handle potentially multiple SVN projects
                throw new RuntimeException("Currently pipeline with SVN/MultiSCM projects are not supported on your job to publish Last Changes.");
            }
        }else {
            if (isGit) {
                DirScanner.Glob dirScanner = new DirScanner.Glob("**/.git/**", null, false);
                FilePath gitFolderOnSlave = new FilePath(workspace, "gitFolders");
                gitFolderOnSlave.mkdirs();
                workspace.copyRecursiveTo(dirScanner, gitFolderOnSlave, "Git folders");

                Callable<List<MultiLastChanges>, IOException> getMultiLastChangesOnSlave = new MasterToSlaveCallable<List<MultiLastChanges>, IOException>() {
                    public List<MultiLastChanges> call() throws IOException{
                        List<MultiLastChanges> multiLastChangesList = new ArrayList<>();
                        List<String> gitDirPaths = SCMUtils.findPathsOfGitRepos(gitFolderOnSlave.getRemote());
                        GitLastChanges gitLastChanges = new GitLastChanges(gitDirPaths.get(0));
                        if(endRevision != null && !"".equalsIgnoreCase(endRevision.trim()) && gitDirPaths.size()>1){
                            MultiLastChanges multiLastChanges = gitLastChanges.getChangesOf(gitLastChanges.getCurrentRevision(), ObjectId.fromString(endRevision));
                            multiLastChangesList.add(multiLastChanges);
                        } else {
                            MultiLastChanges multiLastChanges = gitLastChanges.getLastChanges();
                            multiLastChangesList.add(multiLastChanges);
                        }
                        return multiLastChangesList;
                    }
                };

                List<MultiLastChanges> multiLastChangesFromSlave = launcher.getChannel().call(getMultiLastChangesOnSlave);
                multiLastChangesList.addAll(multiLastChangesFromSlave);
            } else if (isSvn) {
                AbstractProject<?, ?> rootProject = (AbstractProject<?, ?>) projectAction.getProject();
                SubversionSCM scm = SubversionSCM.class.cast(rootProject.getScm());
                SvnLastChanges svnLastChanges = new SvnLastChanges(rootProject, scm);
                if (endRevision != null && !"".equals(endRevision.trim())) {
                    Long svnRevision = Long.parseLong(endRevision);
                    SVNRepository repository = SvnLastChanges.repository(scm, projectAction.getProject());
                    try {
                        MultiLastChanges multiLastChanges = svnLastChanges.getChangesOf(repository, repository.getLatestRevision(), svnRevision);
                        multiLastChangesList.add(multiLastChanges);
                    } catch (SVNException e) {
                        e.printStackTrace();
                    }
                } else {
                    MultiLastChanges multiLastChanges = svnLastChanges.getLastChangesOf(SvnLastChanges.repository(scm, projectAction.getProject()));
                    multiLastChangesList.add(multiLastChanges);
                }
            } else {
                DirScanner.Glob dirScanner = new DirScanner.Glob("**/.git/**", null, false);
                FilePath gitFolderOnSlave = new FilePath(workspace, "gitFolders");
                gitFolderOnSlave.mkdirs();
                workspace.copyRecursiveTo(dirScanner, gitFolderOnSlave, "Git folders");
                Callable<List<MultiLastChanges>, IOException> getMultiLastChangesOnSlave = new MasterToSlaveCallable<List<MultiLastChanges>, IOException>() {
                    public List<MultiLastChanges> call() throws IOException{
                        List<MultiLastChanges> multiLastChangesList = new ArrayList<>();
                        MultiScmLastChanges multiScmLastChanges = new MultiScmLastChanges(gitFolderOnSlave.getRemote());
                        if(endRevision != null && !"".equalsIgnoreCase(endRevision.trim())){
                            listener.getLogger().println("End revision ignored as multiple repos detected.");
                        }
                        multiLastChangesList.addAll(multiScmLastChanges.getLastChanges());
                        return multiLastChangesList;
                    }
                };
                List<MultiLastChanges> multiLastChangesFromSlave = launcher.getChannel().call(getMultiLastChangesOnSlave);
                multiLastChangesList.addAll(multiLastChangesFromSlave);
            }
        }
        MultiLastChangesBuildAction multiLastChangesBuildAction = new MultiLastChangesBuildAction(build, multiLastChangesList,
                new MultiLastChangesConfig(endRevision, format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons));
        build.addAction(multiLastChangesBuildAction);
        listener.hyperlink("../" + build.getNumber() + "/" + MultiLastChangesBaseAction.BASE_URL, "Last changes published successfully!");
        listener.getLogger().println("");
        //can clean up now
        build.setResult(Result.SUCCESS);
    }

    /**
     * .git directory can be on a workspace sub dir, see JENKINS-36971
     */
    private FilePath findGitDir(FilePath workspace) throws IOException, InterruptedException {
        FilePath gitDir = null;
        int recusursionDepth = RECURSION_DEPTH;
        while ((gitDir = findGitDirInSubDirectories(workspace)) == null && recusursionDepth > 0) {
            recusursionDepth--;
        }
        if (gitDir == null) {
            throw new RuntimeException("No .git directory found in workspace.");
        }
        return gitDir;
    }

    private FilePath findGitDirInSubDirectories(FilePath sourceDir) throws IOException, InterruptedException {
        for (FilePath filePath : sourceDir.listDirectories()) {
            if (filePath.getName().equalsIgnoreCase(GIT_DIR)) {
                return filePath;
            } else {
                FilePath gitDir = findGitDirInSubDirectories(filePath);
                if(gitDir==null){
                    continue;
                }else{
                    return gitDir;
                }
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
            return "Publish Multi Last Changes";
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

    public String getEndRevision() {
        return endRevision;
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
