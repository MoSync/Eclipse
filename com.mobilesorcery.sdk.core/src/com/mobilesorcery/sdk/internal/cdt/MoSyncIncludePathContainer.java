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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IMacroEntry;
import org.eclipse.cdt.core.model.IOutputEntry;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.model.IPathEntryContainer;
import org.eclipse.cdt.core.model.ISourceEntry;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolverException;
import com.mobilesorcery.sdk.core.Util;

public class MoSyncIncludePathContainer implements IPathEntryContainer {

	public final static IPath CONTAINER_ID = new Path("com.mobilesorcery.mosync.includepaths");

    private final IProject project;

    public MoSyncIncludePathContainer(IProject project) {
        this.project = project;
    }

    @Override
	public String getDescription() {
        return "MoSync Include Path";
    }

    @Override
	public IPath getPath() {
        return CONTAINER_ID;
    }

    @Override
	public IPathEntry[] getPathEntries() {
        List<IPathEntry> entries = new ArrayList<IPathEntry>();
        MoSyncProject project = MoSyncProject.create(this.project);
        if (project != null) {
            IBuildVariant variant = MoSyncBuilder.getActiveVariant(project);
        	try {
        		IPath[] includePaths = MoSyncBuilder.getBaseIncludePaths(project, variant);
	        	for (int i = 0; i < includePaths.length; i++) {
	        		IContainer[] includePathInWorkspace = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(includePaths[i]);
	        		IPath resourcePath = includePathInWorkspace.length > 0 ? includePathInWorkspace[0].getProjectRelativePath() : Path.EMPTY;
	        		entries.add(CoreModel.newIncludeEntry(resourcePath, Path.EMPTY, includePaths[i], true));
	        	}
			} catch (ParameterResolverException e) {
				// TODO: Error marker?
				CoreMoSyncPlugin.getDefault().log(e);
			}
        }
        entries.addAll(Arrays.asList(createCompilerSymbols()));
        entries.addAll(createOutputEntries(project.getWrappedProject()));

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
    	ArrayList<IMacroEntry> compilerSymbols = new ArrayList<IMacroEntry>();
    	MoSyncProject mosyncProject = MoSyncProject.create(project);
    	if (mosyncProject == null) {
    		throw new IllegalStateException("Project does not have MoSync Nature");
    	}
    	Map<String, String> defines = MoSyncBuilder.extractMacroDefinesFromGCCArgs(mosyncProject, MoSyncBuilder.getActiveVariant(mosyncProject));
    	for (Map.Entry<String, String> define : defines.entrySet()) {
			IMacroEntry macroEntry = CoreModel.newMacroEntry(Path.EMPTY, define.getKey(), define.getValue());
			compilerSymbols.add(macroEntry);
    	}
    	return compilerSymbols;
    }

    private static List<IOutputEntry> createOutputEntries(IProject project) {
    	// This one seems to be a bit prototype-ish, feel free to rip it out and replace it with something useful
    	// Note that the original intent of this was to make sure that the indexer does not index the large
    	// generated XCode files for the iPhone. HOWEVER, I've left this approach and started using resource filters instead...
    	IFolder outputPath = project.getFolder(new Path(MoSyncBuilder.OUTPUT));

    	IPath[] exclusionPattern = new IPath[] { new Path("rebuild.build.cpp") };

    	List<IOutputEntry> result = new ArrayList<IOutputEntry>();
    	addAsOutputEntryIfExists(result, outputPath, exclusionPattern);

    	return result;
    }


	private static void addAsOutputEntryIfExists(List<IOutputEntry> result,
			IFolder sourceFolder, IPath[] exclusionPattern) {
		if (sourceFolder.exists()) {
    		result.add(CoreModel.newOutputEntry(sourceFolder.getFullPath(), exclusionPattern));
    	}
	}
}
