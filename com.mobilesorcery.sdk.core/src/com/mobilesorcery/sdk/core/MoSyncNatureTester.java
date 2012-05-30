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
package com.mobilesorcery.sdk.core;

import org.eclipse.core.expressions.PropertyTester;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class MoSyncNatureTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return extractProject(receiver, property, args, expectedValue) != null;
	}
	
	/**
	 * Tries to derive a mosync project from whatever object is passed
	 * as the <code>receiver</code>; this method will accept <code>List</code>s,
	 * <code>IAdaptable</code>s, <code>IResource</code>s, and then if project
	 * associated with these is compatible with a MoSyncProject, return that project.
	 * 
	 * @param receiver
	 * @param property
	 * @param args
	 * @param expectedValue
	 * @return
	 * @see MoSyncProject#isCompatible
	 */
	protected MoSyncProject extractProject(Object receiver, String property, Object[] args, Object expectedValue) {
		return CoreMoSyncPlugin.getDefault().extractProject(receiver);
    }

}
