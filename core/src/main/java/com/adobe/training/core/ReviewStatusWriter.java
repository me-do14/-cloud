package com.adobe.training.core;

import java.util.List;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.workflow.PayloadMap;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.commons.jcr.JcrConstants;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This workflow process writes the asset review status. If the user has added in
 * a workflow comment, it is added to the review status comment.
 * 
 * /content/dam/<payload-node>/jcr:content/metadata
 *  + dam:status = approved | rejected | changesRequested
 *  + dam:statusComment = "string here"
 */
@Component(service = WorkflowProcess.class,
		   property = {Constants.SERVICE_DESCRIPTION + "=Workflow process to set Asset Review Status",
				       Constants.SERVICE_VENDOR + "=Adobe",
				       "process.label=Set Asset Review Status"
		   })
public class ReviewStatusWriter implements WorkflowProcess {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	// Convenience string to find the log messages for this training example class
	// Logs can be found in crx-quickstart/logs/error.log
	private String searchableLogStr = "@@@@@";

	private static final String REVIEW_APPROVED = "approved";
	private static final String REVIEW_REJECTED = "rejected";
	private static final String REVIEW_CHANGES_REQUESTED = "changesRequested";

	@Override
	public void execute(WorkItem item, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
		
		// Get the status review status to set in the process arguments.
		String assetReviewStatusArg = readArgument(args); 
		if(assetReviewStatusArg != null && !assetReviewStatusArg.isEmpty()) {
			
			WorkflowData workflowData = item.getWorkflowData();
			if (workflowData.getPayloadType().equals(PayloadMap.TYPE_JCR_PATH)) {
				String contentPath = workflowData.getPayload().toString(); //content path attached to this workflow
				try (ResourceResolver rr = workflowSession.adaptTo(ResourceResolver.class)){
					
					Resource resource = rr.getResource(contentPath);

					//Verify that the content attached to this workflow is an asset
					Asset asset = resource.adaptTo(Asset.class);
					if (asset != null){
						// Get a modifieable object for the metadata attached to this asset
						String metadataPath = JcrConstants.JCR_CONTENT + "/metadata"; // jcr:content/metadata
						ModifiableValueMap map = asset.getChild(metadataPath).adaptTo(ModifiableValueMap.class);
						
						//Update the asset review status property based on the processs arguments
						logger.info(searchableLogStr + "Updating asset review status to: " + assetReviewStatusArg);
						map.put("dam:status", assetReviewStatusArg);
						
						// If there was a comment in the last completed workflow step, add it to the asset
						List<HistoryItem> historyList = workflowSession.getHistory(item.getWorkflow());
						String lastComment = getLastStepComment(historyList);
						if(lastComment != null){
							if(!lastComment.isEmpty()){
								map.put("dam:statusComment", lastComment);
								logger.info("Updating asset review status comment to: " + lastComment);
							} else {
								map.remove("dam:statusComment");
								logger.info("Removing asset review status comment");
							}
						}
						rr.commit(); // Save all changes
					}
					else {
						// If the payload is not an AEM asset, throw an workflow error
						throw new WorkflowException(searchableLogStr + " The path: " + contentPath + " is not an asset.");
					}
				}
				catch (PersistenceException e ) {
					throw new WorkflowException(e.getMessage(), e);
				}
			} 
		} else {
			// If the process arguments are not valid to set the review status, throw an error
			throw new WorkflowException(searchableLogStr + " Process argument is not a valid, check the process step in the workflow. Valid arguments are [" 
			+ REVIEW_APPROVED +" | " + REVIEW_REJECTED + " | " + REVIEW_CHANGES_REQUESTED + "]");
		}
	}

	/**
	 * This method takes in the history of an active workflow and returns the comment of the last completed step.
	 * @param historyList List of previous workitems in the workflow
	 * @return The comment from the last workitem or an empty string if there no comment.
	 */
	private String getLastStepComment(List<HistoryItem> historyList) {
        int listSize = historyList.size();
        HistoryItem lastItem = historyList.get(listSize-1);
        String comment = lastItem.getComment();
        if(comment != null && comment.length() > 0) {
            return comment;
        } 
		return "";
	}

	/**
	 * This method reads the arguments that were added into the workflow step. 
	 * To set these argument go into the workflow and find the Process step > Process tab > Arguments
	 * @param args These optional arguments added to the process step
	 * @return returns the correct normalized value for the asset review status if it was set
	 */
	private static String readArgument(MetaDataMap args) {
		String argument = args.get("PROCESS_ARGS", "");
		if(argument.equalsIgnoreCase(REVIEW_APPROVED)){
			return REVIEW_APPROVED;
		} else if(argument.equalsIgnoreCase(REVIEW_REJECTED)){
			return REVIEW_REJECTED;
		} else if(argument.equalsIgnoreCase(REVIEW_CHANGES_REQUESTED)){
			return REVIEW_CHANGES_REQUESTED;
		}
		return "";
	}
}