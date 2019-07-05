package com.oscar.downloader.web;

import com.oscar.downloader.model.job.Job;
import com.oscar.downloader.service.job.JobProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Date;

@RestController
@Slf4j
public class IndexController {

    private final JobProcessorService jobProcessorService;

    @Autowired
    public IndexController(JobProcessorService jobProcessorService) {
        this.jobProcessorService = jobProcessorService;
    }

    @GetMapping(path = "/")
    public ResponseEntity<?> index() {
        return ResponseEntity.ok(new Date().toString());
    }

    @PostMapping(path = "/download")
    public ResponseEntity<?> download(@RequestBody Job job) {
        log.info("Job request : {}", job);
        if (jobProcessorService.canDoAnotherJob()) {
            Mono.just(job)
                    .flatMap(jobProcessorService::processJob)
                    .doOnError(throwable -> log.error("Failed job: {}", throwable))
                    .doOnSuccess(j -> log.info("Job finished : {}", j))
                    .subscribe();
        } else {
            log.debug("Job processor service is busy doing job {}", jobProcessorService.getCurrentJobId());
        }

        return ResponseEntity.ok("done");
    }
}
