package com.github.jenkins.multiLastChanges.model;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.Serializable;

/**
 * Created by rmpestano on 7/3/16.
 */
public class MultiLastChanges implements Serializable {

    private CommitInfo currentRevision; //information aboud head commit
    private CommitInfo endRevision;
    private String diff; //diff between head and ´head -1'

    public MultiLastChanges(CommitInfo current, CommitInfo end, String diff) {
        this.currentRevision = current;
        this.endRevision = end;
        this.diff = diff;
    }

    public CommitInfo getCurrentRevision() {
        return currentRevision;
    }

    public CommitInfo getEndRevision() {
        return endRevision;
    }

    public String getDiff() {
        return diff;
    }

    public String getEscapedDiff() {
        if (diff != null) {
            return StringEscapeUtils.escapeEcmaScript(getDiff());
        } else {
            return "";
        }
    }
}
