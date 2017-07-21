/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.multiLastChanges.impl;

import com.github.jenkins.multiLastChanges.api.VCSChanges;
import com.github.jenkins.multiLastChanges.exception.RepositoryNotFoundException;
import com.github.jenkins.multiLastChanges.model.CommitInfo;
import com.github.jenkins.multiLastChanges.model.MultiLastChanges;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.scm.SubversionSCM;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class SvnLastChanges implements VCSChanges<SVNRepository, Long> {

    private AbstractProject<?, ?> rootProject;
    private SubversionSCM scm;

    public SvnLastChanges(AbstractProject<?, ?> rootProject, SubversionSCM scm) {
        this.rootProject = rootProject;
        this.scm = scm;
    }

    @Override
    public MultiLastChanges getLastChanges() {
        return getLastChangesOf(getRepository());
    }

    public SVNRepository getRepository() {
        String path = null;
        try {
            path = scm.getLocations()[0].getURL();
            ISVNAuthenticationProvider svnAuthProvider;
            try{
                svnAuthProvider = scm.createAuthenticationProvider(rootProject, scm.getLocations()[0]);
            } catch (NoSuchMethodError e) {
                //fallback for versions under 2.x of org.jenkins-ci.plugins:subversion
                svnAuthProvider = scm.getDescriptor().createAuthenticationProvider(rootProject);
            }
            ISVNAuthenticationManager svnAuthManager = SVNWCUtil.createDefaultAuthenticationManager();
            svnAuthManager.setAuthenticationProvider(svnAuthProvider);
            SVNClientManager svnClientManager = SVNClientManager.newInstance(null, svnAuthManager);
            return svnClientManager.createRepository(SVNURL.parseURIEncoded(path), false);
        } catch (Exception e) {
            throw new RepositoryNotFoundException("Could not find svn repository at " + path, e);
        }
    }

    public static SVNRepository repository(SubversionSCM scm, Job<?, ?> job) {

        String path = null;
        try {
            path = scm.getLocations()[0].getURL();
            ISVNAuthenticationProvider svnAuthProvider;
            try{
                svnAuthProvider = scm.createAuthenticationProvider(job, scm.getLocations()[0]);
            } catch (NoSuchMethodError e) {
                //fallback for versions under 2.x of org.jenkins-ci.plugins:subversion
                svnAuthProvider = scm.getDescriptor().createAuthenticationProvider();
            }
            ISVNAuthenticationManager svnAuthManager = SVNWCUtil.createDefaultAuthenticationManager();
            svnAuthManager.setAuthenticationProvider(svnAuthProvider);
            SVNClientManager svnClientManager = SVNClientManager.newInstance(null, svnAuthManager);
            return svnClientManager.createRepository(SVNURL.parseURIEncoded(path), false);
        } catch (Exception e) {
            throw new RepositoryNotFoundException("Could not find svn repository at " + path, e);
        }
    }


    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository svn repository to get last changes
     * @return LastChanges commit info and svn diff
     */
    @Override
    public MultiLastChanges getLastChangesOf(SVNRepository repository) {
         try {
            return changesOf(repository, repository.getLatestRevision(), repository.getLatestRevision() - 1);
        } catch (SVNException e) {
            throw new RuntimeException("Could not retrieve lastest revision of svn repository located at " + repository.getLocation().getPath() + " due to following error: "+e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), e);
        }
    }

    public MultiLastChanges getChangesOf(SVNRepository repository, long startRevision, long endRevision){
        return changesOf(repository, startRevision, endRevision);
    }

    /**
     * Creates last changes from two revisions of repository
     *
     * @param repository
     *            svn repository to get last changes
     * @return LastChanges commit info and svn diff
     */
    public MultiLastChanges changesOf(SVNRepository repository, Long currentRevision, Long previousRevision) {
    	ByteArrayOutputStream diffStream = null;
        try {
            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File(""));
            diffStream = new ByteArrayOutputStream();
            final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            svnOperationFactory.setAuthenticationManager(repository.getAuthenticationManager());
            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSources(SvnTarget.fromURL(repository.getLocation(), SVNRevision.create(previousRevision)),
                    SvnTarget.fromURL(repository.getLocation(), SVNRevision.create(currentRevision)));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(diffStream);
            diff.run();

            CommitInfo lastCommitInfo = CommitInfo.Builder.buildFromSvn(repository,currentRevision);
            CommitInfo oldCommitInfo = CommitInfo.Builder.buildFromSvn(repository,previousRevision);


            return new MultiLastChanges(lastCommitInfo, oldCommitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve last changes of svn repository located at " + repository.getLocation().getPath() + " due to following error: "+e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), e);

        }
        finally {
			if(diffStream != null) {
				try {
					diffStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
				
		}
    }

}
