package com.github.jenkins.multiLastChanges;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCMRevisionState;
import org.apache.commons.io.FileUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by rafael-pestano on 28/06/2016.
 */
public class DirectorySCM extends NullSCM {

    private File sourceDir;
    private String targetWorkspaceDirName;

    public DirectorySCM(String targetWorkspaceDir, File sourceDir) {
        this.targetWorkspaceDirName = targetWorkspaceDir;
        this.sourceDir = sourceDir;
    }

    @Override
    public void checkout(Run<?,?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace, @Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException {

        File workspaceDir = new File(workspace.toURI().getPath());
        FileUtils.copyDirectoryToDirectory(sourceDir, workspaceDir);
        if(targetWorkspaceDirName != null && !"".equals(targetWorkspaceDirName)){
            //rename dest dir
            Path oldName = new File(workspace.toURI().getPath()+"/"+sourceDir.getName()).toPath();
            Files.move(oldName, oldName.resolveSibling(targetWorkspaceDirName));
        }

    }


}
