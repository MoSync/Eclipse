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
package com.mobilesorcery.sdk.internal.dependencies;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.internal.builder.MoSyncBuilderVisitor;

/**
 * A dependency provider that uses the GCC makefiles produced
 * by the <code>-MMD</code> switch.
 *  
 * @author Mattias Bybro
 *
 */
public class GCCDependencyProvider implements IDependencyProvider<IResource> {

	private static final Map<IResource, Collection<IResource>> EMPTY = new HashMap<IResource, Collection<IResource>>();
	
	private MoSyncBuilderVisitor builder;

	/**
	 * 
	 * @param moSyncBuilderVisitor 
	 * @param mmdFile The gcc dependency file to scan for dependencies
	 */
	public GCCDependencyProvider(MoSyncBuilderVisitor builder) {
		this.builder = builder;
	}
	
	public Map<IResource, Collection<IResource>> computeDependenciesOf(IResource resource) throws CoreException {
		if (MoSyncBuilderVisitor.getCFile(resource, false) == null) {
			return EMPTY;
		}
		
		IPath output = null;
		
		if (resource.getType() == IResource.FILE) {
			output = builder.mapFileToOutput((IFile) resource);
		}
		
		try {
			GCCDependencyFileParser parser = new GCCDependencyFileParser();
			File depsFile = getMMDFile(output);
			if (depsFile != null && depsFile.exists()) {
				parser.parse(depsFile);
			}
			
			// Explicitly add the .c -> .s dependency
			IResource outputResource = GCCDependencyFileParser.getFile(output.toOSString());
			if (outputResource != null) {
				parser.getDependencies().put(resource, Arrays.asList(outputResource));
			}
			return parser.getDependencies();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
		}
	}

	private File getMMDFile(IPath objFile) {
		if (objFile == null) {
			return null;
		}
		
		String depsFileName = MoSyncBuilderVisitor.mapToDependencyFile(objFile.toOSString());
		return new File(depsFileName);
	}

}
