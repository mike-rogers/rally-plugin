package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyAttributes;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.utils.TemplatedUriResolver;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenkinsConnector implements ScmConnector {
    private final TemplatedUriResolver uriResolver;
    private ScmConfiguration config;
    private BuildConfiguration buildConfig;
    private AdvancedConfiguration advancedConfig;

    public JenkinsConnector() {
        this.uriResolver = new TemplatedUriResolver();
    }

    public void setScmConfiguration(ScmConfiguration configuration) {
        this.config = configuration;
    }

    public void setBuildConfiguration(BuildConfiguration configuration) {
        this.buildConfig = configuration;
    }

    public void setAdvancedConfiguration(AdvancedConfiguration configuration) {
        this.advancedConfig = configuration;
    }

    public List<RallyDetailsDTO> getChanges(AbstractBuild build, PrintStream out) throws RallyException {
        Changes changes;
        // TODO: if a third is added it might be time to inheritance it up
        switch(this.buildConfig.getCaptureRangeAsEnum()) {
            case SinceLastBuild:
                changes = getChangesSinceLastBuild(build);
                break;
            case SinceLastSuccessfulBuild:
                changes = getChangesSinceLastSuccessfulBuild(build);
                break;
            default:
                throw new RallyException("Looking at invalid capture range");
        }

        List<RallyDetailsDTO> detailsBeans = new ArrayList<RallyDetailsDTO>();
        for (ChangeInformation info : changes.getChangeInformation()) {
            for (Object item : info.getChangeLogSet().getItems()) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) item;
                detailsBeans.add(createRallyDetailsDTO(info, entry, build, out));
            }
        }

        return detailsBeans;
    }

    private Changes getChangesSinceLastBuild(AbstractBuild build) {
        Run run = build.getPreviousBuild();
        return new Changes(build, run != null ? run.getNumber() + 1 : build.getNumber());
    }

    private Changes getChangesSinceLastSuccessfulBuild(AbstractBuild build) {
        Run run = build.getPreviousBuild();
        while (run != null && (run.getResult() == null || run.getResult().isWorseThan(Result.SUCCESS)))
            run = run.getPreviousBuild();

        return new Changes(build, run != null ? run.getNumber() + 1 : build.getNumber());
    }

    private RallyDetailsDTO createRallyDetailsDTO(
            ChangeInformation changeInformation,
            ChangeLogSet.Entry changeLogEntry,
            AbstractBuild build,
            PrintStream out) {
        Boolean isDebugOn = this.advancedConfig.getIsDebugOn() == null
                ? false
                : Boolean.parseBoolean(this.advancedConfig.getIsDebugOn());
        RallyDetailsDTO details = new RallyDetailsDTO();
        details.setOrigBuildNumber(changeInformation.getBuildNumber());
        details.setCurrentBuildNumber(String.valueOf(build.number));
        details.setMsg(getMessage(changeLogEntry, details.getOrigBuildNumber(), details.getCurrentBuildNumber()));
        details.setFileNameAndTypes(getFileNameAndTypes(changeLogEntry));
        details.setId(getId(changeLogEntry));
        details.setOut(out);
        details.setDebugOn(isDebugOn);
        details.setStory(details.getId().startsWith("US"));
        details.setRevision(changeLogEntry.getCommitId());

        if (details.isStory()) {
            populateTaskDetails(details, changeLogEntry.getMsg());
        }

        if (changeLogEntry.getTimestamp() == -1) {
            details.setTimeStamp(changeInformation.getBuildTimeStamp());
        } else {
            details.setTimeStamp(toTimeZoneTimeStamp(changeLogEntry.getTimestamp()));
        }

        return details;
    }

    private void populateTaskDetails(final RallyDetailsDTO details, final String msg) {
        if (msg != null) {
            details.setTaskIndex(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.TaskIndex));
            details.setTaskID(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.TaskID));
            String rallyStatus = mapStatusToRallyStatus(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.Status));
            details.setTaskStatus(rallyStatus);
            details.setTaskActuals(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.Actuals));
            details.setTaskToDO(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.ToDo));
            details.setTaskEstimates(getRallyAttrValue(msg.toUpperCase(), RallyAttributes.Estimates));
        }
    }

    private String getRallyAttrValue(String comment, RallyAttributes attr) {
        String value = StringUtils.substringAfter(comment, attr.getType1());
        if(value.isEmpty())
            value = StringUtils.substringAfter(comment, attr.getType2());
        if(value.isEmpty())
            value = StringUtils.substringAfter(comment, attr.getType3());
        if(value.isEmpty())
            value = StringUtils.substringAfter(comment, attr.getType4());

        return StringUtils.substringBefore(value, ",").trim();
    }

    private String mapStatusToRallyStatus(String status) {
        String rallyStatus = "";
        if(StringUtils.startsWithIgnoreCase(status, "In-"))
            rallyStatus = "In-Progress";
        if(StringUtils.startsWithIgnoreCase(status, "comp"))
            rallyStatus = "Completed";
        if(StringUtils.startsWithIgnoreCase(status, "def"))
            rallyStatus = "Defined";
        return rallyStatus;
    }


    private String getMessage(ChangeLogSet.Entry cse, String origBuildNumber, String currentBuildNumber) {
        String msg;
        if(origBuildNumber.equals(currentBuildNumber))
            msg = cse.getAuthor() + " # " + cse.getMsg() + " (Build #" + origBuildNumber + ")";
        else
            msg = cse.getAuthor() + " # " + cse.getMsg() + " (Builds #" + currentBuildNumber + " - " + origBuildNumber + ")";
        return msg;
    }

    // TODO: rework with Map?
    private String[][] getFileNameAndTypes(ChangeLogSet.Entry cse) {
        String [][] fileNames = new String[cse.getAffectedFiles().size()][2];
        int i = 0;
        for(ChangeLogSet.AffectedFile files : cse.getAffectedFiles()) {
            fileNames[i][0] = files.getPath();
            fileNames[i][1] = files.getEditType().getName();
            i++;
        }
        return fileNames;
    }

    private String getId(ChangeLogSet.Entry cse) {
        String id = "";
        String comment = cse.getMsg();
        if(comment != null) {
            id = evaluateRegEx(comment, "US[0-9]+[\\w]*");
            if(id.isEmpty())
                id = evaluateRegEx(comment, "DE[0-9]+[\\w]*");
        }
        return id.trim();
    }

    private String evaluateRegEx(String comment, String regEx) {
        String result = "";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(comment);
        if(m.find())
            result = m.group(0);
        return result;
    }

    private String toTimeZoneTimeStamp(long time) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

        return df.format(new Date(time));
    }

    public String getRevisionUriFor(String revision) {
        return this.uriResolver.resolveCommitUri(this.config.getCommitTemplate(), revision);
    }
}
