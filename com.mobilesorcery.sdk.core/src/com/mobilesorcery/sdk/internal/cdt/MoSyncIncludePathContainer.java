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
package com.mobilesorcery.sdk.internal.cdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IMacroEntry;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.model.IPathEntryContainer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

public class MoSyncIncludePathContainer implements IPathEntryContainer {

	public final static IPath CONTAINER_ID = new Path("com.mobilesorcery.mosync.includepaths");
	
    private IProject project;

    public MoSyncIncludePathContainer(IProject project) {
        this.project = project;
    }
    
    public String getDescription() {
        return "MoSync Include Path";
    }

    public IPath getPath() {
        return CONTAINER_ID;
    }

    public IPathEntry[] getPathEntries() {
        List<IPathEntry> entries = new ArrayList<IPathEntry>();
        MoSyncProject project = MoSyncProject.create(this.project);
        if (project != null) {
            IBuildVariant variant = MoSyncBuilder.getActiveVariant(project, false);
        	IPath[] includePaths = MoSyncBuilder.getBaseIncludePaths(project, variant);
        	for (int i = 0; i < includePaths.length; i++) {
        		IContainer[] includePathInWorkspace = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(includePaths[i]);
        		IPath resourcePath = includePathInWorkspace.length > 0 ? includePathInWorkspace[0].getProjectRelativePath() : Path.EMPTY;
        		entries.add(CoreModel.newIncludeEntry(resourcePath, Path.EMPTY, includePaths[i], true));
        	}
        }
        entries.addAll(Arrays.asList(createCompilerSymbols()));
        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
        	CoreMoSyncPlugin.trace(entries);
        }
        return entries.toArray(new IPathEntry[entries.size()]);
    }
    
    private IMacroEntry[] createCompilerSymbols() {
    	ArrayList<IMacroEntry> compilerSymbols = new ArrayList<IMacroEntry>(createPredefinedCompilerSymbols());
    	compilerSymbols.addAll(extractCompilerSymbolsFromGCCArgs(project));
    	return compilerSymbols.toArray(new IMacroEntry[0]);
    }
    
    private List<IMacroEntry> createPredefinedCompilerSymbols() {
    	return Arrays.asList(new IMacroEntry[] { CoreModel.newMacroEntry(Path.EMPTY, "__GNUC__", ""), 
    			CoreModel.newMacroEntry(Path.EMPTY, "MAPIP", "") });    	
    }

    /**
     * <p>Given a project with mosync nature, extracts all user defined -Darg=value
     * from the project's <i>extra</i> gcc command line arguments, and converts
     * them into a set of <code>IMacroEntry</code>s.</p> 
     * <p>Refactoring note: this method could (should?) be moved</p> 
     * @return
     */
    public static List<IMacroEntry> extractCompilerSymbolsFromGCCArgs(IProject project) {
    	MoSyncProject mosyncProject = MoSyncProject.create(project);
    	if (mosyncProject == null) {
    		throw new IllegalStateException("Project does not have MoSync Nature");
    	}
    	String extraCompilerSwitchesLine = mosyncProject.getPropertyOwner().getProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES);
        ArrayList<IMacroEntry> compilerSymbols = new ArrayList<IMacroEntry>();
        
        if (!Util.isEmpty(extraCompilerSwitchesLine)) {
            String[] extraCompilerSwitches = Util.parseCommandLine(extraCompilerSwitchesLine);	
        	
        	for (int i = 0; i < extraCompilerSwitches.length; i++) {
        		String extraCompilerSwitch = extraCompilerSwitches[i];
        		if (extraCompilerSwitch.startsWith("-D") && extraCompilerSwitch.length() > 2) {
        			String trimmedExtraCompilerSwitch = extraCompilerSwitch.substring(2);
        			String[] keyAndValue = trimmedExtraCompilerSwitch.split("=", 2);
        			String key = keyAndValue[0];
        			String value = keyAndValue.length > 1 ? keyAndValue[1] : "";
        			IMacroEntry macroEntry = CoreModel.newMacroEntry(Path.EMPTY, key, value);
        			compilerSymbols.add(macroEntry);
        		}
        	}
        }
    	
    	return compilerSymbols;
    }
}
