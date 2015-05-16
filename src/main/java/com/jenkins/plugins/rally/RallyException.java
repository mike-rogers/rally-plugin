package com.jenkins.plugins.rally;

public class RallyException extends Exception {
    public RallyException() {
        // Empty
    }

    public RallyException(Exception exception) {
        super(exception);
    }
}
