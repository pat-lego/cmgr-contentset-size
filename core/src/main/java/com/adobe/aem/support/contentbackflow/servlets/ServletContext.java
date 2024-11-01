package com.adobe.aem.support.contentbackflow.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.AuthenticationSupport;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;

@Component(service = ServletContextHelper.class, property = {
        "osgi.http.whiteboard.context.name=" + ServletContext.CONTEXT_NAME,
        "osgi.http.whiteboard.context.path=" + ServletContext.CONTEXT_PATH
})
public class ServletContext extends ServletContextHelper {

    @Reference
    private AuthenticationSupport authenticationSupport;

    public static final String CONTEXT_NAME = "com.adobe.aem.support.contentbackflow";
    public static final String CONTEXT_SELECTOR = "(osgi.http.whiteboard.context.name=" + CONTEXT_NAME + ")";
    public static final String CONTEXT_PATH = "/api/adobe/support";

    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
                return authenticationSupport.handleSecurity(request, response);
    }
}
