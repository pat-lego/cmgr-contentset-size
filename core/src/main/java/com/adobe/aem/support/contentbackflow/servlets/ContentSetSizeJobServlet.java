package com.adobe.aem.support.contentbackflow.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import com.adobe.aem.support.contentbackflow.entity.ContentSetInput;
import com.adobe.aem.support.contentbackflow.entity.ContentSetResult;
import com.adobe.aem.support.contentbackflow.jobs.ContentSetCountConsumer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = Servlet.class, immediate = true)
@HttpWhiteboardServletPattern(value = "/cbf/contentset/size/*")
@HttpWhiteboardContextSelect(value = ServletContext.CONTEXT_SELECTOR)
public class ContentSetSizeJobServlet extends HttpServlet {

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) {
        try (PrintWriter writer = response.getWriter()) {
            ContentSetInput input = getPayload(request);
            Map<String, Object> props = Map.of("input", input);
            Job job = jobManager.addJob(ContentSetCountConsumer.TOPIC, props);
            String jobId = job.getId();
            JsonObject result = new JsonObject();
            result.addProperty("jobId", jobId);
            writer.write(result.toString());
            writer.flush();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            log.error("Failed to serialize the inputstream to a pojo", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (PrintWriter writer = response.getWriter();
                ResourceResolver resourceResolver = this.resourceResolverFactory
                        .getServiceResourceResolver(
                                Map.of(ResourceResolverFactory.SUBSERVICE, "support-contentbackflow"))) {
            Session session = resourceResolver.adaptTo(Session.class);
            String jobId = getJobId(request.getRequestURI());
            log.debug("Retrieved jobId {}", jobId);
            if (jobId != null) {
                List<ContentSetResult> contentSetResults = getJob(jobId, session);
                Gson gson = new Gson();
                String result = gson.toJson(contentSetResults);
                response.setContentType("application/json");
                writer.write(result);
                response.setStatus(HttpServletResponse.SC_OK);
                writer.flush();
            } else {
                log.warn("Invalid jobId submitted");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }

        } catch (InvalidQueryException e) {
            log.error("Leveraged an invalid query to search the repository", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (RepositoryException e) {
            log.error("Failed to access the repository", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (LoginException e) {
            log.error("Failed authenticate against the repository with subservice user", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public ContentSetInput getPayload(HttpServletRequest request) throws IOException {
        String payload = IOUtils.toString(request.getInputStream(), java.nio.charset.Charset.defaultCharset());
        Gson gson = new Gson();
        return gson.fromJson(payload, ContentSetInput.class);
    }

    public String getJobId(String uri) {
        return uri.replace(ServletContext.CONTEXT_PATH + "/cbf/contentset/size/", "");
    }

    public NodeIterator executeQuery(String jobId, Session session) throws InvalidQueryException, RepositoryException {
        String query = "select * from [nt:unstructured] as a where isdescendantnode(a, \"/var/support/contentbackflow\") and a.[id] = \""
                + jobId + "\" option(index tag supportcbf)";
        QueryResult result = session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute();
        return result.getNodes();
    }

    public List<ContentSetResult> getJob(String jobId, Session session)
            throws InvalidQueryException, RepositoryException {
        NodeIterator it = executeQuery(jobId, session);
        List<ContentSetResult> contentSetResults = new LinkedList<ContentSetResult>();

        while (it.hasNext()) {
            Node node = it.nextNode();
            ContentSetResult contentSetJob = new ContentSetResult();
            contentSetJob.status = node.getProperty("status").getString();
            contentSetJob.completed = node.getProperty("completed").getDate();
            contentSetJob.total = node.getProperty("total").getLong();
            contentSetJob.paths = getPaths(node.getProperty("paths").getValues());
            contentSetResults.add(contentSetJob);
        }

        return contentSetResults;
    }

    public List<String> getPaths(Value[] value)
            throws ValueFormatException, IllegalStateException, RepositoryException {
        List<String> result = new LinkedList<String>();
        for (Value v : value) {
            result.add(v.getString());
        }
        return result;
    }
}
