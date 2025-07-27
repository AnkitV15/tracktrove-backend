package com.tracktrove.config;

import com.tracktrove.job.RetryJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryJobConfig {

    @Bean
    public JobDetail retryJobDetail() {
        return JobBuilder.newJob(RetryJob.class)
            .withIdentity("retryJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger retryJobTrigger(JobDetail retryJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(retryJobDetail)
            .withIdentity("retryTrigger")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(1)
                .repeatForever())
            .build();
    }
}
