package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import hudson.model.Run;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class LastChangesBuildAction extends LastChangesBaseAction {

    private final Run<?, ?> build;
    private LastChanges buildChanges;
    private Set<LastChanges> allBuildChanges;
    private LastChangesConfig config;

    public LastChangesBuildAction(Run<?, ?> build, LastChanges lastChanges, LastChangesConfig config) {
        this.build = build;
        buildChanges = lastChanges;
        allBuildChanges = new HashSet<>();
        allBuildChanges.add(buildChanges);
        if (config == null) {
            config = new LastChangesConfig();
        }
        this.config = config;
    }

    public LastChangesBuildAction(Run<?, ?> build, Set<LastChanges> lastChanges, LastChangesConfig config) {
        this.build = build;
        allBuildChanges = lastChanges;
        if (config == null) {
            config = new LastChangesConfig();
        }
        this.config = config;
    }

    @Override
    protected String getTitle() {
        return "Last Changes of Build #" + this.build.getNumber();
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), BASE_URL);
    }

    public LastChanges getBuildChanges() {
        return buildChanges;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public LastChangesConfig getConfig() {
        return config;
    }

    public Set<LastChanges> getAllBuildChanges(){
        return allBuildChanges;
    }


}
