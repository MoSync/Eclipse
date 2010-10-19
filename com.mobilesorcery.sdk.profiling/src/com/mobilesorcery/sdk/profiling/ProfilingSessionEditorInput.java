package com.mobilesorcery.sdk.profiling;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class ProfilingSessionEditorInput implements IEditorInput {

	private IProfilingSession session;

	public ProfilingSessionEditorInput(IProfilingSession session) {
		this.session = session;
	}
	
	public boolean equals(Object other) {
		if (other instanceof ProfilingSessionEditorInput) {
			return getSession() == ((ProfilingSessionEditorInput) other).getSession();
		}
		return false;
	}
	
	public Object getAdapter(Class adapter) {
		if (IProfilingSession.class.equals(adapter)) {
			return session;
		}
		
		return null;
	}

	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return session.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return getName();
	}

	public IProfilingSession getSession() {
		return session;
	}

}
