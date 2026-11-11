package com.hulk.processor.job;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobManager jobManager;

    @PostMapping
    public JobInfo startJob(@RequestBody JobRequest request) {
        var execution = jobManager.startJob(request.jobName(), LocalDate.now());

        return new JobInfo(execution);
    }

    @GetMapping("/names")
    public List<String> getJobNames() {
        return jobManager.getJobNames();
    }

    public record JobRequest(
        String jobName
    ) {
    }

}
