package com.oscar.downloader.service;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CommitInfo {

    private String revisionNumber;

    private String authorEmail;

    private String authorName;

    private ZonedDateTime authorDate;

    private String committerEmail;

    private String committerName;

    private ZonedDateTime committerDate;

    private int fileChanged;

    private int insertionsCount;

    private int deletionsCount;

    private String message;

}
