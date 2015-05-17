package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.utils.TemplatedUriResolver;
import hudson.model.AbstractBuild;
import hudson.model.Run;

public class JenkinsConnector implements ScmConnector {
    private final TemplatedUriResolver uriResolver;
    private ScmConfiguration config;

    public JenkinsConnector() {
        this.uriResolver = new TemplatedUriResolver();
    }

    public void setScmConfiguration(ScmConfiguration configuration) {
        this.config = configuration;
    }

    public Changes getChangesSinceLastBuild(AbstractBuild build) {
        Run run = build.getPreviousBuild();
        return new Changes(build, run != null ? run.getNumber() + 1 : build.getNumber());
    }

    public String getRevisionUriFor(String revision) {
        return this.uriResolver.resolveCommitUri(this.config.getCommitTemplate(), revision);
    }
}
