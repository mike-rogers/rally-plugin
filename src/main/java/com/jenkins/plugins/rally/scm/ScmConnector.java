package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.utils.TemplatedUriResolver;
import hudson.model.AbstractBuild;

import java.net.URI;
import java.net.URISyntaxException;

public interface ScmConnector {
    String getRevisionUriFor(String revision);
    void setScmConfiguration(ScmConfiguration configuration);

    Changes getChangesSinceLastBuild(AbstractBuild build);
}
