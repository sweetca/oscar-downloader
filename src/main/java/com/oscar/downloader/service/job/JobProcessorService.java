package com.oscar.downloader.service.job;

import com.oscar.downloader.model.job.Job;
import reactor.core.publisher.Mono;

public interface JobProcessorService {

    String getCurrentJobId();

    Mono<Job> processJob(Job job);

    boolean canDoAnotherJob();
}
