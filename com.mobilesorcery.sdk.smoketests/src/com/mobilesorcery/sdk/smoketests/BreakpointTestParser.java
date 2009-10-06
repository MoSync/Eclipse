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
package com.mobilesorcery.sdk.smoketests;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.model.ICBreakpointType;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class BreakpointTestParser {

	public int parse(InputStream cFile, IFile cFileInProject) throws IOException, CoreException {
		LineNumberReader cReader = new LineNumberReader(new InputStreamReader(cFile));
		int bpCount = 0;
		for (String line = cReader.readLine(); line != null; line = cReader.readLine()) {
			if (line.contains("//BREAKPOINT")) {
				int lineNumber = cReader.getLineNumber();
				CDIDebugModel.createLineBreakpoint(cFileInProject.getLocation().toOSString(), cFileInProject, ICBreakpointType.REGULAR, lineNumber, true, 0, null, true);
				bpCount++;
			}			
		}
		
		return bpCount;
	}
}
