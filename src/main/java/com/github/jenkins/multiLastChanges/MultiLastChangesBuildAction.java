package com.github.jenkins.multiLastChanges;

import com.github.jenkins.multiLastChanges.model.CommitInfo;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import com.github.jenkins.multiLastChanges.model.MultiLastChangesConfig;
import hudson.model.Run;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MultiLastChangesBuildAction extends MultiLastChangesBaseAction{

    private final Run<?, ?> build;
    private List<MultiLastChanges> buildChanges;
    private MultiLastChangesConfig config;
    private List<MultiLastChangesProjectAction> projectActions;

    public MultiLastChangesBuildAction(Run<?, ?> build, MultiLastChanges multiLastChanges, MultiLastChangesConfig config) {
        this.build = build;
        buildChanges = new ArrayList<>();
        buildChanges.add(multiLastChanges);
        if (config == null) {
            config = new MultiLastChangesConfig();
        }
        this.config = config;
        List<MultiLastChangesProjectAction> projectActions = new ArrayList<>();
        projectActions.add(new MultiLastChangesProjectAction(build.getParent()));
        this.projectActions = projectActions;
        removeOldCommitsFromAllBuildChanges();
    }

    public MultiLastChangesBuildAction(Run<?, ?> build, List<MultiLastChanges> multiLastChanges, MultiLastChangesConfig config) {
        this.build = build;
        buildChanges = multiLastChanges;
        if (config == null) {
            config = new MultiLastChangesConfig();
        }
        this.config = config;
        removeOldCommitsFromAllBuildChanges();
    }

    @Override
    protected String getTitle() {
        return "Last Changes of Build #" + this.build.getNumber();
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), BASE_URL);
    }

    public List<MultiLastChanges> getBuildChanges() {
        return buildChanges;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public MultiLastChangesConfig getConfig() {
        return config;
    }

    /**
     * Will remove LastChanges that have a commit date earlier than the last build's time.
     */
    private void removeOldCommitsFromAllBuildChanges(){
        buildChanges.removeIf(l -> {
            boolean notNewCommit = false; //default is no, don't remove
            if (build.getPreviousBuild() == null){ //if there was no previous build, don't remove
                return notNewCommit;
            }
            try{
                Date commitDate = CommitInfo.dateFormat.parse(l.getEndRevision().getCommitDate());
                notNewCommit = commitDate.before(build.getPreviousBuild().getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return notNewCommit;
        });
    }
}
