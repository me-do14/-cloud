package com.adobe.training.core.servlets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.util.HashMap;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.day.cq.tagging.InvalidTagFormatException;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.api.WCMException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This Servlet take an input parameter in the form of a string or CSV:
 * 
 * New Page Path, Page Title, Template, Page Tag
 *
 * And outputs AEM page(s) created. 
 * 
 * To test, you must create a content node with a resourceType=training/tools/pagecreator
 * 
 * /content/pagecreator {sling:resourceType=training/tools/pagecreator}
 *
 * Example cURL Command:
 * String Example:
 * $ curl -u admin:admin -X POST http://localhost:4502/content/pagecreator.json -F importer="/content/training/us/en/community,Our Community,/conf/training/settings/wcm/templates/page-content,/content/cq:tags/training/community"
 * 
 * CSV example:
 * $ curl -u admin:admin -X POST http://localhost:4502/content/pagecreator.json -F importer=@PageCreator.csv
 */

@Component(service = { Servlet.class })
@SlingServletResourceTypes(
		resourceTypes="training/tools/pagecreator",
		methods=HttpConstants.METHOD_POST)
public class PageCreator extends SlingAllMethodsServlet {
	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_TEMPLATE_PATH = "/conf/training/settings/wcm/templates/page-content";

	//Get a PageManager instance from the factory Service
	@Reference private PageManagerFactory pageManagerFactory;

	@Override
	public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)throws IOException {
		response.setContentType("application/json");
		HashMap<String, HashMap<String, String>> resultObject = new HashMap<>();

		String param = request.getParameter("importer");
		byte[] input = param.getBytes(StandardCharsets.UTF_8);
		InputStream stream = new ByteArrayInputStream(input);
		try {
			resultObject = readInput(request, stream);
		} catch (IOException e) {
			resultObject = null;
		}

		ObjectMapper objMapper = new ObjectMapper();
		if (resultObject != null) {		
			//Write the result to the page
			response.getWriter().print(objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultObject));
		} else {
			HashMap<String, String> errorObject = new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
			{
				put("Error","Could not read csv input");
			}};
			response.getWriter().print(objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorObject));
		}
		response.getWriter().close();
	}

	/**
	 * Reads the input. The input MUST be in the form of:
	 * <p>
	 * JCR path, Page Title, Page Template, AEM Tag
	 *
	 * @param request resourceResolver will be derived from the request
	 * @param stream Stream from the input
	 * @return JSON object that contains the results of the page creation process
	 */
	private HashMap<String, HashMap<String, String>> readInput(SlingHttpServletRequest request, InputStream stream) throws IOException {
		HashMap<String, HashMap<String, String>> out = new HashMap<>();

		String line;
		String[] newPage;
		HashMap<String, String> createdPageObject = null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
			//Read each line of the CSV and if the input is a string, this will loop once
			while ((line = br.readLine()) != null) {
				newPage = line.split(",");
				String inputPath="", inputTitle="", inputTemplatePath="", inputTag="";				
	
				//set values from input
				if (newPage.length > 0) 	
					inputPath = newPage[0]; //Add the creation path
				if (newPage.length > 1) 
					inputTitle = newPage[1]; //Add the Title of the new page
				if (newPage.length > 2) 
					inputTemplatePath = newPage[2]; //Add the desired template
				if (newPage.length > 3)
					inputTag = newPage[3]; //Add requested tags
				
				//Create a new page based on a line of input
				createdPageObject = createTrainingPage(request, inputPath, inputTitle, inputTemplatePath, inputTag);
	
				//add the status of the row into the json array
				if(createdPageObject.get("Status").contentEquals("Successful")) {
					out.put(inputPath, createdPageObject);
				} else {
					out.put(line, createdPageObject);
				}
				createdPageObject = null;
			}
		}
		return out;
	}

	/** Helper method to create the page based on available input
	 * 
	 * @param request resourceResolver will be derived from the request
	 * @param path JCR location of the page to be created
	 * @param title Page Title
	 * @param template AEM Template this page should be created from. The template must exist in the JCR already.
	 * @param tag Tag must already be created in AEM. The tag will be in the form of a path. Ex /content/cq:tags/marketing/interest
	 * @return HashMap
	 */
	private HashMap<String, String> createTrainingPage(SlingHttpServletRequest request, String path, String title, String template, String tagPath) {
		HashMap<String, String> pageInfo = new HashMap<>();
		pageInfo.put("Status","Error");
		
		if (path != null && !path.isEmpty()) {
			//Parse the path to get the pageNodeName and parentPath
			int lastSlash = path.lastIndexOf("/");
			String pageNodeName = path.substring(lastSlash + 1);
			String parentPath = path.substring(0, lastSlash);
			
			//Set a default template if none is given
			boolean invalidTemplate = false;
			if (template == null || template.isEmpty()) { //if no template has been given, assign the default
				template = DEFAULT_TEMPLATE_PATH;
			}
			else if(request.getResourceResolver().getResource(template) == null) { //check to see if the template exists
				invalidTemplate = true;
				pageInfo.put("Template","The template " + template + " doesn not exist.");
			}
	
			//Create page
			PageManager pageManager = pageManagerFactory.getPageManager(request.getResourceResolver());
	
			Page p = null;
			//Verify parentPath exists, a node for this page exists, and the template is valid
			if (pageManager != null && !parentPath.isEmpty() && !pageNodeName.isEmpty() && !invalidTemplate) {
				if(title == null || title.isEmpty()) {
					pageInfo.put("Warning","No Page title given, using path name: " + pageNodeName);
					title = pageNodeName;
				}
				try {
					p = pageManager.create(parentPath,
							pageNodeName,
							template,
							title);
				} catch (WCMException e) {
					pageInfo.put("Error","Page couldn't be created. Parent path probably doesn't exist.");
				}
					
				//Check to see if the page was successfully created
				if(p != null) {
					//Add a tag to the page
					if (tagPath != null && !tagPath.isEmpty()) {
						//Make sure tag namespaces are properly formed
						if(tagPath.contains("/content/cq:tags") || tagPath.contains(":") || tagPath.contains("/etc/tags")) {
							//TagManager can be retrieved via adaptTo
							TagManager tm = request.getResourceResolver().adaptTo(TagManager.class);
							Tag tag;
							try {
								tag = tm.resolve(tagPath); //check if tag already exists
								if(tag == null) {
									pageInfo.put("Warning","Tag doesn't exist, creating new tag: " + tagPath);
									tag = tm.createTag(tagPath, null, null);
								}
								tm.setTags(p.getContentResource(), new Tag[] {tag}, true);
							} catch (AccessControlException e) {
								pageInfo.put("Warning","Could not access the tags.");
							} catch (InvalidTagFormatException e) {
								pageInfo.put("Warning","Invalid Tag.");
							} 
						} else {
							pageInfo.put("Warning", "Tag path malformed and not added: " + tagPath);
						}
					}
					
					pageInfo.put("Status", "Successful");
					pageInfo.put("Location", p.getPath());
					pageInfo.put("Title", p.getTitle());
					pageInfo.put("Template Used", p.getTemplate().getPath());
					String tags = "";
					for(Tag t : p.getTags() ) { tags = tags + t.getTitle() + " "; }
					pageInfo.put("Tagged with", tags);
				}
			}
		} else {
			pageInfo.put("Error", "Page path not provided");
		}
		return pageInfo;
	}
}