package com.jenkins.plugins.rally.config;

public class ScmConfiguration {
    private final String commitTemplate;
    private final String fileTemplate;

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
}
