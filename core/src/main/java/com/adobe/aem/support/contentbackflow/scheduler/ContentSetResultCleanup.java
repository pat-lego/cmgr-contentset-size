package com.adobe.aem.support.contentbackflow.scheduler;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = Runnable.class, immediate = true)
public class ContentSetResultCleanup implements Runnable {

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    public ContentSetResultCleanup(@Reference Scheduler scheduler) {
        ScheduleOptions options = scheduler.EXPR("0 0 0/12 1/1 * ? *");
        options.canRunConcurrently(false);
        options.onLeaderOnly(true);
        scheduler.schedule(this, options);
    }

    @Override
    public void run() {
        try (ResourceResolver resourceResolver = this.resourceResolverFactory
                .getServiceResourceResolver(
                        Map.of(ResourceResolverFactory.SUBSERVICE, "support-contentbackflow"))) {
            Session session = resourceResolver.adaptTo(Session.class);
            boolean hasResults = true;
            while (hasResults) {
                log.debug("Running cleanup job");
                NodeIterator iterator = executeQuery(session);
                hasResults = false;
                while (iterator.hasNext()) {
                    log.debug("Found some results will assume there can be more");
                    hasResults = true;
                    session.removeItem(iterator.nextNode().getPath());
                }
                session.save();
            }
        } catch (LoginException e) {
            log.error("Unable to login and perform cleanup job", e);
        } catch (InvalidQueryException e) {
            log.error("Unable to perform cleanup job due to incorrect query", e);
        } catch (RepositoryException e) {
            log.error("Unable to perform cleanup job due to repository exception", e);
        }
    }

    public NodeIterator executeQuery(Session session) throws InvalidQueryException, RepositoryException {
        Date monthAgo = Date.from(ZonedDateTime.now().minusMonths(1).toInstant());
        SimpleDateFormat dateFromat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        String query = "select * from [sling:HierarchyNode] where ISDESCENDANTNODE(\"/var/support/contentbackflow\") and [completed] < CAST('"
                + dateFromat.format(monthAgo) + "' AS DATE) order by [id] option(index tag supportcbf limit 100)";

        return session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute().getNodes();
    }

}
