package com.github.jenkins.multiLastChanges.model;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Created by rmpestano on 6/26/16.
 */
public class CommitInfo implements Serializable {

    public static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
    private static final String newLine = System.getProperty("line.separator");

    private String commitId;
    private String commitMessage;
    private String commiterName;
    private String commiterEmail;
    private String commitDate;

    private CommitInfo() {
    }

    public String getCommiterName() {
        return commiterName;
    }

    public String getCommitDate() {
        return commitDate;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getCommiterEmail() {
        return commiterEmail;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().
                append("Commit: ").append(commitId).append(newLine).
                append("Author: " + commiterName).append(newLine).
                append("E-mail: ").append(commiterEmail).append(newLine).
                append("Date: ").append(commitDate).append(newLine).
                append("Message: ").append(commitMessage).append(newLine).append(newLine);

        return sb.toString();
    }


    public static class Builder {

        public static CommitInfo buildFromSvn(SVNRepository repository, long revision) throws SVNException {
            CommitInfo commitInfo = new CommitInfo();
            try {
                Collection<SVNLogEntry> entries = repository.log(new String[]{""}, null, revision, revision, true, true);
                Iterator<SVNLogEntry> iterator = entries.iterator();
                SVNLogEntry logEntry = iterator.next();
                TimeZone tz = TimeZone.getDefault();
                dateFormat.setTimeZone(tz);
                commitInfo.commitDate = dateFormat.format(logEntry.getDate()) + " " + tz.getDisplayName();
                commitInfo.commiterName = logEntry.getAuthor();
                commitInfo.commitId = logEntry.getRevision() + "";
                commitInfo.commitMessage = logEntry.getMessage();
            } catch (Exception e) {
                Logger.getLogger(CommitInfo.class.getName()).warning(String.format("Could not get commit info from revision %d due to following error " + e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), revision));
            }
            return commitInfo;
        }

        public static CommitInfo buildFromGit(Repository repository, ObjectId commitId) {
            RevWalk revWalk = new RevWalk(repository);
            CommitInfo commitInfo = new CommitInfo();
            PersonIdent committerIdent = null;
            RevCommit commit = null;
            try {
                if (revWalk.parseAny(commitId) instanceof RevCommit) {
                    commit = revWalk.parseCommit(commitId);
                    committerIdent = commit.getCommitterIdent();
                } else if(revWalk.parseAny(commitId) instanceof RevTree) {

                    RevCommit rootCommit = revWalk.parseCommit(repository.resolve(Constants.HEAD));
                    revWalk.sort(RevSort.COMMIT_TIME_DESC);
                    revWalk.markStart(rootCommit);
                    //resolve commit from tree
                    RevTree tree = revWalk.parseTree(commitId);
                    for (RevCommit revCommit : revWalk) {
                        if(revCommit.getTree().getId().equals(tree.getId())){
                            commit = revCommit;
                            committerIdent = commit.getCommitterIdent();
                            break;
                        }
                    }

                }

                Date commitDate = committerIdent.getWhen();
                commitInfo.commitId = commit.getName();
                commitInfo.commitMessage = commit.getFullMessage();
                commitInfo.commiterName = committerIdent.getName();
                commitInfo.commiterEmail = committerIdent.getEmailAddress();
                TimeZone tz = committerIdent.getTimeZone() != null ? committerIdent.getTimeZone() : TimeZone.getDefault();
                dateFormat.setTimeZone(tz);
                commitInfo.commitDate = dateFormat.format(commitDate) + " " + tz.getDisplayName();
            } catch (Exception e) {
                Logger.getLogger(CommitInfo.class.getName()).warning(String.format("Could not get commit info from revision %d due to following error " + e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), commitId));
            } finally {
                  revWalk.dispose();
            }
            return commitInfo;
        }

    }

}
