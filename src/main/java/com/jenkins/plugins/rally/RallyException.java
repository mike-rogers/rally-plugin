package com.jenkins.plugins.rally;

import com.google.common.base.Joiner;

public class RallyException extends Exception {
    public RallyException() {
        // Empty
    }

    public RallyException(Exception exception) {
        super(exception);
    }

    public RallyException(String[] errors) {
        super(Joiner.on(';').join(errors));
    }

    public RallyException(String message) {
        super(message);
    }
}
