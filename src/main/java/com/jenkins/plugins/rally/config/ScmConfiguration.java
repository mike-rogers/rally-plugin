package com.jenkins.plugins.rally.config;

import com.jenkins.plugins.rally.utils.TemplatedUriResolver;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class ScmConfiguration implements Describable<ScmConfiguration> {
    private final String commitTemplate;
    private final String fileTemplate;

    @DataBoundConstructor
    public ScmConfiguration(String commitTemplate, String fileTemplate) {
        this.commitTemplate = commitTemplate;
        this.fileTemplate = fileTemplate;
    }

    public String getCommitTemplate() {
        return commitTemplate;
    }

    public String getFileTemplate() {
        return fileTemplate;
    }

    public Descriptor<ScmConfiguration> getDescriptor() {
        return new ScmConfigurationDescriptor();
    }

    @Extension
    public static final class ScmConfigurationDescriptor extends Descriptor<ScmConfiguration> {
        public String getDisplayName() {
            return "SCM Information";
        }
    }
}
