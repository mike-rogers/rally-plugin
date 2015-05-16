package com.jenkins.plugins.rally.config;

public enum ScmChangeCaptureRange {
    SinceLastBuild("changesSinceLastBuild"),
    SinceLastSuccessfulBuild("changesSinceLastSuccessfulBuild");

    private final String value;

    ScmChangeCaptureRange(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
