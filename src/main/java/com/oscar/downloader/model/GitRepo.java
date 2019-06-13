package com.oscar.downloader.model;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder(builderMethodName = "aRepo", buildMethodName = "create")
public class GitRepo {

    private String id;

    private String url;

    private String branch;

    private ComponentType type;

    private Path path;

    private String accessToken;

    private String userName;

    @Override
    public String toString() {
        return String.format("%s (%s)", url, id);
    }
}
