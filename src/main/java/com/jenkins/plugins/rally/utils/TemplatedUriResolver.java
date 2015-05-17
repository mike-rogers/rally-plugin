package com.jenkins.plugins.rally.utils;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class TemplatedUriResolver {
    public String resolveCommitUri(String uriTemplate, String revision) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("revision", revision);

        StrSubstitutor substitutor = new StrSubstitutor(values, "${", "}");
        return substitutor.replace(uriTemplate);
    }

    public String resolveFileCommitUri(String uriTemplate, String revision, String filename) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("revision", revision);
        values.put("file", filename);

        StrSubstitutor substitutor = new StrSubstitutor(values, "${", "}");
        return substitutor.replace(uriTemplate);
    }
}
