package com.jenkins.plugins.rally.connector;

import java.io.PrintStream;
import java.util.Arrays;

public class RallyDetailsDTO {
	
	private String msg;
	private String revision;
	private String timeStamp;
	private String id;
	private String [][] fileNameAndTypes;
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
	private boolean debugOn;
	
	private boolean isPrinted = false;
	
	public void printAllFields() {
		if(!isPrinted) {
			this.out.println("\tmsg= " + msg);
			this.out.println("\trevision= " + revision);
			this.out.println("\ttimeStamp= " + timeStamp);
			this.out.println("\tid= " + id);
			this.out.println("\tfileNameAndTypes= " + Arrays.deepToString(fileNameAndTypes));
			this.out.println("\tout= " + out);
			this.out.println("\torigBuildNumber= " + origBuildNumber);
			this.out.println("\tcurrentBuildNumber= " + currentBuildNumber);
			this.out.println("\ttaskID= " + taskID);
			this.out.println("\ttaskIndex= " + taskIndex);
			this.out.println("\ttaskStatus= " + taskStatus);
			this.out.println("\ttaskToDO= " + taskToDO);
			this.out.println("\ttaskEstimates= " + taskEstimates);
			this.out.println("\ttaskActuals= " + taskActuals);
			this.out.println("\tstory= " + story);
			this.out.println("\tdebugOn= " + debugOn);
			isPrinted = true;
		}	
	}
	
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
	public String[][] getFileNameAndTypes() {
		return fileNameAndTypes;
	}
	public void setFileNameAndTypes(String[][] fileNameAndTypes) {
		this.fileNameAndTypes = fileNameAndTypes;
	}
	public PrintStream getOut() {
		return out;
	}
	public void setOut(PrintStream out) {
		this.out = out;
	}
	public boolean isDebugOn() {
		return debugOn;
	}

	public void setDebugOn(boolean debugOn) {
		this.debugOn = debugOn;
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