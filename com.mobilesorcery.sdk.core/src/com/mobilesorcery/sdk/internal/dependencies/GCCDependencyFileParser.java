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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class GCCDependencyFileParser {

	private static final int IN_DEPFILE = 0;
	private static final int IN_DEP_LIST = 1;

	private Map<IResource, Collection<IResource>> dependencies = new HashMap<IResource, Collection<IResource>>();
	private IResource currentDependentFile;
	private ArrayList<IResource> currentDependencies;

	private String projectRoot;

	public GCCDependencyFileParser() {

	}

	public void parse(File mmdFile) throws IOException {
		FileInputStream mmdFileInputStream = new FileInputStream(mmdFile);
		try {
			IResource fileInWs = getFile(mmdFile.getAbsolutePath());
			String projectRoot = fileInWs == null ? "" : fileInWs.getProject().getLocation().toOSString();
			parse(projectRoot, mmdFileInputStream);
		} finally {
			mmdFileInputStream.close();
		}
	}

	public void parse(String projectRoot, InputStream input) throws IOException {
		this.projectRoot = projectRoot;
		
		LineNumberReader lines = new LineNumberReader(new InputStreamReader(
				input));
		StringBuffer compoundLine = new StringBuffer();
		for (String line = lines.readLine(); line != null; line = lines
				.readLine()) {
			int continuationIx = line.lastIndexOf('\\');
			boolean endsWithSlash = line.trim().endsWith("\\");
			if (continuationIx != -1 && endsWithSlash) {
				compoundLine.append(line.substring(0, continuationIx));
			} else {
				compoundLine.append(line);
				parseLine(compoundLine);
				compoundLine = new StringBuffer();
			}
		}

		// Just in case; should never happen, though.
		if (compoundLine.length() > 0) {
			parseLine(compoundLine);
		}
	}

	private void parseLine(StringBuffer line) {
		char[] lineAr = new char[line.length()];
		line.getChars(0, line.length(), lineAr, 0);
		StringBuffer depFile = new StringBuffer();
		int pos = -1;
		int state = IN_DEPFILE;
		while (pos < lineAr.length - 1) {
			pos++;
			boolean doAppend = true;
			char ch = lineAr[pos];
			char peek = pos == lineAr.length - 1 ? '\0' : lineAr[pos + 1];
			char prev = pos == 0 ? '\0' : lineAr[pos - 1];

			boolean potentialEscapeState = ch == '\\';
			boolean isInEscapeState = prev == '\\' && ch == ' ';

			switch (state) {
			case IN_DEPFILE:
				if (ch == ':') {
					// Is this the full name? A colon followed by a space, or
					// an existing file -> full name!
					if (peek == ' ' || new File(depFile.toString()).exists()) {
						currentDependentFile = getFile(depFile);
						if (currentDependentFile != null) {
							currentDependencies = new ArrayList<IResource>();
							dependencies.put(currentDependentFile,
									currentDependencies);
						}
						depFile.delete(0, depFile.length());
						doAppend = false;
						state = IN_DEP_LIST;
					}
				} else {
					doAppend = !potentialEscapeState || peek != ' ';
				}

				break;

			case IN_DEP_LIST:
				doAppend = !potentialEscapeState || peek != ' ';
				
				if (!isInEscapeState && ch == ' ' && currentDependencies != null) {
					addCurrentFileToDependencies(depFile);
					doAppend = false;
				}
			}
			
			boolean unappendableCh = ch == '\t';
			
			if (doAppend && !unappendableCh) {
				depFile.append(ch);
			}
		}
		
		addCurrentFileToDependencies(depFile);
		
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("GCC dependency parsing:\n{0}", dependencies);
		}
		currentDependentFile = null;
		currentDependencies = null;
	}

	private void addCurrentFileToDependencies(StringBuffer depFile) {
		if (currentDependencies != null && depFile != null && depFile.length() > 0) {
			IResource file = getFile(depFile);
			if (file != null) {
				currentDependencies.add(file);
			}
			depFile.delete(0, depFile.length());
		}
	}

	public IResource getFile(StringBuffer filename) {
		String filenameStr = filename.toString();
		String absFilenameStr = new File(projectRoot, filenameStr).getAbsolutePath(); 

		IResource result = getFile(filenameStr);
		return result == null ? getFile(absFilenameStr) : result;
	}
	
	public static IResource getFile(String filename) {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IFile[] files = ws.getRoot().findFilesForLocation(new Path(filename.toString().trim()));
        if (files != null && files.length > 0) {
        	return files[0];
        }

		return null;
	}

	public Map<IResource, Collection<IResource>> getDependencies() {
		return dependencies;
	}

}
