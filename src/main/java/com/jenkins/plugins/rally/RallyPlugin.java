package com.jenkins.plugins.rally;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.jenkins.plugins.rally.config.*;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.service.RallyService;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Tushar Shinde
 * @author R. Michael Rogers
 */
public class RallyPlugin extends Builder {
    private final RallyPluginConfiguration config;
    private RallyService rallyService;
    private ScmConnector jenkinsConnector;

    @DataBoundConstructor
    public RallyPlugin(String rallyApiKey, String rallyWorkspaceName, String rallyScmName, String shouldCreateIfAbsent, String scmCommitTemplate, String scmFileTemplate, String buildCaptureRange, String advancedIsDebugOn, String advancedProxyUri) throws RallyException, URISyntaxException {
        RallyConfiguration rally = new RallyConfiguration(rallyApiKey, rallyWorkspaceName, rallyScmName, shouldCreateIfAbsent);
        ScmConfiguration scm = new ScmConfiguration(scmCommitTemplate, scmFileTemplate);
        BuildConfiguration build = new BuildConfiguration(buildCaptureRange);
        AdvancedConfiguration advanced = new AdvancedConfiguration(advancedProxyUri, advancedIsDebugOn);

        this.config = new RallyPluginConfiguration(rally, scm, build, advanced);
    }

    private void initialize() throws RallyException {
        AbstractModule module = new AbstractModule() {
            @Override
            protected void configure() {
                bind(AdvancedConfiguration.class).toInstance(config.getAdvanced());
                bind(BuildConfiguration.class).toInstance(config.getBuild());
                bind(RallyConfiguration.class).toInstance(config.getRally());
                bind(ScmConfiguration.class).toInstance(config.getScm());
            }
        };

        Injector injector = Guice.createInjector(Modules.override(new RallyGuiceModule()).with(module));
        injector.injectMembers(this);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            initialize();
        } catch (RallyException exception) {
            System.out.println(exception.getMessage());
            exception.printStackTrace(System.out);
            return false;
        }

        boolean shouldBuildSucceed = true;
        PrintStream out = listener.getLogger();

        List<RallyDetailsDTO> detailsList;
        try {
            detailsList = this.jenkinsConnector.getChanges(build, out);
        } catch (RallyException exception) {
            out.println("Unable to retrieve SCM changes from Jenkins: " + exception.getMessage());
            return false;
        }

        for (RallyDetailsDTO details : detailsList) {
            if (!details.getId().isEmpty()) {
                try {
                    this.rallyService.updateChangeset(details);
                } catch (Exception e) {
                    out.println("\trally update plug-in error: could not update changeset entry: " + e.getMessage());
                    e.printStackTrace(out);
                    shouldBuildSucceed = false;
                }

                try {
                    this.rallyService.updateRallyTaskDetails(details);
                } catch (Exception e) {
                    out.println("\trally update plug-in error: could not update TaskDetails entry: " + e.getMessage());
                    e.printStackTrace(out);
                    shouldBuildSucceed = false;
                }
            } else {
                out.println("Could not update rally due to absence of id in a comment " + details.getMsg());
            }
        }

        try {
            this.rallyService.closeConnection();
        } catch (RallyException exception) {
            // Ignore
        }

        return shouldBuildSucceed;
    }

    @Inject
    public void setRallyService(RallyService service) {
        this.rallyService = service;
    }

    @Inject
    public void setScmConnector(ScmConnector connector) {
        this.jenkinsConnector = connector;
    }

    public RallyPluginConfiguration getConfig() {
        return config;
    }

    public String getRallyApiKey() {
        return this.config.getRally().getApiKey();
    }

    public String getRallyWorkspaceName() {
        return this.config.getRally().getWorkspaceName();
    }

    public String getRallyScmName() {
        return this.config.getRally().getScmName();
    }

    public String getShouldCreateIfAbsent() {
        return this.config.getRally().shouldCreateIfAbsent().toString();
    }

    public String getScmCommitTemplate() {
        return this.config.getScm().getCommitTemplate();
    }

    public String getScmFileTemplate() {
        return this.config.getScm().getFileTemplate();
    }

    public String getBuildCaptureRange() {
        return this.config.getBuild().getCaptureRange();
    }

    public String getAdvancedIsDebugOn() {
        return this.config.getAdvanced().getIsDebugOn();
    }

    public String getAdvancedProxyUri() {
        return this.config.getAdvanced().getProxyUri().toString();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This get displayed at 'Add build step' button.
         */
        public String getDisplayName() {
            return "Update Rally Task and ChangeSet";
        }
    }
}
