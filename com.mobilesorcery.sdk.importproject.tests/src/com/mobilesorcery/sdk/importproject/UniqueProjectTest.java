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
package com.mobilesorcery.sdk.importproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class UniqueProjectTest {

	@Test
	public void testUniqueName() throws CoreException {
		IProject project = createUniqueProject("test");
		assertEquals("test", project.getName());
		
		IProject newProject = createUniqueProject("test");		
		assertEquals("test2", newProject.getName());
	}


	@Test
	public void testUniqueNameCaseSensitive() throws CoreException {
		IProject project = createProject("test-TEST");
		MosyncUIPlugin.createProject(project, null, new NullProgressMonitor());		
		
		IProject newProject = createUniqueProject("test-test");
		System.err.println(newProject.getName());
		assertTrue("test-test2".equalsIgnoreCase(newProject.getName()));
	}
	
	private IProject createUniqueProject(String name) throws CoreException {
		ImportProjectsRunnable r = new ImportProjectsRunnable(new File[0], ImportProjectsRunnable.COPY_ALL_FILES);
		IProject newProject = r.createProjectWithUniqueName(name, null, new NullProgressMonitor());
		return newProject;
	}

	private IProject createProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}


}
