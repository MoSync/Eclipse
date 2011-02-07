package com.mobilesorcery.sdk.core;

import java.util.Set;

import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.core.MoSyncProject.IConverter;

public class MoSyncProjectConverter1_2 implements MoSyncProject.IConverter {

	private static IConverter instance = new MoSyncProjectConverter1_2();

	public static MoSyncProject.IConverter getInstance() {
		return instance;
	}
	
	@Override
	public MoSyncProject convert(MoSyncProject project) {
    	Version projectVersion = project.getFormatVersion();
    	if (MoSyncProject.CURRENT_VERSION.isNewer(projectVersion)) {
    		// Default libs are now newlib so we need to convert
    		//convertToNewlib(project);
    		Set<String> cfgs = project.getBuildConfigurations();
    		for (String cfg : cfgs) {
    			//convertToNewlib(project.getBuildConfiguration(cfg).getProperties());
    		}
    	}
		return project;
	}

    private void convertToNewlib(IPropertyOwner properties) {
    	// TODO: What about removing the default include, lib & lib paths soon?
    	// We just replace default paths with whatever it was at that time...
    	if (!PropertyUtil.getBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS)) {
    		//prependPaths(properties, MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS, new IPath[] { "%mosync-"});
    	}
		
		if (!PropertyUtil.getBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES)) {
    		
    	}
		
		if (!PropertyUtil.getBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS)) {
    		
    	}
		
		// ...and then make sure to ignore default paths
		PropertyUtil.setBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS, true);
		PropertyUtil.setBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES, true);
		PropertyUtil.setBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS, true);
    }

	private void prependPaths(IPropertyOwner properties, String key, IPath[] prepended) {
		IPath[] paths = PropertyUtil.getPaths(properties, key);
		IPath[] newPaths = new IPath[prepended.length + paths.length];
		System.arraycopy(prepended, 0, newPaths, 0, prepended.length);
		System.arraycopy(paths, 0, newPaths, prepended.length, paths.length);
		PropertyUtil.setPaths(properties, key, newPaths);
	}

}
