package com.jenkins.plugins.rally;

import com.jenkins.plugins.rally.config.RallyPluginConfiguration;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.PrintStream;

import org.kohsuke.stapler.DataBoundConstructor;

import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.ChangeInformation;
import com.jenkins.plugins.rally.scm.Changes;

/**
 * @author Tushar Shinde
 * @author R. Michael Rogers
 */
public class RallyPlugin extends Builder {
    private final RallyPluginConfiguration config;

    @DataBoundConstructor
    public RallyPlugin(RallyPluginConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream out = listener.getLogger();
        Changes changes = null; //PostBuildHelper.getChanges(changesSince, startDate, endDate, build, out);

        RallyConnector rallyConnector = null;
        try {
            rallyConnector = null; //new RallyConnector(apiKey, workspace, project, scmuri, scmRepoName, proxy);
            for(ChangeInformation ci : changes.getChangeInformation()) { //build level
                try {
                    for(Object item : ci.getChangeLogSet().getItems()) { //each changes in above build
                        ChangeLogSet.Entry cse = (ChangeLogSet.Entry) item;
                        RallyDetailsDTO rdto = null; //PostBuildHelper.populateRallyDetailsDTO(debugOn, build, ci, cse, out);
                        if(!rdto.getId().isEmpty()) {
                            try {
                                rallyConnector.updateChangeset(rdto);
                            } catch(Exception e) {
                                out.println("\trally update plug-in error: could not update changeset entry: "  + e.getMessage());
                                e.printStackTrace(out);
                            }

                            try {
                                rallyConnector.updateRallyTaskDetails(rdto);
                            } catch(Exception e) {
                                out.println("\trally update plug-in error: could not update TaskDetails entry: "  + e.getMessage());
                                e.printStackTrace(out);
                            }
                        } else {
                            out.println("Could not update rally due to absence of id in a comment " + rdto.getMsg());
                        }
                    }
                } catch(Exception e) {
                    out.println("\trally update plug-in error: could not iterate or populate through getChangeLogSet().getItems(): "  + e.getMessage());
                    e.printStackTrace(out);
                }
            }
        } catch(Exception e) {
            out.println("\trally update plug-in error: error while creating connection to rally: " + e.getMessage());
            e.printStackTrace(out);
        } finally {
            try {
                if(rallyConnector != null) rallyConnector.closeConnection();
            } catch(Exception e) {out.println("\trally update plug-in error: error while closing connection: " + e.getMessage());
                e.printStackTrace(out);
            }
        }

        return true;
    }

    public RallyPluginConfiguration getConfig() {
        return config;
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
