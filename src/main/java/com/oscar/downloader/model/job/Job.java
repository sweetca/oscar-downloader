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
    public String getGitId() {
        if (payload != null && payload.size() > 0) {
            String gitId = (String) payload.get(PayloadType.gitId.name());
            if (gitId != null) {
                return gitId.trim();
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
    public String getComponentType() {
        if (payload != null && payload.size() > 0) {
            String componentType = (String) payload.get(PayloadType.componentType.name());
            if (componentType != null) {
                return componentType.trim();
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
            String accessToken = (String) payload.get(PayloadType.accessToken.name());
            if (accessToken != null) {
                return accessToken.trim();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getUserName() {
        if (payload != null && payload.size() > 0) {
            String userName = (String) payload.get(PayloadType.userName.name());
            if (userName != null) {
                return userName.trim();
            }
        }
        return null;
    }
}
