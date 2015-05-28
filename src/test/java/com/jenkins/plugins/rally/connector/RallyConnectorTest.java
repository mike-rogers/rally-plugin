package com.jenkins.plugins.rally.connector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;



@RunWith(MockitoJUnitRunner.class)
public class RallyConnectorTest {
    private static final String WORKSPACE_NAME = "WORKSPACE_NAME";
    private static final String SCM_NAME = "SCM_NAME";

    @Mock
    private RallyRestApi rallyRestApi;

    private RallyConnector connector;

    @Before
    public void setUp() {
        RallyConfiguration rallyConfiguration = new RallyConfiguration("API_KEY", WORKSPACE_NAME, SCM_NAME);
        this.connector = new RallyConnector(this.rallyRestApi, rallyConfiguration);
    }

    @Test
    public void shouldConfigureVanillaProxyUri() throws Exception {
        URI sampleUri = new URI("http://proxy.net:1234");
        this.connector.configureProxy(sampleUri);

        verify(this.rallyRestApi).setProxy(sampleUri);
    }

    @Test
    public void shouldConfigureProxyWithUsernameAndPassword() throws Exception {
        URI sampleUri = new URI("http://username:password@proxy.net:1234");
        this.connector.configureProxy(sampleUri);

        verify(this.rallyRestApi).setProxy(sampleUri, "username", "password");
    }

    @Test(expected = RallyException.class)
    public void shouldThrowExceptionWhenSendingConfusingUsernamePasswordData() throws Exception {
        URI sampleUri = new URI("http://sample:username:password@proxy.net:1234");
        this.connector.configureProxy(sampleUri);
    }

    @Test
    public void shouldCloseRallyApiConnection() throws Exception {
        this.connector.close();

        verify(this.rallyRestApi).close();
    }

    @Test
    public void shouldGetStoryObjectAndReturnRef() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForStory("US12345");

        String expectedFilterString = new QueryFilter("FormattedID", "=", "US12345").toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test
    public void shouldGetDefectObjectAndReturnRef() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForDefect("DE12345");

        String expectedFilterString = new QueryFilter("FormattedID", "=", "DE12345").toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfStoryIsNotFound() throws Exception {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(response);

        this.connector.queryForStory("US12345");
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfDefectIsNotFound() throws Exception {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(response);

        this.connector.queryForDefect("DE12345");
    }

    @Test
    public void shouldGetScmRepositoryObjectAndReturnRef() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForRepository();

        String expectedFilterString = new QueryFilter("Name", "=", SCM_NAME).toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfRepositoryIsNotFound() throws Exception {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(response);

        this.connector.queryForRepository();
    }

    @Test
    public void shouldCreateChangesetObject() throws Exception {
        ArgumentCaptor<CreateRequest> createCaptor = ArgumentCaptor.forClass(CreateRequest.class);
        CreateResponse response = mock(CreateResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithRef("_ref"));
        when(response.wasSuccessful()).thenReturn(true);
        when(this.rallyRestApi.create(createCaptor.capture())).thenReturn(response);

        String ref = this.connector.createChangeset("_ref", "revision", "uri", "commitTimestamp", "message", "artifactRef");

        JsonElement capturedJson = new JsonParser().parse(createCaptor.getValue().getBody());
        JsonElement expectedJson = createJsonChangesetObject();
        assertThat(capturedJson, is(equalTo(expectedJson)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test(expected = RallyException.class)
    public void shouldThrowExceptionIfCreateChangesetOperationNotSuccessful() throws Exception {
        CreateResponse response = mock(CreateResponse.class);
        when(response.wasSuccessful()).thenReturn(false);
        when(this.rallyRestApi.create(any(CreateRequest.class))).thenReturn(response);

        this.connector.createChangeset(null, null, null, null, null, null);
    }

    private JsonObject createJsonObjectWithRef(String ref) {
        JsonObject object = new JsonObject();
        object.addProperty("_ref", ref);
        return object;
    }

    private JsonArray createJsonArrayWithRef(String ref) {
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("_ref", ref);
        array.add(object);
        return array;
    }

    private JsonObject createJsonChangesetObject() {
        JsonObject result = new JsonObject();

        JsonObject changesetObject = new JsonObject();

        JsonObject repositoryObject = new JsonObject();
        repositoryObject.addProperty("_ref", "_ref");

        JsonArray artifactArray = new JsonArray();
        JsonObject artifactObject = new JsonObject();
        artifactObject.addProperty("_ref", "artifactRef");
        artifactArray.add(artifactObject);

        changesetObject.add("SCMRepository", repositoryObject);
        changesetObject.addProperty("Revision", "revision");
        changesetObject.addProperty("Uri", "uri");
        changesetObject.addProperty("CommitTimestamp", "commitTimestamp");
        changesetObject.addProperty("Message", "message");
        changesetObject.add("Artifacts", artifactArray);

        result.add("Changeset", changesetObject);

        return result;
    }
}
