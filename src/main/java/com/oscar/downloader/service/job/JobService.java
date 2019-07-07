package com.oscar.downloader.service.job;

import com.oscar.downloader.configuration.JobProperties;
import com.oscar.downloader.model.job.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class JobService {

    private static final String FIND_JOB_URL = "find_job/%d/%s";

    private static final String FINISH_JOB_URL = "finish_job/%s/%s";

    private final JobProperties jobProperties;

    private final JobProcessorService jobProcessorService;

    private WebClient webClient;

    @Autowired
    public JobService(JobProperties jobProperties,
                      JobProcessorService jobProcessorService) {
        this.jobProperties = jobProperties;
        this.jobProcessorService = jobProcessorService;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient
                .builder()
                .baseUrl(this.jobProperties.getUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Scheduled(cron = "${job.checkCron}")
    public void checkForNewJob() {
        if (this.jobProcessorService.canDoAnotherJob()) {
            log.debug("Start checkForNewJob");
            findNewJob()
                    .doOnError(e -> log.debug("Cannot get job. {}", e.getMessage()))
                    .doOnNext(job -> log.info("Received job {}", job.getId()))
                    .flatMap(this.jobProcessorService::processJob)
                    .doOnSuccess(job -> finishJob(job.getId()))
                    .subscribe();
        } else {
            log.debug("Job processor service is busy {}", this.jobProcessorService.getCurrentJobId());
        }
    }

    private synchronized Mono<Job> findNewJob() {
        String url = String.format(FIND_JOB_URL, this.jobProperties.getType(), this.jobProperties.getPodName());
        log.debug("Looking for git clone job via url : {}", url);
        return this.webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Job.class);
    }

    private void finishJob(String jobId) {
        String url = String.format(FINISH_JOB_URL, jobId, this.jobProperties.getPodName());
        log.debug("Finishing job : {}", url);
        this.webClient
                .put()
                .uri(url)
                .exchange()
                .subscribe(
                        clientResponse -> log.info("Reported that job with id {} is finished, result code {}", jobId, clientResponse.statusCode()),
                        throwable -> log.warn("Failed to report finished job {} due to the error {}", jobId, throwable.getMessage()));
    }

}
