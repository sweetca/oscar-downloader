package com.oscar.downloader.service.job;

import com.oscar.downloader.configuration.JobProperties;
import com.oscar.downloader.model.job.Job;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

    private static final String FINISH_JOB_URL = "finish_job/%s";

    private final JobProperties jobProperties;

    private final JobProcessorService jobProcessorService;

    private WebClient webClient;

    public JobService(JobProperties jobProperties,
                      JobProcessorService jobProcessorService) {
        this.jobProperties = jobProperties;
        this.jobProcessorService = jobProcessorService;
    }

    @PostConstruct
    @SneakyThrows
    public void init() {
        webClient = WebClient
                .builder()
                .baseUrl(jobProperties.getUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Scheduled(cron = "${job.checkCron}")
    public void checkForNewJob() {
        if (jobProcessorService.canDoAnotherJob()) {
            log.debug("Job processor service is ready for next job processing");
            findNewJob()
                    .doOnError(e -> log.error("Cannot get job. {}", e.getMessage()))
                    .doOnNext(job -> log.info("Received job {}", job.getId()))
                    .flatMap(jobProcessorService::processJob)
                    .doOnSuccess(job -> finishJob(job.getId()))
                    .subscribe();
        } else {
            log.debug("Job processor service is busy doing job {}", jobProcessorService.getCurrentJobId());
        }
    }

    private synchronized Mono<Job> findNewJob() {
        log.debug("Looking for git clone job at {}, job types {}", jobProperties.getUrl(), jobProperties.getType());
        return findNewJob(jobProperties.getType());
    }

    private Mono<Job> findNewJob(int jobType) {
        return webClient
                .get()
                .uri(String.format(FIND_JOB_URL, jobType, jobProperties.getPodName()))
                .retrieve()
                .bodyToMono(Job.class);
    }

    private void finishJob(String jobId) {
        log.info("Finishing job with id {}", jobId);
        webClient
                .put()
                .uri(String.format(FINISH_JOB_URL, jobId))
                .exchange()
                .subscribe(clientResponse -> log.info("Reported that job with id {} is finished, result code {}", jobId, clientResponse.statusCode()),
                        throwable -> log.warn("Failed to report finished job {} due to the error {}", jobId, throwable.getMessage()));
    }

}
