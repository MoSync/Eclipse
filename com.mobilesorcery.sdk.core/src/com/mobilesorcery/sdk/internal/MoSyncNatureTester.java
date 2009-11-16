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
package com.mobilesorcery.sdk.internal;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class MoSyncNatureTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return extractProject(receiver, property, args, expectedValue) != null;
	}
	
	protected MoSyncProject extractProject(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof IAdaptable) {
            receiver = ((IAdaptable) receiver).getAdapter(IResource.class);             
        }
        
        if (receiver instanceof List) {
            if (((List)(receiver)).size() == 0) {
                return null;
            }
            
            return extractProject(((List)receiver).get(0), property, args, expectedValue);
        }
        
        if(receiver == null) {
            return null;
        }
        
        if (receiver instanceof IResource) {
            IProject project = ((IResource)receiver).getProject();

            try {                
                return MoSyncNature.isCompatible(project) ? MoSyncProject.create(project) : null;
            } catch (CoreException e) {
                return null;
            }
        }
        
        return null;
    }

}
