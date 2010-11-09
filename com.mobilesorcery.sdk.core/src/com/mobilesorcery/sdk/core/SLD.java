/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.internal.SLDInfoImpl;
import com.mobilesorcery.sdk.internal.SLDParser;

/**
 * A class for handling a single the state of SLD data;
 * each build configuration typically have an SLD.
 * 
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class SLD {

    public static final String NO_CACHE_SLD_KEY = "no.cache.sld";

	private MoSyncProject project;

	private long lastSLDTimestamp;

	private SLDInfoImpl lastSLD;

	private IPath sldFile;

	public SLD(MoSyncProject project, IPath sldFile) {
		this.project = project;
		this.sldFile = sldFile;
	}
	
	/**
	 * <p>Parses the SLD of this project; equivalent to
	 * <code>parseSLD(false)</code></p>
	 * @return
	 */
    public ISLDInfo parseSLD() {
    	return parseSLD(false);
    }
    
    /**
     * Parses the SLD of this project if the SLD
     * file has a newer time stamp than when last parsed,
     * or if <code>force</code> is set to <code>true</code>.
     * If the project property defined by NO_CACHE_SLD_KEY is
     * set to <code>true</code>, parsing will always take place.
     * @param force
     * @return
     */
    public synchronized ISLDInfo parseSLD(boolean force) {
        IPath sld = getSLDPath();
        if (!sld.toFile().exists()) {
            return null;
        }
        
        boolean dontCache = Boolean.parseBoolean(project.getProperty(NO_CACHE_SLD_KEY));
        boolean alwaysParse = force || dontCache; 
        
        SLDInfoImpl result = dontCache ? null : lastSLD;
        
        long currentSLDTimestamp = sld.toFile().lastModified();
        boolean timestampChanged = lastSLDTimestamp != currentSLDTimestamp;
        boolean hasNoCached = lastSLD == null;
        
        boolean doParse = alwaysParse || hasNoCached || timestampChanged;
        
        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
        	if (doParse) {
        		CoreMoSyncPlugin.trace("Parsing SLD: force = {0}, nocache = {1}, dirty = {2}", alwaysParse, hasNoCached, timestampChanged);
        	}
        }
        
        if (doParse) {
	        SLDParser parser = new SLDParser();
	        try {
	            parser.parse(sld.toFile());
	            result = parser.getSLD();
	            if (!dontCache) { 
	            	lastSLD = result;
	            	lastSLDTimestamp = currentSLDTimestamp; 
	            }
	        } catch (IOException e) {
	            // Ignore.
	        	e.printStackTrace();
	            CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not parse SLD file", e));
	        }
        }
        
        return result;
    }

    /**
     * <p>Returns the path to the SLD file.</p>
     * <p>The SLD file maps addresses to files and line numbers.</p> 
     * @return
     */
    public IPath getSLDPath() {
    	return sldFile;       
    }
    
    /**
     * Utility method for parsing SDL info files
     * @param input
     * @param enc The character encoding to use, or <code>null</code> for
     * the default encoding
     * @return
     * @throws IOException 
     */
    public static ISLDInfo parseSLDInfo(InputStream input, String enc) throws IOException {
    	if (enc == null) {
    		enc = "UTF-8";
    	}
    	InputStreamReader reader = new InputStreamReader(input, enc);
    	SLDParser parser = new SLDParser();
    	parser.parse(reader);
    	return parser.getSLD();
    }
	
}
