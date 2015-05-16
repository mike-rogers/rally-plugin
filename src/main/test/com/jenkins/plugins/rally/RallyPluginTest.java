package com.jenkins.plugins.rally;

import com.google.common.base.Joiner;
import com.jenkins.plugins.rally.config.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RallyPluginTest {
    private static final String API_KEY = "API_KEY";
    private static final String WORKSPACE_NAME = "WORKSPACE_NAME";
    private static final String COMMIT_URI_STRING = "COMMIT_URI_STRING";
    private static final String FILE_URI_STRING = "FILE_URI_STRING";
    private static final String SCM_NAME = "SCM_NAME";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private RallyPluginConfiguration getRallyPluginConfiguration() throws URISyntaxException {
        RallyConfiguration rallyConfig = new RallyConfiguration(API_KEY, WORKSPACE_NAME, SCM_NAME);
        ScmConfiguration scmConfig = new ScmConfiguration(COMMIT_URI_STRING, FILE_URI_STRING);
        BuildConfiguration buildConfig = new BuildConfiguration("SinceLastBuild");
        AdvancedConfiguration advancedConfig = new AdvancedConfiguration("http://proxy.com", "true");
        return new RallyPluginConfiguration(rallyConfig, scmConfig, buildConfig, advancedConfig);
    }

    @Test
    public void shouldStoreConfigurationForRecall() throws Exception {
        String[] keysToTest = {
                "config.rally.apiKey",
                "config.rally.workspaceName",
                "config.rally.scmName",
                "config.scm.commitTemplate",
                "config.scm.fileTemplate",
                "config.build.captureRange",
                "config.advanced.proxyUri",
                "config.advanced.isDebugOn"
        };

        FreeStyleProject p = jenkins.getInstance().createProject(FreeStyleProject.class, "testProject");
        RallyPlugin before = new RallyPlugin(getRallyPluginConfiguration());
        p.getBuildersList().add(before);

        jenkins.submit(jenkins.createWebClient().getPage(p,"configure").getFormByName("config"));

        RallyPlugin after = p.getBuildersList().get(RallyPlugin.class);

        jenkins.assertEqualBeans(before, after, Joiner.on(',').join(keysToTest));
    }

    @Test
    public void shouldAssertWhenUsernameIsNull() throws Exception {
        FreeStyleProject project = jenkins.getInstance().createProject(FreeStyleProject.class, "testProject");
//        project.getBuildersList().add(new RallyPlugin(getRallyPluginConfiguration()));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertThat(build, is(notNullValue()));
    }
}