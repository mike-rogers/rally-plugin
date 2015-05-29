package com.jenkins.plugins.rally.connector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RallyConnector {
    public static class FactoryHelper {
        RallyRestApi createConnection(String uriAsString, String apiKey) throws URISyntaxException {
            return new RallyRestApi(new URI(uriAsString), apiKey);
        }
    }

    private final RallyRestApi rallyRestApi;
    private final RallyConfiguration rallyConfiguration;

    @Inject
    public RallyConnector(FactoryHelper factoryHelper,
                          RallyConfiguration rallyConfiguration,
                          @Named("SERVER URL") String rallyUrl,
                          @Named("API VERSION") String apiVersion,
                          @Named("APP NAME") String appName) throws RallyException {
        try {
            this.rallyRestApi = factoryHelper.createConnection(rallyUrl, rallyConfiguration.getApiKey());
        } catch (URISyntaxException exception) {
            throw new RallyException(exception);
        }

        this.rallyRestApi.setApplicationName(appName);
        this.rallyRestApi.setApplicationVersion(apiVersion);

        this.rallyConfiguration = rallyConfiguration;
    }

    public void configureProxy(URI uri) throws RallyException {
        if (uri == null || uri.getHost() == null) {
            return;
        }

        String userInfo = uri.getUserInfo();

        if (userInfo == null) {
            this.rallyRestApi.setProxy(uri);
        } else {
            String[] usernamePassword = userInfo.split(":");
            if (usernamePassword.length != 2) {
                throw new RallyException("Unable to parse username/password from proxy URL.");
            }
            this.rallyRestApi.setProxy(uri, usernamePassword[0], usernamePassword[1]);
        }
    }

    public void close() {
        try {
            this.rallyRestApi.close();
        } catch (IOException ignored) {
            // ignored
        }
    }

    public String queryForStory(String formattedId) throws RallyException {
        return this.queryForWorkItem("HierarchicalRequirement", formattedId);
    }

    public String queryForDefect(String formattedId) throws RallyException {
        return this.queryForWorkItem("Defect", formattedId);
    }

    private String queryForWorkItem(String workItemType, String formattedId) throws RallyException {
        QueryRequest storyRequest = new QueryRequest(workItemType);
        storyRequest.setFetch(new Fetch("FormattedID", "Name", "Changesets"));
        storyRequest.setQueryFilter(new QueryFilter("FormattedID", "=", formattedId));
        storyRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        try {
            QueryResponse storyQueryResponse = this.rallyRestApi.query(storyRequest);

            if (storyQueryResponse.getTotalResultCount() == 0) {
                throw new RallyAssetNotFoundException();
            }

            return storyQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public String queryForRepository() throws RallyException {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID","Name","SCMType"));
        scmRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        scmRequest.setQueryFilter(new QueryFilter("Name", "=", this.rallyConfiguration.getScmName()));
        try {
            QueryResponse scmQueryResponse = this.rallyRestApi.query(scmRequest);

            if (scmQueryResponse.getTotalResultCount() == 0) {
                throw new RallyAssetNotFoundException();
            }

            return scmQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public String createChangeset(String scmRepositoryRef, String revision, String uri, String commitTimestamp, String message, String artifactRef) throws RallyException{
        JsonObject newChangeset = new JsonObject();
        JsonObject scmJsonObject = new JsonObject();
        scmJsonObject.addProperty("_ref", scmRepositoryRef);
        newChangeset.add("SCMRepository", scmJsonObject);
        newChangeset.addProperty("Revision", revision);
        newChangeset.addProperty("Uri", uri);
        newChangeset.addProperty("CommitTimestamp", commitTimestamp);
        newChangeset.addProperty("Message", message);

        JsonArray artifactsJsonArray = new JsonArray();
        JsonObject artifactJsonObject = new JsonObject();
        artifactJsonObject.addProperty("_ref", artifactRef);
        artifactsJsonArray.add(artifactJsonObject);
        newChangeset.add("Artifacts", artifactsJsonArray);

        CreateResponse response;

        try {
            response = this.rallyRestApi.create(new CreateRequest("Changeset", newChangeset));
        } catch (IOException exception) {
            throw new RallyException(exception);
        }

        if (!response.wasSuccessful()) {
            throw new RallyException("Unable to create Changeset object!");
        }

        return response.getObject().get("_ref").getAsString();
    }

    public String createChange(String changesetRef, String filename, String action, String uri) throws RallyException {
        JsonObject newChange = new JsonObject();
        JsonObject changesetObject = new JsonObject();
        changesetObject.addProperty("_ref", changesetRef);
        newChange.add("Changeset", changesetObject);
        newChange.addProperty("PathAndFilename", filename);
        newChange.addProperty("Action", action);
        newChange.addProperty("Uri", uri);

        CreateResponse response;

        try {
            response = this.rallyRestApi.create(new CreateRequest("Change", newChange));
        } catch (IOException exception) {
            throw new RallyException(exception);
        }

        if (!response.wasSuccessful()) {
            throw new RallyException("Unable to create Changeset object!");
        }

        return response.getObject().get("_ref").getAsString();
    }
}