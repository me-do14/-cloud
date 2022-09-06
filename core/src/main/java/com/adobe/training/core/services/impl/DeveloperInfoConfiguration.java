package com.adobe.training.core.services.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Training DeveloperInfo Config")
public @interface DeveloperInfoConfiguration {

    @AttributeDefinition(
            name = "Show Info",
            description = "Should the Developer information be shown?",
            type = AttributeType.BOOLEAN
    )
    boolean developerinfo_showinfo() default false;

    @AttributeDefinition(
            name = "Name",
            description = "Name of the Developer",
            type = AttributeType.STRING
    )
    String developerinfo_name() default "";

    @AttributeDefinition(
            name = "Hobbies",
            description = "List your favorite Hobbies",
            type = AttributeType.STRING
    )
    String[] developerinfo_hobbies() default {"swimming", "climbing"};

    @AttributeDefinition(
            name = "Language",
            description = "Favorite Language Preference",
            options = {
                    @Option(label = "HTL", value = "HTL"),
                    @Option(label = "Java", value = "Java"),
                    @Option(label = "JSP", value = "JSP"),
                    @Option(label = "HTML", value = "HTML"),
                    @Option(label = "JavaScript", value = "JavaScript")
            }
    )
    String developerinfo_language() default "";
}