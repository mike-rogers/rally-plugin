package com.jenkins.plugins.rally.config;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;

public class RallyPluginConfiguration implements Describable<RallyPluginConfiguration> {
    private final RallyConfiguration rallyConfig;
    private final ScmConfiguration scmConfig;
    private final BuildConfiguration buildConfig;
    private final AdvancedConfiguration advancedConfig;

    public RallyPluginConfiguration(RallyConfiguration rally, ScmConfiguration scm, BuildConfiguration build, AdvancedConfiguration advanced) {
        this.rallyConfig = rally;
        this.scmConfig = scm;
        this.buildConfig = build;
        this.advancedConfig = advanced;
    }

    public RallyConfiguration getRally() {
        return rallyConfig;
    }

    public ScmConfiguration getScm() {
        return scmConfig;
    }

    public BuildConfiguration getBuild() {
        return buildConfig;
    }

    public AdvancedConfiguration getAdvanced() {
        return advancedConfig;
    }

    public Descriptor<RallyPluginConfiguration> getDescriptor() {
        return new RallyPluginConfigurationDescriptor();
    }

    @Extension
    public static final class RallyPluginConfigurationDescriptor extends Descriptor<RallyPluginConfiguration> {
        public String getDisplayName() {
            return "Plugin Configuration";
        }
    }
}
