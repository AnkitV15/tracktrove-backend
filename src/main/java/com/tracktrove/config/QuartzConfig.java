package com.tracktrove.config;

import com.tracktrove.scheduler.SettlementJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail settlementJobDetail() {
        return JobBuilder.newJob(SettlementJob.class)
                .withIdentity("settlementJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger settlementTrigger(JobDetail settlementJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(settlementJobDetail)
                .withIdentity("settlementTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")) // Every 5 mins
                .build();
    }
}


