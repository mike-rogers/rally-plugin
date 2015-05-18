package com.jenkins.plugins.rally.config;

public class ScmConfiguration {
    private final String commitTemplate;

    public ScmConfiguration(String commitTemplate) {
        this.commitTemplate = commitTemplate;
    }

    public String getCommitTemplate() {
        return commitTemplate;
    }
}
