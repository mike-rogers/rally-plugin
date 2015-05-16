package com.jenkins.plugins.rally.connector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.jenkins.plugins.rally.RallyException;
import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.apache.commons.lang3.text.StrSubstitutor;

public class RallyConnector implements AlmConnector {
    private final String workspace;
    private final String scmUri;
    private final String scmRepoName;

    private final RallyRestApi restApi;

    public static final String RALLY_URL = "https://rally1.rallydev.com";
    public static final String APPLICATION_NAME = "RallyConnect";
    public static final String WSAPI_VERSION = "v2.0";
    private String DEFAULT_REPO_NAME_CREATED_BY_PLUGIN = "plugin_repo";

    public RallyConnector(final String apiKey, final String workspace, final String scmUri, final String scmRepoName, final String proxy) throws URISyntaxException {
        this.workspace = workspace;
        this.scmUri = scmUri;
        this.scmRepoName = scmRepoName;

        restApi = new RallyRestApi(new URI(RALLY_URL), apiKey);
        restApi.setWsapiVersion(WSAPI_VERSION);
        restApi.setApplicationName(APPLICATION_NAME);
        if(proxy != null && proxy.trim().length() > 0){
            initializeProxy(proxy);
        }
    }

    private void initializeProxy(String proxy) throws URISyntaxException {
        URI proxyUri = new URI(proxy);
        String userInfo = proxyUri.getUserInfo();

        if (userInfo != null && !userInfo.isEmpty()) {
            if (userInfo.contains(":")) {
                String[] tokens = userInfo.split(":");
                if (tokens.length != 2) {
                    throw new URISyntaxException(proxy, "The URI must have a userName and a apiKey (or neither)");
                }
                String username = tokens[0];
                String passwd = tokens[1];
                restApi.setProxy(proxyUri, username, passwd);
            } else {
                throw new URISyntaxException(proxy, "Unable to set userName on proxy URI without apiKey");
            }
        } else {
            restApi.setProxy(proxyUri);
        }
    }

    public void closeConnection() throws IOException {
        restApi.close();
    }

    public void updateChangeset(RallyDetailsDTO details) throws RallyException {
        details.getOut().println("Updating Rally -- " + details.getMsg());
        JsonObject newChangeset = createChangeSet(details);
        CreateRequest createRequest = new CreateRequest("Changeset", newChangeset);
        CreateResponse createResponse;

        try {
            createResponse = restApi.create(createRequest);
        } catch (IOException exception) {
            throw new RallyException(exception);
        }

        printWarningsOrErrors(createResponse, details, "updateChangeset.CreateChangeSet");
        String csRef = createResponse.getObject().get("_ref").getAsString();
        for(int i=0; i<details.getFileNameAndTypes().length;i++) {
            String fileName = details.getFileNameAndTypes()[i][0];
            String fileType = details.getFileNameAndTypes()[i][1];
            String revision = details.getRevison();
            JsonObject newChange = createChange(csRef, fileName, fileType, revision);
            CreateRequest cRequest = new CreateRequest("change", newChange);
            try {
                CreateResponse cResponse = restApi.create(cRequest);
                printWarningsOrErrors(cResponse, details, "updateChangeset. CreateChange");
            } catch (IOException exception) {
                throw new RallyException(exception);
            }
        }

        if (!createResponse.wasSuccessful()) {
            throw new RallyException();
        }
    }

    private String resolveScmUri(String revision) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("revision", revision);

