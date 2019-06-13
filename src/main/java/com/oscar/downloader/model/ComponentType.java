package com.oscar.downloader.model;

public enum ComponentType {

    github, gitlab;

    public static ComponentType fromString(final String val) {
        if (val.equals(github.name())) {
            return github;
        } else if (val.equals(gitlab.name())) {
            return gitlab;
        }

        throw new IllegalStateException("Wrong component type " + val);
    }

}
