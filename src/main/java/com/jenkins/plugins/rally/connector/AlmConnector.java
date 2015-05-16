package com.jenkins.plugins.rally.connector;

import com.jenkins.plugins.rally.RallyException;

public interface AlmConnector {
    void updateChangeset(RallyDetailsDTO details) throws RallyException;
    boolean updateRallyTaskDetails(RallyDetailsDTO details) throws RallyException;
}
