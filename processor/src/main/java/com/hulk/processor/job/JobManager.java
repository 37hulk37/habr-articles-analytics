package com.hulk.processor.job;

import com.hulk.processor.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobManager {

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final Job job;

    public JobExecution startJob(String jobName, LocalDate startTime) {
        var parameters = new JobParametersBuilder()
            .addString("id", UUID.randomUUID().toString())
            .addString("name", jobName)
            .addLocalDate("createdAt", startTime)
            .toJobParameters();

        try {
            return jobOperator.start(job, parameters);
        } catch (JobExecutionException e) {
           throw new ApplicationException("Can not run job with parameters:" + parameters, e);
        }
    }

    public Set<JobExecution> getJobs(String jobName) {
        return jobRepository.findRunningJobExecutions(jobName);
    }

    public List<String> getJobNames() {
        return List.of(job.getName());
    }

}
