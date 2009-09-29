package com.mobilesorcery.sdk.internal;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import com.mobilesorcery.sdk.core.MoSyncNature;

public class MoSyncNatureTester extends PropertyTester {

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof IAdaptable) {
            receiver = ((IAdaptable) receiver).getAdapter(IResource.class);             
        }
        
        if (receiver instanceof List) {
            if (((List)(receiver)).size() == 0) {
                return false;
            }
            
            return test(((List)receiver).get(0), property, args, expectedValue);
        }
        
        if(receiver == null) {
            return false;
        }
        
        if (receiver instanceof IResource) {
            IProject project = ((IResource)receiver).getProject();

            try {                
                return MoSyncNature.isCompatible(project);
            } catch (CoreException e) {
                return false;
            }
        }
        
        return false;
    }

}
