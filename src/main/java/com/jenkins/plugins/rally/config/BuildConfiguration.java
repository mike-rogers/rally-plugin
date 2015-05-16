package com.jenkins.plugins.rally.config;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class BuildConfiguration implements Describable<BuildConfiguration> {
    private final ScmChangeCaptureRange captureRange;

    @DataBoundConstructor
    public BuildConfiguration(String captureRange) {
        this.captureRange = ScmChangeCaptureRange.valueOf(captureRange);
    }

    public String getCaptureRange() {
        return captureRange.getValue();
    }

    public Descriptor<BuildConfiguration> getDescriptor() {
        return new BuildConfigurationDescriptor();
    }

    @Extension
    public static final class BuildConfigurationDescriptor extends Descriptor<BuildConfiguration> {
        public String getDisplayName() {
            return "Build Information";
        }
    }
}
