package com.hulk.processor.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobManager jobManager;

    @PostMapping
    public JobExecution startJob(@RequestBody JobRequest request) {
        return jobManager.startJob(request.jobName(), LocalDate.now());
    }

    @GetMapping("/{jobName}")
    public Set<JobExecution> getJobs(@PathVariable String jobName) {
        return jobManager.getJobs(jobName);
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
