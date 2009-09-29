package com.mobilesorcery.sdk.core.templates;

import java.io.IOException;
import java.util.Map;

/**
 * In lieu of the sparsely documented eclipse templates...
 * @author Mattias
 *
 */
public interface ITemplate {

    /**
     * Returns the user-friendly name of this template
     * @return
     */
    String getDescription();
    
    /**
     * Returns the id of this template
     */
    String getId();
    
    /**
     * Returns a document with resolved parameters
     * @param map The map containing key-value pairs
     * to be inserted instead of <i>${key}</i> in the document.
     * The map may be null
     * 
     */
    String resolve(Map<String, String> map) throws IOException;

    /**
     * Returns the (user-friendly) name of this template.
     * @return
     */
    String getName();
    
    /**
     * Returns the project-relative path of the resolved template
     * (eg "main.cpp")
     * @return
     */
    String getPathToResolvedTemplate();

    Map<String, String> getSettings();       
}
