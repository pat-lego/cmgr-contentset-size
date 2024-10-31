package com.adobe.aem.support.contentbackflow.jobs;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = JobConsumer.class, property = {
        JobConsumer.PROPERTY_TOPICS + "=" + ContentSetCountConsumer.TOPIC
})
public class ContentSetCountConsumer implements JobConsumer {

    public static final String TOPIC = "aem/support/contentset/count";

    @Override
    public JobResult process(Job job) {
        log.debug("Starting to process job for topic {}", TOPIC);
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            log.error("Could not sleep");
        }
        return JobResult.OK;
    }
}
