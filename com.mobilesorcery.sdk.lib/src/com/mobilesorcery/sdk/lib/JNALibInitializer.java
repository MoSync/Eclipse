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
package com.mobilesorcery.sdk.lib;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import com.sun.jna.Platform;

/**
 * Utility class for allowing JNA to be run on dll's within a plugin. The
 * <code>init</code> method of this class must be run before calling the JNA
 * <code>Native.loadLibrary</code> method.
 * 
 * @author Mattias, fmattias (modified by)
 */
public class JNALibInitializer
{
	/**
	 * Adds the given library to the path of JNA, so that it can 
	 * be found when calling Native.loadLibrary(). The library
	 * will only be searched for within the plugin.
	 * 
	 * @param bundle The bundle corresponding to this plugin.
	 * @param libname The name of the library to find. It is important
	 *                that this name contains the full name of the library without
	 *                the extension. E.g. if you want to load libsdl.so then 
	 *                'libsdl' should be passed to this function.
	 * @throws IOException will be thrown if the library could not be found.
	 */
    public static void init(Bundle bundle, String libname) throws IOException
    {
    	/* Get relative path to library */
        IPath pluginRelativePathToLib = new Path(libname + getLibExtension());
        
        /* Find library in plugin path */
        URL libInPluginLocation = FileLocator.find(bundle, pluginRelativePathToLib, null);
        URL libAsFileLocation = FileLocator.toFileURL(libInPluginLocation);
        
        /* Find absolute path of library */
        String libFilename = libAsFileLocation.getPath();
        String directoryOfLib = new File(libFilename).getParent();
        
        /* Ensure that JNA does not contain the library path already */
        String curLibPath = System.getProperty("jna.library.path", "");
        if(curLibPath.contains(directoryOfLib))
        {
        	return;
        }
        
        /* Add path to JNA library path so that JNA can find the library */
        if(!curLibPath.equals(""))
        {
        	curLibPath += File.pathSeparator + directoryOfLib;
        }
        else
        {
        	curLibPath = directoryOfLib;
        }
        
        System.setProperty("jna.library.path", curLibPath);
    }

    /**
     * Returns the default extension of libraries on
     * the given platform. 
     * 
     * @return the default extension of libraries on
     * the given platform. Null is returned if the 
     * current platform is not known.
     */
    private static String getLibExtension()
    {
    	if(Platform.isWindows() || Platform.isWindowsCE())
    	{
    		return ".dll";
    	}
    	else if(Platform.isLinux() || Platform.isOpenBSD() ||
    			Platform.isSolaris() || Platform.isFreeBSD())
    	{
    		return ".so";
    	}
    	else if(Platform.isMac())
    	{
    		return ".dylib";
    	}
    	else
    	{
    		return null;
    	}
    }    
}
