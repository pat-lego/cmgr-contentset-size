package com.adobe.aem.support.contentbackflow.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import com.adobe.aem.support.contentbackflow.entity.ContentSetResult;
import com.adobe.aem.support.contentbackflow.utils.HttpHelper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = Servlet.class, immediate = true)
@HttpWhiteboardServletPattern(value = "/cbf/contentset/sizes")
@HttpWhiteboardContextSelect(value = ServletContext.CONTEXT_SELECTOR)
public class GetContentSetJobsServlet extends HttpServlet {

  @Reference
  private JobManager jobManager;

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  private final Integer LIMIT = 10;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try (PrintWriter writer = response.getWriter();
        ResourceResolver resourceResolver = this.resourceResolverFactory
            .getServiceResourceResolver(
                Map.of(ResourceResolverFactory.SUBSERVICE, "support-contentbackflow"))) {
      Integer limit = getLimit(request);
      Session session = resourceResolver.adaptTo(Session.class);
      Collection<ContentSetResult> contentSetResults = getJob(limit, session);
      Gson gson = new Gson();
      String result = gson.toJson(contentSetResults);
      response.setContentType("application/json");
      writer.write(result);
      response.setStatus(HttpServletResponse.SC_OK);
      writer.flush();
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

  public Integer getLimit(HttpServletRequest request) {
    String limit = new HttpHelper(request.getQueryString()).getQueryStringByKey("limit");
    if (limit == null) {
      return LIMIT;
    }

    try {
      int l = Integer.parseInt(limit);
      if (l < 0) {
        return LIMIT;
      }
      return l;
    } catch (Exception e) {
      log.warn("Failed to parse limit value {} dfaulting to {}", limit, LIMIT);
      return LIMIT;
    }
    
  }

  public NodeIterator executeQuery(int limit, Session session) throws InvalidQueryException, RepositoryException {
    String query = "select * from [nt:unstructured] as a where isdescendantnode(a, \"/var/support/contentbackflow\") order by [id] option(index tag supportcbf limit "
        + limit + ")";
    QueryResult result = session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute();
    return result.getNodes();
  }

  public List<ContentSetResult> getJob(int limit, Session session)
      throws InvalidQueryException, RepositoryException {
    NodeIterator it = executeQuery(limit, session);
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
