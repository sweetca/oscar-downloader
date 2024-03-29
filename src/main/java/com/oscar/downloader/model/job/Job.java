package com.oscar.downloader.model.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

    private String id;

    private Map<String, Object> payload = new HashMap<>();

    @JsonIgnore
    public String getComponentPath() {
        if (payload != null && payload.size() > 0) {
            String componentPath = (String) payload.get(PayloadType.componentPath.name());
            if (componentPath != null) {
                return componentPath.trim();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getComponentId() {
        if (payload != null && payload.size() > 0) {
            String componentId = (String) payload.get(PayloadType.component.name());
            if (componentId != null) {
                return componentId.trim();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getGitUrl() {
        if (payload != null && payload.size() > 0) {
            String gitUrl = (String) payload.get(PayloadType.gitUrl.name());
            if (gitUrl != null) {
                return gitUrl.trim();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getGitBranch() {
        if (payload != null && payload.size() > 0) {
            String gitBranch = (String) payload.get(PayloadType.gitBranch.name());
            if (gitBranch != null) {
                return gitBranch.trim();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getAccessToken() {
        if (payload != null && payload.size() > 0) {
            String accessToken = (String) payload.get(PayloadType.gitToken.name());
            if (accessToken != null) {
                return accessToken.trim();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getUserName() {
        if (payload != null && payload.size() > 0) {
            String userName = (String) payload.get(PayloadType.gitName.name());
            if (userName != null) {
                return userName.trim();
            }
        }
        return null;
    }
}
