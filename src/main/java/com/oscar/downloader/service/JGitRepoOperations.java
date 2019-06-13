package com.oscar.downloader.service;

import com.oscar.downloader.configuration.GitProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class JGitRepoOperations {

    private final GitProperties gitProperties;

    public JGitRepoOperations(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    public Flux<CommitInfo> getCommits(Path repoPath, LocalDate dateFrom, LocalDate dateTo, boolean doCheckout) {
        return Flux.create(sink -> {
            File gitDir = repoPath.resolve(".git").toFile();
            if (!gitDir.exists()) {
                gitDir = repoPath.toFile();
            }
            if (!gitDir.exists()) {
                log.warn("Neither {} nor {} directories exist", repoPath.resolve(".git"), repoPath);
                sink.complete();
                return;
            }
            log.info("Getting commits of repo located at {} for [{}, {}], doCheckout={}",
                    gitDir, dateFrom, dateTo, doCheckout);

            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            repositoryBuilder.setMustExist(true);
            repositoryBuilder.setGitDir(gitDir);
            try {
                try (Repository repository = repositoryBuilder.build();
                     ObjectReader reader = repository.newObjectReader();
                     DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    diffFormatter.setRepository(repository);

                    try (Git git = new Git(repository)) {
                        ObjectId head = repository.resolve(Constants.HEAD);
                        if (head == null) {
                            log.warn("No commits of {} found", repoPath);
                        } else {
                            int commitsCount = 0;
                            try (RevWalk revWalk = new RevWalk(repository)) {
                                revWalk.markStart(revWalk.parseCommit(head));

                                revWalk.sort(RevSort.REVERSE);

                                for (RevCommit revCommit : revWalk) {
                                    String revisionNumber = extractRevisionNumber(revCommit);


                                    if (doCheckout) {
                                        log.trace("Making checkout of {}", revisionNumber);
                                        try {
                                            git.checkout().setName(revisionNumber).call();
                                        } catch (GitAPIException e) {
                                            log.error("Failed to checkout rev " + revisionNumber, e);
                                        }
                                    }

                                    LocalDate commitDate = getCommitTimestamp(revCommit.getCommitterIdent()).toLocalDate();
                                    if (dateFrom != null && commitDate.isBefore(dateFrom)) {
                                        log.debug("Ignore commit of date {} which is before {}", commitDate, dateFrom);
                                        continue;
                                    }
                                    if (dateTo != null && commitDate.isAfter(dateTo)) {
                                        log.debug("Do not check commits of date after {}", dateTo);
                                        break;
                                    }

                                    CommitInfo commitInfo = new CommitInfo();

                                    keepAuthorInfo(revCommit, commitInfo);
                                    keepCommitterInfo(revCommit, commitInfo);
                                    commitInfo.setMessage(revCommit.getFullMessage());
                                    commitInfo.setRevisionNumber(revisionNumber);

                                    if (revCommit.getParentCount() <= 1) {
                                        keepCommitChangesStat(reader, diffFormatter, revCommit, commitInfo);
                                    }

                                    commitsCount++;

                                    log.trace("Collected {} commit info: {}", commitsCount, commitInfo);

                                    sink.next(commitInfo);
                                }
                            }

                            log.info("Finished collecting of {} commits statistics of repo located at {}", commitsCount, repoPath);

                        }
                    }
                }

                sink.complete();
            } catch (IOException e) {
                log.error("Error happened while analysing git commits", e);
                sink.error(e);
            }
        });
    }

    private String extractRevisionNumber(RevCommit revCommit) {
        String commitId = revCommit.getId().toString();
        if (commitId != null && !commitId.isEmpty()) {
            String[] parts = commitId.split(" ");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return commitId;
    }

    private void keepCommitChangesStat(ObjectReader reader, DiffFormatter diffFormatter, RevCommit revCommit, CommitInfo commitInfo) {
        if (revCommit.getParentCount() == 0) {
            diffCommits(null, revCommit, commitInfo, reader, diffFormatter);
        } else {
            diffCommits(revCommit.getParent(0), revCommit, commitInfo, reader, diffFormatter);
        }
    }

    private void keepCommitterInfo(RevCommit revCommit, CommitInfo commitInfo) {
        PersonIdent committerIdent = revCommit.getCommitterIdent();
        commitInfo.setCommitterEmail(committerIdent.getEmailAddress());
        commitInfo.setCommitterName(committerIdent.getName());
        commitInfo.setCommitterDate(getCommitTimestamp(committerIdent));
    }

    private void keepAuthorInfo(RevCommit revCommit, CommitInfo commitInfo) {
        PersonIdent authorIdent = revCommit.getAuthorIdent();
        commitInfo.setAuthorEmail(authorIdent.getEmailAddress());
        commitInfo.setAuthorName(authorIdent.getName());
        commitInfo.setAuthorDate(getCommitTimestamp(authorIdent));
    }

    private ZonedDateTime getCommitTimestamp(PersonIdent authorIdent) {
        return ZonedDateTime.ofInstant(authorIdent.getWhen().toInstant(), authorIdent.getTimeZone().toZoneId());
    }

    private void diffCommits(RevCommit oldCommit, RevCommit newCommit, CommitInfo commitInfo,
                             ObjectReader reader, DiffFormatter diffFormatter) {
        try {
            AbstractTreeIterator oldTreeIter;
            if (oldCommit != null) {
                oldTreeIter = new CanonicalTreeParser(null, reader, oldCommit.getTree());
            } else {
                oldTreeIter = new EmptyTreeIterator();
            }
            AbstractTreeIterator newTreeIter = new CanonicalTreeParser(null, reader, newCommit.getTree());

            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

            int insertionsCount = 0;
            int deletionsCount = 0;
            commitInfo.setFileChanged(entries.size());

            if (!gitProperties.isCommitsDiff()) {
                return;
            }

            for (DiffEntry entry : entries) {
                log.trace("Start");
                FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                List<? extends HunkHeader> hunks = fileHeader.getHunks();
                log.trace("Processing entry {} of {} hunks", entry, hunks.size());
                int hunkCntr = 0;
                for (HunkHeader hunk : hunks) {
                    EditList edits = hunk.toEditList();
                    log.trace("Processing hunk {} of {}, it has {} edits", ++hunkCntr, hunks.size(), edits.size());
                    int editsCount = 0;
                    for (Edit edit : edits) {
                        log.trace("Processing edit {} of {}: {}", ++editsCount, edits.size(), edit);
                        switch (edit.getType()) {
                            case INSERT: {
                                insertionsCount += (edit.getEndB() - edit.getBeginB());
                                break;
                            }
                            case DELETE: {
                                deletionsCount += (edit.getEndA() - edit.getBeginA());
                                break;
                            }
                            case REPLACE: {
                                insertionsCount += (edit.getEndB() - edit.getBeginB());
                                deletionsCount += (edit.getEndA() - edit.getBeginA());
                                break;
                            }
                        }
                    }
                }
                log.trace("End. insertionsCount={}, deletionsCount={}", insertionsCount, deletionsCount);
            }
            commitInfo.setInsertionsCount(insertionsCount);
            commitInfo.setDeletionsCount(deletionsCount);
        } catch (IOException e) {
            log.warn("Failed to get commits diff", e);
        }
    }

}
