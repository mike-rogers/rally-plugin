package com.jenkins.plugins.rally.service;

import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.ScmConnector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RallyServiceTest {

    @Mock
    private ScmConnector scmConnector;

    @Mock
    private RallyConnector connector;

    private RallyService service;

    @Before
    public void setUp() throws Exception {
        this.service = new RallyService(this.connector, scmConnector, new AdvancedConfiguration("http://proxy.url", "false"));
    }

    @Test(expected=RallyAssetNotFoundException.class)
    public void shouldThrowErrorIfAttemptToUpdateWithoutValidStoryRef() throws Exception {
        when(this.connector.queryForStory("US12345")).thenThrow(new RallyAssetNotFoundException());

        RallyDetailsDTO details = new RallyDetailsDTO();
        details.setStory(true);
        details.setTaskID("TA54321");
        details.setId("US12345");

        this.service.updateRallyTaskDetails(details);
    }
}
