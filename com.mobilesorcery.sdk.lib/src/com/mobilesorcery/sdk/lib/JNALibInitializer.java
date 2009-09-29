package com.mobilesorcery.sdk.lib;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;

import com.sun.jna.NativeLibrary;

/**
 * Utility class for allowing jna to be run on dll's within a plugin. The
 * <code>init</code> method of this class must be run before calling the JNA
 * <code>Native.loadLibrary</code> method.
 * 
 * @author Mattias
 * 
 */
public class JNALibInitializer {

    public static void init(Bundle bundle, IPath pluginRelativePathToLib) throws IOException {
        String libName = pluginRelativePathToLib.lastSegment();
        URL dllInPluginLocation = FileLocator.find(bundle, pluginRelativePathToLib, null);
        URL dllAsFileLocation = FileLocator.toFileURL(dllInPluginLocation);
        String dllFilename = dllAsFileLocation.getPath();
        String directoryOfDll = new File(dllFilename).getParent();
        String libAbbrName = libName.substring(0, libName.length() - ".dll".length()); //$NON-NLS-1$
        NativeLibrary.addSearchPath(libAbbrName, directoryOfDll);
        //System.err.println("ADDED SEARCH PATH " + directoryOfDll + " FOR " + libAbbrName);
    }

}
