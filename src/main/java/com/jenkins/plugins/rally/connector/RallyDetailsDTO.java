package com.jenkins.plugins.rally.connector;

import hudson.scm.EditType;

import java.io.PrintStream;
import java.util.List;

public class RallyDetailsDTO {
    public static class FilenameAndAction {
        public String filename;
        public EditType action;
    }

    private String msg;
    private String revision;
    private String timeStamp;
    private String id;
    private List<FilenameAndAction> filenamesAndActions;
    private PrintStream out;
    private String origBuildNumber;
    private String currentBuildNumber;
    private String taskID = "";
    private String taskIndex = "";
    private String taskStatus = "";
    private String taskToDO = "";
    private String taskEstimates = "";
    private String taskActuals = "";
    private boolean story;

    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public String getRevision() {
        return revision;
    }
    public void setRevision(String revision) {
        if(revision == null)
            this.revision = "0";
        else
            this.revision = revision;
    }
    public String getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public List<FilenameAndAction> getFilenamesAndActions() {
        return filenamesAndActions;
    }
    public void setFilenamesAndActions(List<FilenameAndAction> filenamesAndActions) {
        this.filenamesAndActions = filenamesAndActions;
    }
    public PrintStream getOut() {
        return out;
    }
    public void setOut(PrintStream out) {
        this.out = out;
    }
    public String getOrigBuildNumber() {
        return origBuildNumber;
    }
    public void setOrigBuildNumber(String origBuildNumber) {
        this.origBuildNumber = origBuildNumber;
    }
    public String getCurrentBuildNumber() {
        return currentBuildNumber;
    }
    public void setCurrentBuildNumber(String currentBuildNumber) {
        this.currentBuildNumber = currentBuildNumber;
    }
    public String getTaskID() {
        return taskID;
    }
    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }
    public String getTaskIndex() {
        return taskIndex;
    }
    public void setTaskIndex(String taskIndex) {
        this.taskIndex = taskIndex;
    }
    public String getTaskStatus() {
        return taskStatus;
    }
    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }
    public String getTaskToDO() {
        return taskToDO;
    }
    public void setTaskToDO(String taskToDO) {
        this.taskToDO = taskToDO;
    }
    public String getTaskEstimates() {
        return taskEstimates;
    }
    public void setTaskEstimates(String taskEstimates) {
        this.taskEstimates = taskEstimates;
    }
    public String getTaskActuals() {
        return taskActuals;
    }
    public void setTaskActuals(String taskActuals) {
        this.taskActuals = taskActuals;
    }
    public boolean isStory() {
        return story;
    }
    public void setStory(boolean story) {
        this.story = story;
    }
}
