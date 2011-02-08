package com.mobilesorcery.sdk.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.BuildVariant;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncProjectParameterResolver;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.MoSyncProject.IConverter;

public class MoSyncProjectConverter1_2 implements MoSyncProject.IConverter {

	public final static Version VERSION = new Version("1.2");
	
	private static IConverter instance = new MoSyncProjectConverter1_2();

	public static MoSyncProject.IConverter getInstance() {
		return instance;
	}
	
	@Override
	public MoSyncProject convert(MoSyncProject project) throws CoreException {
    	try {
    		Version projectVersion = project.getFormatVersion();
	    	if (VERSION.isNewer(projectVersion)) {
	    		// Default libs are now newlib so we need to convert
	    		convertToNewlib(project, project, null);
	    		Set<String> cfgs = project.getBuildConfigurations();
	    		for (String cfg : cfgs) {
	    			convertToNewlib(project, project.getBuildConfiguration(cfg).getProperties(), cfg);
	    		}
	    		project.setFormatVersion(VERSION);
	    	}
			return project;
    	} catch (ParameterResolverException e) {
    		throw ParameterResolverException.toCoreException(e);
    	}
	}

    private void convertToNewlib(MoSyncProject project, IPropertyOwner properties, String id) throws ParameterResolverException {
    	// TODO: What about removing the default include, lib & lib paths soon?
    	// We just replace default paths with whatever it was at that time...
    	if (!PropertyUtil.getBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS)) {
    		prependPaths(project, id, MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS, new IPath[] { new Path("%mosync-home%/include") });
    	}
		
		if (!PropertyUtil.getBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES)) {
        	IPath stdLib = IBuildConfiguration.DEBUG_ID.equals(id) ? new Path("mastdD.lib") : new Path("mastd.lib");
    		prependPaths(project, id, MoSyncBuilder.ADDITIONAL_LIBRARIES, new IPath[] { stdLib });        	
    	}
		
		if (!PropertyUtil.getBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS)) {
    		prependPaths(project, id, MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS, new IPath[] { new Path("%mosync-home%/lib/pipe") });    		
    	}
		
		// ...and then make sure to ignore default paths
		PropertyUtil.setBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS, true);
		PropertyUtil.setBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES, true);
		PropertyUtil.setBoolean(properties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS, true);
    }

	private void prependPaths(MoSyncProject project, String cfgId, String key, IPath[] prepended) throws ParameterResolverException {
		IBuildConfiguration cfg = project.getBuildConfiguration(cfgId);
		IPropertyOwner properties = cfg == null ? project : cfg.getProperties();
		IPath[] paths = PropertyUtil.getPaths(properties, key);
		IPath[] newPaths = new IPath[prepended.length + paths.length];
		System.arraycopy(prepended, 0, newPaths, 0, prepended.length);
		System.arraycopy(paths, 0, newPaths, prepended.length, paths.length);
		newPaths = removeDuplicates(project, cfg, newPaths);
		PropertyUtil.setPaths(properties, key, newPaths);
	}

	private IPath[] removeDuplicates(MoSyncProject project, IBuildConfiguration cfg, IPath[] paths) throws ParameterResolverException {
		HashSet<String> resolvedPaths = new HashSet<String>();
		ArrayList<IPath> result = new ArrayList<IPath>();
		// We'll assume the current target profile (we do not use it anyway so no problemo)
		BuildVariant variant = new BuildVariant(null, cfg, false);
		ParameterResolver resolver = MoSyncProjectParameterResolver.create(project, variant);
		for (int i = 0; i < paths.length; i++) {
		     String resolvedPath = Util.replace(paths[i].toOSString(), resolver);
		     if (!resolvedPaths.contains(resolvedPath)) {
		    	 resolvedPaths.add(resolvedPath);
		    	 result.add(paths[i]);
		     }
		}
		return result.toArray(new IPath[result.size()]);
	}

}
