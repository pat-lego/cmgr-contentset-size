package com.adobe.aem.support.contentbackflow.jobs;

import java.time.Year;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.Job.JobState;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.aem.support.contentbackflow.entity.ContentSetInput;

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
            log.error("Failed to authenticate with subservice, result will not be persisted in the repository", e);
            return JobResult.FAILED;
        } catch (PersistenceException | RepositoryException e) {
            log.error("Failed to persist the result, result will not be persisted in the repository", e);
            return JobResult.FAILED;
        }
    }

    private void persistResult(Job job, ResourceResolver resourceResolver, long total)
            throws PersistenceException, RepositoryException {
        Session session = resourceResolver.adaptTo(Session.class);
        String path = guaranteeExistance(session);
        persistResult(job, session, total, path);
        resourceResolver.commit();
    }

    private String guaranteeExistance(Session session)
            throws PersistenceException, RepositoryException {
        int year = Year.now().getValue();
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        String path = completed + "/" + year + "/" + month + "/" + day;
        return JcrUtils.getOrCreateByPath(path, "sling:Folder", session).getPath();
    }

    private void persistResult(Job job, Session session, long total, String path) throws PathNotFoundException, RepositoryException {
        ContentSetInput input = (ContentSetInput) job.getProperty("input");
        Node result = JcrUtils.getOrAddNode(session.getNode(path), java.util.UUID.randomUUID().toString(), "nt:unstructured");
        result.setProperty("paths", input.getPaths().toArray(String[]::new));
        result.setProperty("total", total);
        result.setProperty("id", job.getId());
        result.setProperty("status", JobState.SUCCEEDED.name());
        result.setProperty("completed", Calendar.getInstance());
        result.addMixin("sling:HierarchyNode");
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
