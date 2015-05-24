package com.jenkins.plugins.rally.utils;

import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public final class CommitMessageParserTest {

    @Test
    public void shouldParseWorkItemIdFromCommitMessage() {
        String commitMessage = "US12345: do a thing";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getId(), is(equalTo("US12345")));
    }

    @Test
    public void shouldParseDefectFromCommitMessage() {
        String commitMessage = "de12345: fix a bug";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getId(), is(equalTo("de12345")));
    }

    @Test
    public void shouldNotCatchOddballWorkItemCases() {
        String[] commitMessages = new String[]
                {
                        "HELLO BUS1, DO YOU READ?",
                        "IDE1 IS BETTER THAN IDE2",
                        "\"BASTA1!!\" él gritó"
                };

        for (String commitMessage : commitMessages) {
            RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);
            assertThat(details.getId(), is(isEmptyString()));
        }
    }

    @Test
    public void shouldParseTaskIdFromCommitMessage() {
        String commitMessage = "US12345: (TA54321) do a thing";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskID(), is(equalTo("TA54321")));
        assertThat(details.getId(), is(equalTo("US12345")));
    }

    @Test
    public void shouldParseTaskIndexFromCommitMessage() {
        String commitMessage = "US12345: fixes #3";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskIndex(), is(equalTo("3")));
        assertThat(details.getId(), is(equalTo("US12345")));
    }
}
