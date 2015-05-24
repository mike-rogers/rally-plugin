package com.jenkins.plugins.rally.utils;

import com.jenkins.plugins.rally.connector.RallyDetailsDTO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommitMessageParser {
    public static RallyDetailsDTO parse(String commitMessage) {
        RallyDetailsDTO details = new RallyDetailsDTO();
        details.setId(getWorkItemFromCommitMessage(commitMessage));
        details.setTaskID(getTaskItemFromCommitMessage(commitMessage));
        details.setTaskIndex(getTaskIndexFromCommitMessage(commitMessage));
        return details;
    }

    private static String getWorkItemFromCommitMessage(String commitMessage) {
        return executeRegularExpression("\\b((?:US|DE)\\d+)\\b", commitMessage);
    }

    private static String getTaskItemFromCommitMessage(String commitMessage) {
        return executeRegularExpression("\\b(TA\\d+)\\b", commitMessage);
    }

    private static String getTaskIndexFromCommitMessage(String commitMessage) {
        return executeRegularExpression("(?:^|\\s)# ?(\\d+)\\b", commitMessage);
    }

    private static String executeRegularExpression(String regex, String commitMessage) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(commitMessage);
        return matcher.find() ? matcher.group(1) : "";
    }
}
