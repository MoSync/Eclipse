package com.mobilesorcery.sdk.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.junit.Test;

public class NameSpacePropertyOwnerTest {

	@Test
	public void testNameSpace() throws Exception {
		MoSyncProject project = createTestProject("test2");
		NameSpacePropertyOwner child = new NameSpacePropertyOwner(project, "a/b");
		project.initProperty("p1", "v1");

		assertEquals("v1", child.getProperty("p1"));
		assertNull(project.getProperty("p1/a/b"));		
		
		child.setProperty("p1", "hi");
		assertEquals("hi", child.getProperty("p1"));
		assertEquals(project.getProperty("p1"), "v1");
		assertEquals(project.getProperty("p1/a/b"), "hi");
	}
	
	public static MoSyncProject createTestProject(String name) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		project.delete(true, null);
		IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(name);
		return createProject(project, location.toFile().toURI(), new NullProgressMonitor());
	}

	// Due to extremely strange plugin cycle problem I copied this from mosyncuiplugin
    public static MoSyncProject createProject(IProject project, URI location, IProgressMonitor monitor) throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProjectDescription description = workspace.newProjectDescription(project.getName());
        description.setLocationURI(location);

        CreateProjectOperation op = new CreateProjectOperation(description, "Create Project");
        try {
            op.execute(monitor, null);
        } catch (ExecutionException e) {
        	e.printStackTrace();
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
        }
        
        MoSyncProject.addNatureToProject(project);
        return MoSyncProject.create(project);
    }

}
