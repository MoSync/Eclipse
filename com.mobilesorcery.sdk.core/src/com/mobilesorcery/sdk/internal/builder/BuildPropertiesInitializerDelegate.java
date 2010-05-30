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
package com.mobilesorcery.sdk.internal.builder;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.internal.PipeTool;

public class BuildPropertiesInitializerDelegate implements IPropertyInitializerDelegate {

    public BuildPropertiesInitializerDelegate() {
    }

    public String getDefaultValue(IPropertyOwner p, String key) {
    	// TODO: Some of these should be set in an initialization step instead,
    	// lo prio though.
    	
    	String namespacedKey = NameSpacePropertyOwner.getKey(key);
    	String namespace = NameSpacePropertyOwner.getFullNamespace(key);
    	String namespaceSegment = "";
    	String projectName = "";
    	
    	if (p instanceof MoSyncProject) {
    		MoSyncProject project = (MoSyncProject) p;
    		projectName = project.getName();
    		// Config = namespace
    		namespaceSegment = project.areBuildConfigurationsSupported() ? namespace + "/" : "";
    	}
    	
        if (MoSyncBuilder.EXTRA_COMPILER_SWITCHES.equals(namespacedKey)) {
        	if (IBuildConfiguration.DEBUG_ID.equals(namespace)) {
        		return "-O0";
        	} else {
        		return "-O2";
        	}
        } else if (MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS.equals(namespacedKey)) {
        	if (IBuildConfiguration.DEBUG_ID.equals(namespace)) {
        		return PropertyUtil.fromBoolean(true); 
        	} else {
        		return PropertyUtil.fromBoolean(false);
        	}
        } else if (MoSyncBuilder.LIB_OUTPUT_PATH.equals(namespacedKey) && namespaceSegment != null) {
    		return namespaceSegment + projectName + ".lib";          	
        } else if (MoSyncBuilder.APP_OUTPUT_PATH.equals(namespacedKey) && namespaceSegment != null) {
    		return namespaceSegment;
        } else if (MoSyncBuilder.DEFAULT_LIBRARIES.equals(namespacedKey)) {
        	if (IBuildConfiguration.DEBUG_ID.equals(namespace)) {
        		return PropertyUtil.fromPaths(new IPath[] { new Path("mastdD.lib") });
        	} else {
        		return PropertyUtil.fromPaths(new IPath[] { new Path("mastd.lib") });
        	}
        } else if (MoSyncBuilder.MEMORY_HEAPSIZE_KB.equals(namespacedKey)) {
        	return PropertyUtil.fromInteger(PipeTool.DEFAULT_HEAP_SIZE_KB);
        } else if (MoSyncBuilder.MEMORY_STACKSIZE_KB.equals(namespacedKey)) {
        	return PropertyUtil.fromInteger(PipeTool.DEFAULT_STACK_SIZE_KB);        	
        } else if (MoSyncBuilder.MEMORY_DATASIZE_KB.equals(namespacedKey)) {
        	return PropertyUtil.fromInteger(PipeTool.DEFAULT_DATA_SIZE_KB);
        } else if (DefaultPackager.APP_VENDOR_NAME_BUILD_PROP.equals(namespacedKey)) {
            return "Built with MoSync SDK";
        } else if (MoSyncBuilder.PROJECT_VERSION.equals(namespacedKey)) {
            return "1.0";
        } else if (MoSyncBuilder.APP_NAME.equals(namespacedKey)) {
            return projectName;
        }
        
        return null;
    }

}
