package com.mobilesorcery.sdk.html5.live;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.mobilesorcery.sdk.html5.debug.JSODDSupport;

public class ReloadManager implements IResourceChangeListener {

	public ReloadManager() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public void dispose() {
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		final HashSet<IProject> projectsToReload = new HashSet<IProject>();
		if (delta != null) {
			try {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						if (resource != null && (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
							IProject project = resource.getProject();
							if (project != null && resource.getType() == IResource.FILE) {
								projectsToReload.add(resource.getProject());
							}
							//try {
								if (resource.getType() == IResource.FILE) {
									//op.rewrite(resource.getFullPath());
								}
							/*} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}*/
						}
						return true;
					}
				});
			} catch (CoreException e) {
				// Ignore.
				e.printStackTrace();
			}
		}
		try {
			for (IProject projectToReload : projectsToReload) {
				reload(projectToReload);
			}
		} catch (IOException e) {
			// Ignore? Put in status line?
			e.printStackTrace();
		}
	}

	public void reload(IProject projectToReload) throws IOException {
		URL reloadServerURL = new URL("http", "localhost", 8282, "/" + projectToReload.getName() + "/LocalFiles.html");
		// Just ping & close. TODO: Seperate thread, please.
		// Activate if we ever want to support Reload in the IDE.
		/*
		URLConnection connection = reloadServerURL.openConnection();
		connection.connect();
		connection.getInputStream().close();*/
	}
}
