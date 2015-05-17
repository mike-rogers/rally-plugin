package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.config.ScmConfiguration;
import hudson.model.AbstractBuild;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JenkinsConnectorTest {
    @Test
    public void shouldCaptureChangeSinceLastBuild() {
        String timestamp = "1970-01-01 00:00:00+0000";
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getPreviousBuild()).thenReturn(null);
        when(build.getNumber()).thenReturn(5);
        when(build.getTimestampString2()).thenReturn(timestamp);
        when(build.getPreviousBuild()).thenReturn(null);

        ScmConnector connector = new JenkinsConnector();
        connector.setScmConfiguration(new ScmConfiguration(null, null));

        Changes changes = connector.getChangesSinceLastBuild(build);

        List<ChangeInformation> changeInformation = changes.getChangeInformation();
        assertThat(changeInformation, hasSize(1));

        ChangeInformation info = changeInformation.get(0);
        assertThat(info.getBuild(), is(equalTo(build)));
        assertThat(info.getBuildNumber(), is(equalTo("5")));
        assertThat(info.getBuildTimeStamp(), is(equalTo(timestamp)));
        assertThat(info.getChangeLogSet(), is(nullValue()));
    }

    @Test
    public void shouldCaptureMultipleChangesSinceLastBuild() {
        String timestamp = "1970-01-01 00:00:00+0000";
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getPreviousBuild()).thenReturn(null);
        when(build.getNumber()).thenReturn(5);
        when(build.getTimestampString2()).thenReturn(timestamp);
        when(build.getPreviousBuild()).thenReturn(null);

        ScmConnector connector = new JenkinsConnector();
        connector.setScmConfiguration(new ScmConfiguration(null, null));

        Changes changes = connector.getChangesSinceLastBuild(build);

        List<ChangeInformation> changeInformation = changes.getChangeInformation();
        assertThat(changeInformation, hasSize(1));

        ChangeInformation info = changeInformation.get(0);
        assertThat(info.getBuild(), is(equalTo(build)));
        assertThat(info.getBuildNumber(), is(equalTo("5")));
        assertThat(info.getBuildTimeStamp(), is(equalTo(timestamp)));
        assertThat(info.getChangeLogSet(), is(nullValue()));
    }
}
