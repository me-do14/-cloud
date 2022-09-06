package com.adobe.training.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.slf4j.Logger;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.Title;
import com.day.cq.wcm.api.Page;

/**
 * This title model extends the core title model: com.adobe.cq.wcm.core.components.models.Title
 * The core title model is used in both v1 and v2 core title components: core/wcm/components/title
 * In this model, we extend the title model to include a subtitle.
 * 
 * In order to extend a core component model, in core/pom.xml add the dependency:
        <dependency>
 			<groupId>com.adobe.cq</groupId>
		    <artifactId>core.wcm.components.core</artifactId>
		</dependency>
 * 
 * Note: You don't need to add this dependency to the parent pom because it's already included.
 */

@Model(adaptables=SlingHttpServletRequest.class,
	adapters= {ComponentExporter.class},
	resourceType=TitleWithSubtitle.RESOURCE_TYPE,
	defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = "jackson", extensions = "json")
public class TitleWithSubtitle implements ComponentExporter{
	protected static final String RESOURCE_TYPE = "training/components/titlewithsubtitle";
	
	@Inject
    @Named("log")
    private Logger logger;
	
	@SlingObject
	private SlingHttpServletRequest request;

	//HTL global object in the model
	//Learn more  at Helpx > HTL Global Objects
	@ScriptVariable
	private Page currentPage;
	
	//property on the current resource saved from the dialog of a component
	@ValueMapValue
	private String subtitle;
	
	//Core Title model we are extending
	@Self @Via(type = ResourceSuperType.class)
	private Title coreTitle;
	
	//Method called when the model is initialized
	@PostConstruct
	protected void initModel(){
		//setup properties that are extending the title model
		if(subtitle == null){
			subtitle = "";
		}
	}
	//Next 2 methods required to support JSON display for this Component
	public String getTitle() {
		return coreTitle.getText();
	}
	
	public String getLinkURL() {
		return coreTitle.getLinkURL();
	}
	
	public String getSubtitle() {	
		return subtitle;
	}
	
	public boolean isEmpty() {
		//Verify there is a subtitle
		if(!subtitle.isEmpty()) {
			return false;
		}
		//Verify a title was entered from the dialog and
		//Page.title or Page.PageTitle are not being used 
		String uniqueTitle = coreTitle.getText();
		if (!uniqueTitle.equals(currentPage.getTitle())
			&& !uniqueTitle.equals(currentPage.getPageTitle())) {
			return false;
		}
		return true;
	}
	
	@Override
	public String getExportedType() {
		return request.getResource().getResourceType();
	}
}