package com.adobe.training.core.services;
/**
 * Service interface to get the information about the bundle Developer
 *
 * Example code can be inserted into the Helloworld HTL component:
 * /apps/training/components/content/helloworld/helloworld.html
 *
 * Example HTL:
 * <div data-sly-use.devInfo="com.adobe.training.core.services.DeveloperInfo">Developer Info: ${devInfo.DeveloperInfo}</div>
 *
 *
 */
public interface DeveloperInfo {
	public String getDeveloperInfo();
}
