package com.github.jenkins.multiLastChanges;

import com.github.jenkins.multiLastChanges.model.CommitInfo;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import com.github.jenkins.multiLastChanges.model.MultiLastChangesConfig;
import hudson.model.Run;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MultiLastChangesBuildAction extends MultiLastChangesBaseAction {

    private final Run<?, ?> build;
    private MultiLastChanges buildChanges;
    private Set<MultiLastChanges> allBuildChanges;
    private MultiLastChangesConfig config;

    public MultiLastChangesBuildAction(Run<?, ?> build, MultiLastChanges multiLastChanges, MultiLastChangesConfig config) {
        this.build = build;
        buildChanges = multiLastChanges;
        allBuildChanges = new HashSet<>();
        allBuildChanges.add(buildChanges);
        if (config == null) {
            config = new MultiLastChangesConfig();
        }
        this.config = config;
        removeOldCommitsFromAllBuildChanges();
    }

    public MultiLastChangesBuildAction(Run<?, ?> build, Set<MultiLastChanges> multiLastChanges, MultiLastChangesConfig config) {
        this.build = build;
        allBuildChanges = multiLastChanges;
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

    public MultiLastChanges getBuildChanges() {
        return buildChanges;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public MultiLastChangesConfig getConfig() {
        return config;
    }

    public Set<MultiLastChanges> getAllBuildChanges(){
        return allBuildChanges;
    }

    /**
     * Will remove LastChanges that have a commit date earlier than the last build's time.
     * Todo: Decide if this is a useful feature or not
     */
    private void removeOldCommitsFromAllBuildChanges(){
        allBuildChanges.removeIf(l -> {
            boolean notNewCommit = false; //default is no, don't remove
            if (build.getPreviousBuild() == null){ //if there was no previous build, don't remove
                return notNewCommit;
            }
            try {
                Date commitDate = CommitInfo.dateFormat.parse(l.getCommitInfo().getCommitDate());
                notNewCommit = commitDate.before(build.getPreviousBuild().getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return notNewCommit;
        });
    }
}
