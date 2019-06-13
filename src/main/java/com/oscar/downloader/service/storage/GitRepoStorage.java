package com.oscar.downloader.service.storage;

import com.oscar.downloader.model.GitRepo;
import com.oscar.downloader.model.job.Job;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface GitRepoStorage {

    Mono<GitRepo> proceedComponent(Job job);
}
