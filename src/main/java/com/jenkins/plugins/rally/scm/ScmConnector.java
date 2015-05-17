package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import hudson.model.AbstractBuild;

import java.io.PrintStream;
import java.util.List;

public interface ScmConnector {
    String getRevisionUriFor(String revision);
    void setScmConfiguration(ScmConfiguration configuration);
    void setBuildConfiguration(BuildConfiguration configuration);
    void setAdvancedConfiguration(AdvancedConfiguration configuration);

    List<RallyDetailsDTO> getChanges(AbstractBuild build, PrintStream out) throws RallyException;
}
