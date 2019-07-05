package com.oscar.downloader.service.storage;

import com.oscar.downloader.model.GitRepo;
import com.oscar.downloader.model.job.Job;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class LocalGitRepoStorage implements GitRepoStorage {

    private final String origin = "origin/";

    LocalGitRepoStorage() {
    }

    @Override
    public Mono<GitRepo> proceedComponent(Job job) {
        String url = job.getGitUrl();
        String id = job.getComponentId();
        String path = job.getComponentPath();
        String branch = job.getGitBranch();
        String accessToken = job.getAccessToken();
        String userName = job.getUserName();

        GitRepo repo = GitRepo
                .aRepo()
                .id(id)
                .url(url)
                .branch(branch)
                .path(Paths.get(path))
                .accessToken(accessToken)
                .userName(userName)
                .create();

        return Mono.fromCallable(() -> clone(repo))
                .doOnSuccess(gitRepo -> log.info("Completed cloning of repo {}", repo.getUrl()))
                .doOnError(e -> {
                    log.error("Failed to clone repo {} due to {}", repo, e);
                    try {
                        FileSystemUtils.deleteRecursively(repo.getPath());
                    } catch (IOException ex) {
                        log.debug("Failed to clean dir " + repo.getPath().toString(), ex);
                    }
                })
                .onErrorResume(Mono::error);
    }

    private GitRepo clone(GitRepo repo) throws GitAPIException, IOException {
        log.info("Cloning {} repo from {} to {}... (JGit)", repo.getUrl(), repo.getPath());

        boolean newClone = createRepoDir(repo.getPath());

        CredentialsProvider cp = null;
        if (repo.getUserName() != null && repo.getAccessToken() != null) {
            cp = new UsernamePasswordCredentialsProvider(repo.getUserName(), encodeValue(repo.getAccessToken()));
        }

        if (newClone) {
            log.info("Clone repo {}", repo.getPath());
            Git git = Git.cloneRepository()
                    .setURI(repo.getUrl())
                    .setDirectory(repo.getPath().toFile())
                    .setCredentialsProvider(cp)
                    .call();

            git.checkout()
                    .setCreateBranch(true)
                    .setName(origin + repo.getBranch())
                    .setStartPoint(origin + repo.getBranch())
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call();

            git.close();
        } else {
            log.info("Pull repo {}", repo.getPath());
            Git git = Git.open(repo.getPath().toFile());

            git.pull()
                    .setCredentialsProvider(cp)
                    .call();

            git.branchCreate()
                    .setForce(true)
                    .setName(origin + repo.getBranch())
                    .setStartPoint(origin + repo.getBranch())
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call();

            git.checkout()
                    .setName(origin + repo.getBranch())
                    .call();

            git.pull()
                    .setCredentialsProvider(cp)
                    .call();

            git.close();
        }

        return repo;
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("Error url encode {}", value, e);
        }
        return value;
    }

    private boolean createRepoDir(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Created directory {} to keep repository", path);
            return true;
        }

        log.info("Repositories directory {} already exists", path);
        return false;
    }

}
