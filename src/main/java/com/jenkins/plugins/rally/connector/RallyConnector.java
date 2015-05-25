package com.jenkins.plugins.rally.connector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.Response;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RallyConnector implements AlmConnector {
    private static final String DEFAULT_REPO_NAME_CREATED_BY_PLUGIN = "plugin_repo";

    private RallyConfiguration rallyConfiguration;
    private RallyRestApi rallyApiInstance;
    private ScmConnector scmConnector;
    private AdvancedConfiguration advancedConfiguration;

    public RallyConnector() {

    }

    private void initializeProxy(URI proxy) throws URISyntaxException {
        String userInfo = proxy.getUserInfo();

        if (userInfo != null && !userInfo.isEmpty()) {
            if (userInfo.contains(":")) {
                String[] tokens = userInfo.split(":");
                if (tokens.length != 2) {
                    throw new URISyntaxException(proxy.toString(), "The URI must have a userName and a apiKey (or neither)");
                }
                String username = tokens[0];
                String passwd = tokens[1];
                this.rallyApiInstance.setProxy(proxy, username, passwd);
            } else {
                throw new URISyntaxException(proxy.toString(), "Unable to set userName on proxy URI without apiKey");
            }
        } else {
            this.rallyApiInstance.setProxy(proxy);
        }
    }

    public void closeConnection() throws RallyException {
        try {
            this.rallyApiInstance.close();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public void updateChangeset(RallyDetailsDTO details) throws RallyException {
        details.getOut().println("Updating Rally -- " + details.getMsg());
        JsonObject newChangeset = createChangeSet(details);
        CreateRequest createRequest = new CreateRequest("Changeset", newChangeset);
        CreateResponse createResponse;

        try {
            createResponse = this.rallyApiInstance.create(createRequest);
        } catch (IOException exception) {
            throw new RallyException(exception);
        }

        printWarningsOrErrors(createResponse, details, "updateChangeset.CreateChangeSet");
        String csRef = createResponse.getObject().get("_ref").getAsString();
        for(int i=0; i<details.getFileNameAndTypes().length;i++) {
            String fileName = details.getFileNameAndTypes()[i][0];
            String fileType = details.getFileNameAndTypes()[i][1];
            String revision = details.getRevision();
            JsonObject newChange = createChange(csRef, fileName, fileType, revision);
            CreateRequest cRequest = new CreateRequest("change", newChange);
            try {
                CreateResponse cResponse = this.rallyApiInstance.create(cRequest);
                printWarningsOrErrors(cResponse, details, "updateChangeset. CreateChange");
            } catch (IOException exception) {
                throw new RallyException(exception);
            }
        }

        if (!createResponse.wasSuccessful()) {
            throw new RallyException();
        }
    }

    private JsonObject createChangeSet(RallyDetailsDTO details) throws RallyException {
        JsonObject newChangeset = new JsonObject();
        JsonObject scmJsonObject = createSCMRef(details);
        newChangeset.add("SCMRepository", scmJsonObject);
        newChangeset.addProperty("Revision", details.getRevision());
        newChangeset.addProperty("Uri", this.scmConnector.getRevisionUriFor(details.getRevision()));
        newChangeset.addProperty("CommitTimestamp", details.getTimeStamp());
        newChangeset.addProperty("Message", details.getMsg());

        JsonArray artifactsJsonArray = new JsonArray();
        JsonObject ref;
        if(details.isStory())
            ref = getStoryObject(details);
        else
            ref = createDefectRef(details);
        artifactsJsonArray.add(ref);
        newChangeset.add("Artifacts", artifactsJsonArray);
        return newChangeset;
    }

    private String getStoryRef(RallyDetailsDTO details) throws RallyException {
        JsonObject storyObject = getStoryObject(details);
        return storyObject.get("_ref").toString();
    }

    private JsonObject getStoryObject(RallyDetailsDTO details) throws RallyException {
        QueryRequest storyRequest = new QueryRequest("HierarchicalRequirement");
        storyRequest.setFetch(new Fetch("FormattedID", "Name", "Changesets"));
        storyRequest.setQueryFilter(new QueryFilter("FormattedID", "=", details.getId()));
        storyRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        try {
            QueryResponse storyQueryResponse = this.rallyApiInstance.query(storyRequest);
            printWarningsOrErrors(storyQueryResponse, details, "getStoryObject");

            if (storyQueryResponse.getTotalResultCount() == 0) {
                throw new RallyException("Unable to find story with identifier: " + details.getId());
            }
            return storyQueryResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject createDefectRef(RallyDetailsDTO details) throws RallyException {
        QueryRequest defectRequest = new QueryRequest("defect");
        defectRequest.setFetch(new Fetch("FormattedId", "Name", "Changesets"));
        defectRequest.setQueryFilter(new QueryFilter("FormattedID", "=", details.getId()));
        defectRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        defectRequest.setScopedDown(true);
        try {
            QueryResponse defectResponse = this.rallyApiInstance.query(defectRequest);
            printWarningsOrErrors(defectResponse, details, "createDefectRef");
            return defectResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject createChange(String csRef, String fileName, String fileType, String revision) {
        JsonObject newChange = new JsonObject();
        newChange.addProperty("PathAndFilename", fileName);
        newChange.addProperty("Action", fileType);
        newChange.addProperty("Uri", this.scmConnector.getFileUriFor(revision, fileName));
        newChange.addProperty("Changeset", csRef);
        return newChange;
    }

    public void updateRallyTaskDetails(RallyDetailsDTO details) throws RallyException {
        if (!details.isStory() || (details.getTaskIndex().isEmpty() && details.getTaskID().isEmpty())) {
            return;
        }

        String storyRef = getStoryRef(details);
        JsonObject taskObject = getTaskObjectByStoryRef(details, storyRef);

        JsonObject updateTask = new JsonObject();
        if(!details.getTaskStatus().isEmpty())
            updateTask.addProperty("State", details.getTaskStatus());
        else {
            updateTask.addProperty("State", "In-Progress");
        }
        if(!details.getTaskToDO().isEmpty()) {
            Double todo = Double.parseDouble(details.getTaskToDO());
            updateTask.addProperty("ToDo", String.valueOf(todo));
        }
        if(!details.getTaskActuals().isEmpty()) {
            Double actuals = Double.parseDouble(details.getTaskActuals());
            try {
                actuals = actuals + taskObject.get("Actuals").getAsDouble();
            } catch(Exception ignored) {}
            updateTask.addProperty("Actuals", String.valueOf(actuals));
        }
        if(!details.getTaskEstimates().isEmpty()) {
            Double estimates = Double.parseDouble(details.getTaskEstimates());
            updateTask.addProperty("Estimate", String.valueOf(estimates));
        }

        try {
            UpdateRequest updateRequest = new UpdateRequest(taskObject.get("_ref").getAsString(), updateTask);
            UpdateResponse updateResponse = this.rallyApiInstance.update(updateRequest);
            printWarningsOrErrors(updateResponse, details, "updateRallyTaskDetails");
            if (!updateResponse.wasSuccessful()) {
                throw new RallyException(updateResponse.getErrors());
            }
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject getTaskObjectByStoryRef(RallyDetailsDTO details, String storyRef) throws RallyException {
        JsonObject taskObject;
        if(!details.getTaskIndex().isEmpty()) {
            int taskIndex = Integer.parseInt(details.getTaskIndex()) - 1; // index starts with 0 in rally
            taskObject = getTaskObject(storyRef, "TaskIndex", String.valueOf(taskIndex), details);
        } else {
            taskObject = getTaskObject(storyRef, "FormattedID", details.getTaskID(), details);
        }
        return taskObject;
    }

    private JsonObject getTaskObject(String storyRef, String taskQueryAttr, String taskQueryValue, RallyDetailsDTO rdto) throws RallyException {
        QueryRequest taskRequest = new QueryRequest("Task");
        taskRequest.setFetch(new Fetch("FormattedID", "Actuals", "State"));
        QueryFilter qf = new QueryFilter("WorkProduct", "=", storyRef);
        qf = qf.and(new QueryFilter(taskQueryAttr, "=", taskQueryValue));
        taskRequest.setQueryFilter(qf);
        try {
            QueryResponse taskQueryResponse = this.rallyApiInstance.query(taskRequest);
            printWarningsOrErrors(taskQueryResponse, rdto, "getTaskObject");
            return taskQueryResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject createSCMRef(RallyDetailsDTO rdto) throws RallyException {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID","Name","SCMType"));
        scmRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        scmRequest.setQueryFilter(new QueryFilter("Name", "=", getSCMRepoName(rdto, this.rallyConfiguration.getScmName())));
        try {
            QueryResponse scmQueryResponse = this.rallyApiInstance.query(scmRequest);
            printWarningsOrErrors(scmQueryResponse, rdto, "createSCMRef");
            return scmQueryResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException io) {
            throw new RallyException(io);
        }
    }

    private String getSCMRepoName(RallyDetailsDTO rdto, String scmRepoName) throws RallyException {
        if(StringUtils.isNotBlank(scmRepoName)  && isProvidedScmRepoNameExist(rdto, scmRepoName))
            return scmRepoName;

        String anyOtherRepoName = getAnyOtherRepoName(rdto);
        if(!StringUtils.isBlank(anyOtherRepoName))
            return anyOtherRepoName;

        if(isDefaultPluginRepoNameExist(rdto))
            return DEFAULT_REPO_NAME_CREATED_BY_PLUGIN;

        return createDefaultPluginScmRepositoryName(rdto);
    }

    private Boolean isProvidedScmRepoNameExist(RallyDetailsDTO rdto, String scmRepoName) {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID","Name","Name"));
        scmRequest.setQueryFilter(new QueryFilter("Name", "=", scmRepoName));
        scmRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        String providedRepoName = "";
        try {
            QueryResponse scmQueryResponse = this.rallyApiInstance.query(scmRequest);
            printWarningsOrErrors(scmQueryResponse, rdto, "isProvidedScmRepoNameExist");
            JsonObject scmJsonObject = scmQueryResponse.getResults().get(0).getAsJsonObject();
            providedRepoName = scmJsonObject.get("_refObjectName").getAsString();
        } catch (IOException ignored) {
            System.out.println(ignored.getMessage());
            ignored.printStackTrace(System.out);
        }
        return StringUtils.isNotBlank(providedRepoName);
    }

    private String getAnyOtherRepoName(RallyDetailsDTO rdto) {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID", "Name", "Name"));
        scmRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        String anyOtherRepoName = "";
        try {
            QueryResponse scmQueryResponse = this.rallyApiInstance.query(scmRequest);
            printWarningsOrErrors(scmQueryResponse, rdto, "getAnyOtherRepoName");
            JsonObject scmJsonObject = scmQueryResponse.getResults().get(0).getAsJsonObject();
            anyOtherRepoName = scmJsonObject.get("_refObjectName").getAsString();
        } catch (Exception ignored) {
        }
        return anyOtherRepoName;
    }

    private boolean isDefaultPluginRepoNameExist(RallyDetailsDTO rdto) {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID", "Name", "Name"));
        scmRequest.setQueryFilter(new QueryFilter("Name", "=", DEFAULT_REPO_NAME_CREATED_BY_PLUGIN));
        scmRequest.setWorkspace(this.rallyConfiguration.getWorkspaceName());
        String defaultPluginRepoName = "";
        try {
            QueryResponse scmQueryResponse = this.rallyApiInstance.query(scmRequest);
            printWarningsOrErrors(scmQueryResponse, rdto, "isDefaultPluginRepoNameExist");
            JsonObject scmJsonObject = scmQueryResponse.getResults().get(0).getAsJsonObject();
            defaultPluginRepoName = scmJsonObject.get("_refObjectName").getAsString();
        } catch (Exception ignored) {
        }
        return StringUtils.isNotBlank(defaultPluginRepoName);
    }

    private String createDefaultPluginScmRepositoryName(RallyDetailsDTO rdto) throws RallyException {
        JsonObject newSCMRepository = new JsonObject();
        newSCMRepository.addProperty("Description", "This repository name is created by rally update plugin");

        newSCMRepository.addProperty("Name", DEFAULT_REPO_NAME_CREATED_BY_PLUGIN);
        newSCMRepository.addProperty("SCMType", "GIT");
        if(!StringUtils.isBlank(this.rallyConfiguration.getScmName()))
            newSCMRepository.addProperty("Uri", this.scmConnector.getRevisionUriFor(rdto.getRevision()));
        CreateRequest createRequest = new CreateRequest("SCMRepository", newSCMRepository);
        System.out.println(createRequest.getBody());
        try {
            CreateResponse createResponse = this.rallyApiInstance.create(createRequest);
            printWarningsOrErrors(createResponse, rdto, "createDefaultPluginScmRepositoryName");
            return DEFAULT_REPO_NAME_CREATED_BY_PLUGIN;
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private void printWarningsOrErrors(Response response, RallyDetailsDTO rdto, String methodName) {
        if (response.wasSuccessful() && rdto.isDebugOn()) {
            rdto.getOut().println("\tSuccess from method: " + methodName);
            rdto.printAllFields();
            String[] warningList;
            warningList = response.getWarnings();
            for (String aWarningList : warningList) {
                rdto.getOut().println("\twarning " + aWarningList);
            }
        } else {
            String[] errorList;
            errorList = response.getErrors();
            if(errorList.length > 0) {
                rdto.getOut().println("\tError From method: " + methodName);
                rdto.printAllFields();
            }
            for (String anErrorList : errorList) {
                rdto.getOut().println("\terror " + anErrorList);
            }
        }
    }

    public void setRallyConfiguration(RallyConfiguration rallyConfiguration) throws RallyException {
        this.rallyConfiguration = rallyConfiguration;
        initializeProxyPerhaps();
    }

    private void initializeProxyPerhaps() throws RallyException {
        if (isProxyInitializeable()) {
            try {
                initializeProxy(this.advancedConfiguration.getProxyUri());
            } catch (URISyntaxException exception) {
                throw new RallyException(exception);
            }
        }
    }

    public void setScmConnector(ScmConnector connector) {
        this.scmConnector = connector;
    }

    public void setAdvancedConfiguration(AdvancedConfiguration config) throws RallyException {
        this.advancedConfiguration = config;
        initializeProxyPerhaps();
    }

    public void setRallyApiInstance(RallyRestApi rallyApiInstance) {
        this.rallyApiInstance = rallyApiInstance;
    }

    public boolean isProxyInitializeable() {
        return this.advancedConfiguration != null
                && this.advancedConfiguration.getProxyUri() != null
                && !this.advancedConfiguration.getProxyUri().toString().isEmpty()
                && this.rallyApiInstance != null;
    }
}
