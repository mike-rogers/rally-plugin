package com.jenkins.plugins.rally.config;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AdvancedConfiguration implements Describable<AdvancedConfiguration> {
    private final URI proxyUri;
    private final String isDebugOn;

    @DataBoundConstructor
    public AdvancedConfiguration(String proxyUri, String isDebugOn) throws URISyntaxException {
        this.proxyUri = new URI(proxyUri);
        this.isDebugOn = isDebugOn;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public String getIsDebugOn() {
        return isDebugOn;
    }

    public Descriptor<AdvancedConfiguration> getDescriptor() {
        return new AdvancedConfigurationDescriptor();
    }

    @Extension
    public static final class AdvancedConfigurationDescriptor extends Descriptor<AdvancedConfiguration> {
        public String getDisplayName() {
            return "Advanced Information";
        }
    }
}
