package com.beepsoft.hasura.actions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

// For now ActionsFilter must be in Java. When in Kotlin the filter was not picked up.
@Component
public class ActionsFilter implements Filter {

    @Value("${hasuraconf.action-controller.path:/actions}")
    String path;

    @Value("${hasuraconf.action-controller.enabled:false}")
    boolean enabled;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        if (enabled && req.getRequestURI().equals(path)) {
            ActionRequestWrapper wrappedReq = new ActionRequestWrapper(req);
            filterChain.doFilter(wrappedReq, servletResponse);
        }
        else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}

/* !!!!!! Doing the same in Kotlin doesn't work, the ActionsFilter is not found by the scan and is not initialized.
          That's why we have a java Filter class instead

@Component
class ActionsFilter : javax.servlet.Filter {
    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig) {
        println("ActionsFilter.init")
    }

    override fun destroy() {}

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        println("ActionsFilter doFilter")
    }
}

 */
