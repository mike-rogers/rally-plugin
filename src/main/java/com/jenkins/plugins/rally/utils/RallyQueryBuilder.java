package com.jenkins.plugins.rally.utils;

import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.RallyException;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.io.IOException;

public class RallyQueryBuilder {
    public static class RallyQueryResponseObject {
        private final JsonObject jsonObject;

        public RallyQueryResponseObject(JsonObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        public Double getTaskAttributeAsDouble(String attribute) {
            return this.jsonObject.get(attribute).getAsDouble();
        }

        public String getRef() {
            return this.jsonObject.get("_ref").getAsString();
        }
    }

    private QueryRequest query;
    private RallyRestApi rallyRestApi;

    public static RallyQueryBuilder createQueryFrom(RallyRestApi restApi) {
        RallyQueryBuilder rallyQueryBuilder = new RallyQueryBuilder();
        rallyQueryBuilder.rallyRestApi = restApi;
        return rallyQueryBuilder;
    }

    public RallyQueryBuilder ofType(String type) {
        this.query = new QueryRequest(type);
        return this;
    }

    public RallyQueryBuilder withFetchValues(String... values) {
        this.query.setFetch(new Fetch(values));
        return this;
    }

    public RallyQueryBuilder inWorkspace(String workspace) {
        this.query.setWorkspace(workspace);
        return this;
    }

    public RallyQueryBuilder withQueryFilter(String field, String operator, String value) {
        this.query.setQueryFilter(new QueryFilter(field, operator, value));
        return this;
    }

    public String andExecuteReturningRef() throws RallyException {
        try {
            QueryResponse scmQueryResponse = this.rallyRestApi.query(this.query);

            if (scmQueryResponse.getTotalResultCount() == 0) {
                throw new RallyAssetNotFoundException();
            }

            return scmQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public RallyQueryBuilder andQueryFilter(String field, String operator, String value) {
        this.query.setQueryFilter(this.query.getQueryFilter().and(new QueryFilter(field, operator, value)));
        return this;
    }

    public RallyQueryResponseObject andExecuteReturningObject() throws RallyException {
        try {
            QueryResponse scmQueryResponse = this.rallyRestApi.query(this.query);

            if (scmQueryResponse.getTotalResultCount() == 0) {
                throw new RallyAssetNotFoundException();
            }

            return new RallyQueryResponseObject(scmQueryResponse.getResults().get(0).getAsJsonObject());
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }
}