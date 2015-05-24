package com.jenkins.plugins.rally.scm;

import hudson.model.AbstractBuild;
import hudson.model.Api;
import hudson.scm.ChangeLogSet;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.LinkedList;
import java.util.List;

@ExportedBean
public class Changes {

    private List<ChangeInformation> changeInformation = new LinkedList<ChangeInformation>();

    private final AbstractBuild build;

    public Changes(AbstractBuild build, int buildNumber) {
        this.build = build;
        AbstractBuild b = build;
        // TODO: is this logic necessary? if so, write a test.
        while (b != null && b.getNumber() >= buildNumber) {
            populateChangeInformation(b, b.getChangeSet());
            b = b.getPreviousBuild();
        }
    }

    private void populateChangeInformation(AbstractBuild build, ChangeLogSet changeLogSet) {
        ChangeInformation ci = new ChangeInformation();
        ci.setBuildNumber(String.valueOf(build.getNumber()));
        ci.setBuildTimeStamp(build.getTimestampString2());
        ci.setChangeLogSet(changeLogSet);
        ci.setBuild(build);
        changeInformation.add(ci);
    }

    /**
     * Remote API access.
     */
    public final Api getApi() {
        return new Api(this);
    }

    public AbstractBuild getBuild() {
        return build;
    }

    @Exported
    public List<ChangeInformation> getChangeInformation() {
        return changeInformation;
    }
}
