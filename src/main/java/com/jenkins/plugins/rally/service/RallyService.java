package com.jenkins.plugins.rally.service;

import com.google.inject.Inject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.connector.AlmConnector;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.utils.RallyQueryBuilder;
import com.jenkins.plugins.rally.utils.RallyUpdateBean;

public class RallyService implements AlmConnector {
    private ScmConnector scmConnector;
    private RallyConnector rallyConnector;
    private RallyConfiguration rallyConfiguration;

    @Inject
    public RallyService(RallyConnector connector, ScmConnector scmConnector, AdvancedConfiguration configuration, RallyConfiguration rallyConfiguration) throws RallyException {
        this.scmConnector = scmConnector;
        this.rallyConnector = connector;
        this.rallyConnector.configureProxy(configuration.getProxyUri());
        this.rallyConfiguration = rallyConfiguration;
    }

    public void closeConnection() throws RallyException {
        this.rallyConnector.close();
    }

    public void updateChangeset(RallyDetailsDTO details) throws RallyException {
        String repositoryRef;

        try {
            repositoryRef = this.rallyConnector.queryForRepository();
        } catch (RallyAssetNotFoundException exception) {
            if (this.rallyConfiguration.shouldCreateIfAbsent()) {
                repositoryRef = this.rallyConnector.createRepository();
            } else {
                throw exception;
            }
        }

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
        if (hasNoTasks(details)) {
            return;
        }

        String storyRef = this.rallyConnector.queryForStory(details.getId());
        RallyQueryBuilder.RallyQueryResponseObject taskObject = getTaskObjectByStoryRef(details, storyRef);

        RallyUpdateBean updateInfo = new RallyUpdateBean();

        updateInfo.setState(details.getTaskStatus().isEmpty()
                ? "In-Progress"
                : details.getTaskStatus());

        if (!details.getTaskToDO().isEmpty()) {
            updateInfo.setTodo(details.getTaskToDO());
        }

        if(!details.getTaskActuals().isEmpty()) {
            Double actuals = Double.parseDouble(details.getTaskActuals());
            actuals = actuals + taskObject.getTaskAttributeAsDouble("Actuals");
            updateInfo.setActual(Double.toString(actuals));
        }

        if(!details.getTaskEstimates().isEmpty()) {
            updateInfo.setEstimate(details.getTaskEstimates());
        }

        this.rallyConnector.updateTask(taskObject.getRef(), updateInfo);
    }

    private boolean hasNoTasks(RallyDetailsDTO details) {
        return details.getTaskIndex().isEmpty() && details.getTaskID().isEmpty();
    }

    private RallyQueryBuilder.RallyQueryResponseObject getTaskObjectByStoryRef(RallyDetailsDTO details, String storyRef) throws RallyException {
        return details.getTaskIndex().isEmpty()
                ? this.rallyConnector.queryForTaskById(storyRef, details.getTaskID())
                : this.rallyConnector.queryForTaskByIndex(storyRef, Integer.parseInt(details.getTaskIndex()));
    }
}
