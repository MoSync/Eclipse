/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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
