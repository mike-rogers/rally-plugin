package com.jenkins.plugins.rally.config;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class RallyConfiguration implements Describable<RallyConfiguration> {
    private final String apiKey;
    private final String workspaceName;
    private final String scmName;

    @DataBoundConstructor
    public RallyConfiguration(String apiKey, String workspaceName, String scmName) {
        this.apiKey = apiKey;
        this.workspaceName = workspaceName;
        this.scmName = scmName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getScmName() {
        return scmName;
    }

    public Descriptor<RallyConfiguration> getDescriptor() {
        return new RallyConfigurationDescriptor();
    }

    @Extension
    public static final class RallyConfigurationDescriptor extends Descriptor<RallyConfiguration> {
        public String getDisplayName() {
            return "Rally Information";
        }
    }
}
