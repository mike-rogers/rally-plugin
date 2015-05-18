package com.jenkins.plugins.rally.connector;

import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;

import java.io.IOException;
import java.net.URI;

public interface RallyApi {
    void setProxy(URI proxyUri, String username, String password);
    void setProxy(URI proxyUri);
    void close();
    CreateResponse create(CreateRequest createRequest) throws IOException;
    QueryResponse query(QueryRequest queryRequest) throws IOException;
    UpdateResponse update(UpdateRequest updateRequest) throws IOException;
}
