package com.jenkins.plugins.rally.config;

import java.net.URI;
import java.net.URISyntaxException;

public class AdvancedConfiguration {
    private final URI proxyUri;
    private final String isDebugOn;

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
}
