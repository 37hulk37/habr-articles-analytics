package com.hulk.processor.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;

public record JobInfo(
    long executionId,
    BatchStatus status,
    String exitCode
) {

    public JobInfo(JobExecution jobExecution) {
        this(
            jobExecution.getId(),
            jobExecution.getStatus(),
            jobExecution.getExitStatus().getExitCode()
        );
    }
}
