package com.oscar.downloader.service.job;

import com.oscar.downloader.model.job.Job;
import com.oscar.downloader.service.storage.GitRepoStorage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class JobProcessorServiceSingleton implements JobProcessorService {

    private final GitRepoStorage gitRepoStorage;

    private String currentJobId;

    public JobProcessorServiceSingleton(GitRepoStorage gitRepoStorage) {
        this.gitRepoStorage = gitRepoStorage;
    }

    @Override
    @SneakyThrows
    public Mono<Job> processJob(Job job) {
        synchronized (this) {
            if (this.currentJobId != null) {
                log.error("job under progress {}, concurrent leak!", this.currentJobId);
                return Mono.empty();
            }
            this.currentJobId = job.getId();
        }

        log.info("Processing job {}", job.getId());

        return Mono.just(job)
                .flatMap(j -> this.gitRepoStorage.proceedComponent(job))
                .doOnError(throwable -> log.error("Failed job : " + job.getId(), throwable.getMessage()))
                .onErrorResume(Mono::error)
                .doOnTerminate(this::finishJob)
                .then(Mono.just(job));
    }

    private synchronized void finishJob() {
        log.debug("Completed job {}", this.currentJobId);
        this.currentJobId = null;
    }

    @Override
    public synchronized boolean canDoAnotherJob() {
        return this.currentJobId == null;
    }

    public synchronized String getCurrentJobId() {
        return this.currentJobId;
    }

}
