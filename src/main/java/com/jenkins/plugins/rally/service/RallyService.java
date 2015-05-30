package com.jenkins.plugins.rally.service;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.connector.AlmConnector;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.io.IOException;

public class RallyService implements AlmConnector {

    private RallyRestApi rallyApiInstance;
    private ScmConnector scmConnector;
    private RallyConnector rallyConnector;

    @Inject
    public RallyService(RallyConnector connector, ScmConnector scmConnector, AdvancedConfiguration configuration) throws RallyException {
        this.scmConnector = scmConnector;
        this.rallyConnector = connector;
        this.rallyConnector.configureProxy(configuration.getProxyUri());
    }

    public void closeConnection() throws RallyException {
        this.rallyConnector.close();
    }

    public void updateChangeset(RallyDetailsDTO details) throws RallyException {
        String repositoryRef = this.rallyConnector.queryForRepository();
        String artifactRef = details.isStory()
                ? this.rallyConnector.queryForStory(details.getId())
                : this.rallyConnector.queryForDefect(details.getId());
        String revisionUri = this.scmConnector.getRevisionUriFor(details.getRevision());
        String changesetRef = this.rallyConnector.createChangeset(repositoryRef, details.getRevision(), revisionUri, details.getTimeStamp(), details.getMsg(), artifactRef);

        for (RallyDetailsDTO.FilenameAndAction filenameAndAction : details.getFilenamesAndActions()) {
            String fileName = filenameAndAction.filename;
            String fileType = filenameAndAction.action.getName();
            String revision = details.getRevision();
            String fileUri = this.scmConnector.getFileUriFor(revision, fileName);

            this.rallyConnector.createChange(changesetRef, fileName, fileType, fileUri);
        }
    }

    public void updateRallyTaskDetails(RallyDetailsDTO details) throws RallyException {
        if (!details.isStory() || (details.getTaskIndex().isEmpty() && details.getTaskID().isEmpty())) {
            return;
        }

        String storyRef = this.rallyConnector.queryForStory(details.getId());
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
            taskObject = getTaskObject(storyRef, "TaskIndex", String.valueOf(taskIndex));
        } else {
            taskObject = getTaskObject(storyRef, "FormattedID", details.getTaskID());
        }
        return taskObject;
    }

    private JsonObject getTaskObject(String storyRef, String taskQueryAttr, String taskQueryValue) throws RallyException {
        QueryRequest taskRequest = new QueryRequest("Task");
        taskRequest.setFetch(new Fetch("FormattedID", "Actuals", "State"));
        QueryFilter qf = new QueryFilter("WorkProduct", "=", storyRef);
        qf = qf.and(new QueryFilter(taskQueryAttr, "=", taskQueryValue));
        taskRequest.setQueryFilter(qf);
        try {
            QueryResponse taskQueryResponse = this.rallyApiInstance.query(taskRequest);

            if (taskQueryResponse.getTotalResultCount() == 0) {
                throw new RallyAssetNotFoundException();
            }

            return taskQueryResponse.getResults().get(0).getAsJsonObject();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public void setRallyApiInstance(RallyRestApi rallyApiInstance) {
        this.rallyApiInstance = rallyApiInstance;
    }
}
