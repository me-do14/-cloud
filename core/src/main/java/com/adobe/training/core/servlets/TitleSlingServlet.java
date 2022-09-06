package com.adobe.training.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;


/**
 * TitleSlingServlet can use resourceType, selector, method, and extension to bind to URLs
 * 
 * Example URL: http://localhost:4502/content/training/us/en.html
 * Example URL with selector: http://localhost:4502/content/training/us/en.foobar.html
 * 
 */
@Component(service = { Servlet.class })
@SlingServletResourceTypes(
        resourceTypes=TitleSlingServlet.RESOURCE_TYPE,
        selectors="foobar",
        extensions="html")
public class TitleSlingServlet extends SlingSafeMethodsServlet {
	private static final long serialVersionUID = 1L;
	
	protected static final String RESOURCE_TYPE = "training/components/title";
	
	//Get a PageManager instance from the factory Service
	@Reference private PageManagerFactory pageManagerFactory;

	@Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		//Use pageManagerFactory to get page manager from the request.resourceResolver
		PageManager pm = pageManagerFactory.getPageManager(request.getResourceResolver());
		//Use the PageManager to find the containing page of the resource (component)
    	Page curPage = pm.getContainingPage(request.getResource());
    	
		//Verify the page exists and it is a site page and not an XF
    	if(curPage != null && !curPage.getName().equals("master")) {
			String responseStr = "";
			response.setHeader("Content-Type", "text/html");
			responseStr = "<h1>Sling Servlet injected this title on the " + curPage.getName() + " page.</h1>";
			response.getWriter().print(responseStr);
			response.getWriter().close();
		}
    }   
}
