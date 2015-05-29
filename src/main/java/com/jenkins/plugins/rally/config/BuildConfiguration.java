package com.jenkins.plugins.rally.config;

import com.google.inject.Inject;

public class BuildConfiguration {
    private final ScmChangeCaptureRange captureRange;

    @Inject
    public BuildConfiguration(String captureRange) {
        this.captureRange = ScmChangeCaptureRange.valueOf(captureRange);
    }

    public String getCaptureRange() {
        return this.captureRange.name();
    }

    public ScmChangeCaptureRange getCaptureRangeAsEnum() {
        return this.captureRange;
    }
}
