package com.adobe.aem.support.contentbackflow.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import com.adobe.aem.support.contentbackflow.entity.ContentSetJob;
import com.adobe.aem.support.contentbackflow.entity.ContentSetInput;
import com.adobe.aem.support.contentbackflow.jobs.ContentSetCountConsumer;
import com.adobe.aem.support.contentbackflow.utils.HttpHelper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service =  Servlet.class, immediate = true )
@HttpWhiteboardServletPattern(value = "/cbf/contentset/sizes")
@HttpWhiteboardContextSelect(value = ServletContext.CONTEXT_SELECTOR)
public class GetContentSetJobsServlet extends HttpServlet {

  @Reference
  private JobManager jobManager;

  private final int LIMIT = 10;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try (PrintWriter writer = response.getWriter()) {
      int limit = getLimit(request);
      Collection<Job> jobs = jobManager.findJobs(QueryType.ALL, ContentSetCountConsumer.TOPIC, limit, null);
      log.debug("Retrieved {} jobs with topic {}", jobs.size(), ContentSetCountConsumer.TOPIC);
      List<ContentSetJob> contentSetJobs = new LinkedList<ContentSetJob>();
      for (Job job : jobs) {
        ContentSetJob contentSetJob = new ContentSetJob();
        contentSetJob.setJobId(job.getId());
        contentSetJob.setStatus(job.getJobState().name());
        contentSetJob.setInput((ContentSetInput) job.getProperty("input"));
        contentSetJobs.add(contentSetJob);
      }
      Gson gson = new Gson();
      String result = gson.toJson(contentSetJobs);
      response.setContentType("application/json");
      writer.write(result);
      response.setStatus(HttpServletResponse.SC_OK);
      writer.flush();
    } 
  }

  public int getLimit(HttpServletRequest request) {
    return new HttpHelper(request.getQueryString()).getQueryStringByKey("limit", LIMIT);
  }

}