        StrSubstitutor substitutor = new StrSubstitutor(values, "${", "}");
        return substitutor.replace(this.scmUri);
    }

    private JsonObject createChangeSet(RallyDetailsDTO details) throws RallyException {
        JsonObject newChangeset = new JsonObject();
        JsonObject scmJsonObject = createSCMRef(details);
        newChangeset.add("SCMRepository", scmJsonObject);
        newChangeset.addProperty("Author", createUserRef(details));
        newChangeset.addProperty("Revision", details.getRevison());
        newChangeset.addProperty("Uri", resolveScmUri(details.getRevison()));
        newChangeset.addProperty("CommitTimestamp", details.getTimeStamp());
        newChangeset.addProperty("Message", details.getMsg());
        //newChangeset.addProperty("Builds", createBuilds());

        JsonArray artifactsJsonArray = new JsonArray();
        JsonObject ref;
        if(details.isStory())
            ref = createStoryRef(details);
        else
            ref = createDefectRef(details);
        artifactsJsonArray.add(ref);
        newChangeset.add("Artifacts", artifactsJsonArray);
        return newChangeset;
    }

    private JsonObject createStoryRef(RallyDetailsDTO rdto) throws RallyException {
        QueryRequest  storyRequest = new QueryRequest("HierarchicalRequirement");
        storyRequest.setFetch(new Fetch("FormattedID", "Name", "Changesets"));
        storyRequest.setQueryFilter(new QueryFilter("FormattedID", "=", rdto.getId()));
        storyRequest.setWorkspace(workspace);
        try {
            QueryResponse storyQueryResponse = restApi.query(storyRequest);
            printWarningsOrErrors(storyQueryResponse, rdto, "createStoryRef");
            return storyQueryResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject createDefectRef(RallyDetailsDTO rdto) throws RallyException {
        QueryRequest defectRequest = new QueryRequest("defect");
        defectRequest.setFetch(new Fetch("FormattedId", "Name", "Changesets"));
        defectRequest.setQueryFilter(new QueryFilter("FormattedID", "=", rdto.getId()));
        defectRequest.setWorkspace(workspace);
        defectRequest.setScopedDown(true);
        try {
            QueryResponse defectResponse = restApi.query(defectRequest);
            printWarningsOrErrors(defectResponse, rdto, "createDefectRef");
            return defectResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject createChange(String csRef, String fileName, String fileType, String revision) {
        JsonObject newChange = new JsonObject();
        newChange.addProperty("PathAndFilename", fileName);
        newChange.addProperty("Action", fileType);
        newChange.addProperty("Uri", resolveScmUri(revision));
        newChange.addProperty("Changeset", csRef);
        return newChange;
    }

    public boolean updateRallyTaskDetails(RallyDetailsDTO details) throws RallyException {
        boolean result = false;
        if(details.isStory() && (!details.getTaskIndex().isEmpty() || !details.getTaskID().isEmpty())) {
            JsonObject storyRef = createStoryRef(details);
            JsonObject taskRef;
            if(!details.getTaskIndex().isEmpty()) {
                int ti = Integer.parseInt(details.getTaskIndex());
                ti = ti - 1; //index starts with 0 in rally
                taskRef = createTaskRef(storyRef.get("_ref").toString(), "TaskIndex", String.valueOf(ti), details);
            } else {
                taskRef = createTaskRef(storyRef.get("_ref").toString(), "FormattedID", details.getTaskID(), details);
            }

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
                    actuals = actuals + taskRef.get("Actuals").getAsDouble();
                } catch(Exception ignored) {}
                updateTask.addProperty("Actuals", String.valueOf(actuals));
            }
            if(!details.getTaskEstimates().isEmpty()) {
                Double estimates = Double.parseDouble(details.getTaskEstimates());
                updateTask.addProperty("Estimate", String.valueOf(estimates));
            }

            try {
                UpdateRequest updateRequest = new UpdateRequest(taskRef.get("_ref").getAsString(), updateTask);
                UpdateResponse updateResponse = restApi.update(updateRequest);
                printWarningsOrErrors(updateResponse, details, "updateRallyTaskDetails");
                result = updateResponse.wasSuccessful();
            } catch (IOException exception) {
                throw new RallyException(exception);
            }
        }
        return result;
    }

    private JsonObject createTaskRef(String storyRef, String taskQueryAttr, String taskQueryValue, RallyDetailsDTO rdto) throws RallyException {
        QueryRequest taskRequest = new QueryRequest("Task");
        taskRequest.setFetch(new Fetch("FormattedID", "Actuals", "State"));
        QueryFilter qf = new QueryFilter("WorkProduct", "=", storyRef);
        qf = qf.and(new QueryFilter(taskQueryAttr, "=", taskQueryValue));
        taskRequest.setQueryFilter(qf);
        try {
            QueryResponse taskQueryResponse = restApi.query(taskRequest);
            printWarningsOrErrors(taskQueryResponse, rdto, "createTaskRef");
            return taskQueryResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private JsonObject createSCMRef(RallyDetailsDTO rdto) throws RallyException {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID","Name","SCMType"));
        scmRequest.setWorkspace(workspace);
        scmRequest.setQueryFilter(new QueryFilter("Name", "=", getSCMRepoName(rdto, scmRepoName)));
        try {
            QueryResponse scmQueryResponse = restApi.query(scmRequest);
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
        scmRequest.setWorkspace(workspace);
        String providedRepoName = "";
        try {
            QueryResponse scmQueryResponse = restApi.query(scmRequest);
            printWarningsOrErrors(scmQueryResponse, rdto, "isProvidedScmRepoNameExist");
            JsonObject scmJsonObject = scmQueryResponse.getResults().get(0).getAsJsonObject();
            providedRepoName = scmJsonObject.get("_refObjectName").getAsString();
        } catch (Exception ignored) {
        }
        return StringUtils.isNotBlank(providedRepoName);
    }

    private String getAnyOtherRepoName(RallyDetailsDTO rdto) {
        QueryRequest scmRequest = new QueryRequest("SCMRepository");
        scmRequest.setFetch(new Fetch("ObjectID", "Name", "Name"));
        scmRequest.setWorkspace(workspace);
        String anyOtherRepoName = "";
        try {
            QueryResponse scmQueryResponse = restApi.query(scmRequest);
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
        scmRequest.setWorkspace(workspace);
        String defaultPluginRepoName = "";
        try {
            QueryResponse scmQueryResponse = restApi.query(scmRequest);
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
        if(!StringUtils.isBlank(scmUri))
            newSCMRepository.addProperty("Uri", resolveScmUri(rdto.getRevison()));
        CreateRequest createRequest = new CreateRequest("SCMRepository", newSCMRepository);
        System.out.println(createRequest.getBody());
        try {
            CreateResponse createResponse = restApi.create(createRequest);
            printWarningsOrErrors(createResponse, rdto, "createDefaultPluginScmRepositoryName");
            return DEFAULT_REPO_NAME_CREATED_BY_PLUGIN;
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    private String createUserRef(RallyDetailsDTO rdto) throws RallyException {
        QueryRequest userRequest = new QueryRequest("User");
        userRequest.setFetch(new Fetch("UserName", "Subscription", "DisplayName"));
        userRequest.setQueryFilter(new QueryFilter("UserName", "=", "username"));
        try {
            QueryResponse userQueryResponse = restApi.query(userRequest);
            printWarningsOrErrors(userQueryResponse, rdto, "createUserRef");
            JsonArray userQueryResults = userQueryResponse.getResults();
            JsonElement userQueryElement = userQueryResults.get(0);
            return userQueryElement.getAsJsonObject().get("_ref").toString();
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
}
