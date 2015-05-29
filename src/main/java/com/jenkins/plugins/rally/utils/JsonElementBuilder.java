package com.jenkins.plugins.rally.utils;


import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.RallyException;

public class JsonElementBuilder {
    private JsonObject object;
    private String arrayName;

    public JsonElementBuilder() {
        this.object = new JsonObject();
    }

    public JsonElementBuilder withPropertyAndValue(String reference, String value) {
        this.object.getAsJsonObject().addProperty(reference, value);
        return this;
    }

    public static JsonElementBuilder thatReferencesObject() throws RallyException {
        return new JsonElementBuilder();
    }

    public static JsonElementBuilder anObjectWithProperty(String name, String value) {
        JsonElementBuilder builder = new JsonElementBuilder();
        builder.object.addProperty(name, value);
        return builder;
    }

    public JsonElementBuilder withName(String name) {
        this.arrayName = name;
        return this;
    }

    public String getArrayName() {
        return this.arrayName;
    }

    public JsonObject build() {
        return this.object;
    }
}