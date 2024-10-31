package com.adobe.aem.support.contentbackflow.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import com.adobe.aem.support.contentbackflow.entity.ContentSetSize;
import com.adobe.aem.support.contentbackflow.jobs.ContentSetCountConsumer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = Servlet.class, immediate = true)
@HttpWhiteboardServletPattern(value = "/cbf/contentset/size")
@HttpWhiteboardContextSelect(value = ServletContext.CONTEXT_SELECTOR)
public class CreateContentSetSizeJobServlet extends HttpServlet {

    @Reference
    private JobManager jobManager;

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) {
        try (PrintWriter writer = response.getWriter()) {
            ContentSetSize input = getPayload(request);
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

    public ContentSetSize getPayload(HttpServletRequest request) throws IOException {
        String payload = IOUtils.toString(request.getInputStream(), java.nio.charset.Charset.defaultCharset());
        Gson gson = new Gson();
        return gson.fromJson(payload, ContentSetSize.class);
    }
}
