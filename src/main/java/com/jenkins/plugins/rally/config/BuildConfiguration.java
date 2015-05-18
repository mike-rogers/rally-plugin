package com.jenkins.plugins.rally.config;

public class BuildConfiguration {
    private final ScmChangeCaptureRange captureRange;

    public BuildConfiguration(String captureRange) {
        this.captureRange = ScmChangeCaptureRange.valueOf(captureRange);
    }

    public String getCaptureRange() {
        return captureRange.getValue();
    }

    public ScmChangeCaptureRange getCaptureRangeAsEnum() {
        return this.captureRange;
    }
}
