package com.adobe.aem.support.contentbackflow.jobs;

import java.time.Month;
import java.time.Year;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.poi.hdgf.streams.Stream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.aem.support.contentbackflow.entity.ContentSetInput;
import com.adobe.internal.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = JobConsumer.class, property = {
        JobConsumer.PROPERTY_TOPICS + "=" + ContentSetCountConsumer.TOPIC
})
public class ContentSetCountConsumer implements JobConsumer {

    public static final String TOPIC = "aem/support/contentset/count";

    private final String completed = "/var/support/contentbackflow";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public JobResult process(Job job) {
        ContentSetInput contentSetInput = (ContentSetInput) job.getProperty("input");
        try (ResourceResolver resourceResolver = this.resourceResolverFactory
                .getServiceResourceResolver(Map.of(ResourceResolverFactory.SUBSERVICE, "support-contentbackflow"))) {
            long total = 0;
            for (String path : contentSetInput.paths) {
                long instance = 0;
                if (path.startsWith("/content")) {
                    instance = count(path, resourceResolver, instance);
                    log.debug("Completed content set on path {} and found {} nodes", path, instance);
                    total = total + instance;
                } else {
                    log.warn("Path {} does not start with content skipping", path);
                }
            }
            log.debug("Completed content set and found a total number of {} nodes", total);
            persistResult(job, resourceResolver, total);
            return JobResult.OK;
        } catch (LoginException e) {
            log.error("Failed to authenticate with subservice", e);
            return JobResult.FAILED;
        } catch (PersistenceException e) {
            log.error("Failed to persist the result", e);
            return JobResult.FAILED;
        }
    }

    private void persistResult(Job job, ResourceResolver resourceResolver, long total) throws PersistenceException {
        Resource path = guaranteeExistance(resourceResolver);
        String id = ResourceUtil.createUniqueChildName(path, UUID.createUUID().toString());
        ResourceUtil.getOrCreateResource(resourceResolver, path.getPath() + "/" + id, "nt:unstructured", "nt:unstructured", false);
        resourceResolver.commit();
    }

    private Resource guaranteeExistance(ResourceResolver resourceResolver) throws PersistenceException {
        int year = Year.now().getValue();
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        String path = completed + "/" + year + "/" + month + "/" + day;
        resourceResolver.refresh();
        return ResourceUtil.getOrCreateResource(resourceResolver, path, "sling:Folder", "sling:Folder", false);
    }

    private long count(String path, ResourceResolver resourceResolver, long total) {
        Resource resource = resourceResolver.getResource(path);
        if (!resource.hasChildren()) {
            return total;
        } else {
            Iterator it = resource.getChildren().iterator();
            while (it.hasNext()) {
                Resource child = (Resource) it.next(); 
                total = count(child.getPath(), resourceResolver, total + 1);
            }
        }

        return total;
    }
}
