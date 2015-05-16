package com.jenkins.plugins.rally.utils;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class TemplatedUriResolver {
    public URI resolveCommitUri(String uriTemplate, String revision) throws URISyntaxException {
        Map<String, String> values = new HashMap<String, String>();
        values.put("revision", revision);

        StrSubstitutor substitutor = new StrSubstitutor(values, "${", "}");
        return new URI(substitutor.replace(uriTemplate));
    }

    public URI resolveFileCommitUri(String uriTemplate, String revision, String filename) throws URISyntaxException {
        Map<String, String> values = new HashMap<String, String>();
        values.put("revision", revision);
        values.put("file", filename);

        StrSubstitutor substitutor = new StrSubstitutor(values, "${", "}");
        return new URI(substitutor.replace(uriTemplate));
    }
}
