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

import org.eclipse.core.resources.IFile;

/**
 * <p>An interface that maps arbitrary objects to 
 * a corresponding path.</p>
 * <p>(Similar to org.eclipse.ui.editors.text but returns a workspace-relative
 * <code>IFile</code> instead.)</p>
 * @author mattias.bybro@purplescout.se
 *
 */
public interface ILocationProvider {

	public IFile getLocation(Object element);
	
}
