package com.jenkins.plugins.rally;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jenkins.plugins.rally.connector.RallyAttributes;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.BuildDetails;
import com.jenkins.plugins.rally.scm.ChangeInformation;
import com.jenkins.plugins.rally.scm.Changes;

public class PostBuildHelper {

	static Changes getChanges(String changesSince, String startDate, String endDate, AbstractBuild build, PrintStream out) {
		Changes changes = null;
		try {
			if(changesSince.equals("changesSinceLastBuild")) {
				changes = BuildDetails.getChangesSinceLastBuild(build);
			} else if(changesSince.equals("changesSinceLastSuccessfulBuild")) {
				changes = BuildDetails.getChangesSinceLastSuccessfulBuild(build);
			} else if(changesSince.equals("changesBetDates")) {
				changes = BuildDetails.getSince(startDate, endDate, build);
			}
		} catch(Exception e) {
			out.println("rally update plug-in error: error while retrieving scm changes form jenking: ");
			e.printStackTrace(out);
		}
		return changes;
	}

	static RallyDetailsDTO populateRallyDetailsDTO(String debugOn, final AbstractBuild build, final ChangeInformation ci, final ChangeLogSet.Entry cse, PrintStream out) {
		RallyDetailsDTO rdto = new RallyDetailsDTO();
		rdto.setOrigBuildNumber(ci.getBuildNumber());
		rdto.setCurrentBuildNumber(String.valueOf(build.number));
		rdto.setMsg(getMessage(cse, rdto.getOrigBuildNumber(), rdto.getCurrentBuildNumber()));
		rdto.setFileNameAndTypes(getFileNameAndTypes(cse));
		rdto.setId(getId(cse));
		rdto.setOut(out);
		rdto.setDebugOn(Boolean.valueOf(debugOn));
		if(rdto.getId().startsWith("US")) {
			rdto.setStory(true);
			populateTaskDetails(rdto, cse.getMsg());
		}
		else
			rdto.setStory(false);
		rdto.setRevision(cse.getCommitId());
		if(cse.getTimestamp() == -1)
			rdto.setTimeStamp(ci.getBuildTimeStamp());
		else
			rdto.setTimeStamp(toTimeZoneTimeStamp(cse.getTimestamp()));
		return rdto;
	}

	static void populateTaskDetails(final RallyDetailsDTO rdto, final String msg) {
		if(msg != null) {
			rdto.setTaskIndex(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.TaskIndex));
			rdto.setTaskID(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.TaskID));
			String rallyStatus = mapStatusToRallyStatus(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.Status));
			rdto.setTaskStatus(rallyStatus);
			rdto.setTaskActuals(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.Actuals));
			rdto.setTaskToDO(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.ToDo));
			rdto.setTaskEstimates(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.Estimates));
		}
	}

	static String getRallyAttrValue(String comment, RallyAttributes attr) {
		String value = StringUtils.substringAfter(comment, attr.getType1());
		if(value.isEmpty())
			value = StringUtils.substringAfter(comment, attr.getType2());
		if(value.isEmpty())
			value = StringUtils.substringAfter(comment, attr.getType3());
		if(value.isEmpty())
			value = StringUtils.substringAfter(comment, attr.getType4());

		return StringUtils.substringBefore(value, ",").trim();
	}

	static String mapStatusToRallyStatus(String status) {
		String rallyStatus = "";
		if(StringUtils.startsWithIgnoreCase(status, "In-"))
			rallyStatus = "In-Progress";
		if(StringUtils.startsWithIgnoreCase(status, "comp"))
			rallyStatus = "Completed";
		if(StringUtils.startsWithIgnoreCase(status, "def"))
			rallyStatus = "Defined";
		return rallyStatus;
	}

	static String toTimeZoneTimeStamp(long time) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

		return df.format(new Date(time));
	}

	static String [][] getFileNameAndTypes(ChangeLogSet.Entry cse) {
		String [][] fileNames = new String[cse.getAffectedFiles().size()][2];
		int i = 0;
		for(AffectedFile files : cse.getAffectedFiles()) {
			fileNames[i][0] = files.getPath();
			fileNames[i][1] = files.getEditType().getName();
			i++;
		}
		return fileNames;
	}

	static String getMessage(ChangeLogSet.Entry cse, String origBuildNumber, String currentBuildNumber) {
		String msg;
		if(origBuildNumber.equals(currentBuildNumber))
			msg = cse.getAuthor() + " # " + cse.getMsg() + " (" + origBuildNumber + ")";
		else
			msg = cse.getAuthor() + " # " + cse.getMsg() + " (" + currentBuildNumber + " - " + origBuildNumber + ")";
		return msg;
	}

	static String getId(ChangeLogSet.Entry cse) {
		String id = "";
		String comment = cse.getMsg();
		if(comment != null) {
			id = evaluteRegEx(comment, "US[0-9]+[\\w]*");
			if(id.isEmpty())
				id = evaluteRegEx(comment, "DE[0-9]+[\\w]*");
		}
		return id.trim();
	}

	static String evaluteRegEx(String comment, String regEx) {
		String result = "";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(comment);
		if(m.find())
			result = m.group(0);
		return result;
	}
}
