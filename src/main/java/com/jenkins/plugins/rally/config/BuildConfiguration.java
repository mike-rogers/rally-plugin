package com.jenkins.plugins.rally.config;

public class BuildConfiguration {
    private final ScmChangeCaptureRange captureRange;

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
