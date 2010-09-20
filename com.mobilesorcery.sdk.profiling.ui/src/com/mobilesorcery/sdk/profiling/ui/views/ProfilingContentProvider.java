/**
 * 
 */
package com.mobilesorcery.sdk.profiling.ui.views;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.mobilesorcery.sdk.profiling.FunctionDesc;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.Invocation;

class ProfilingContentProvider implements ITreeContentProvider {

    private static final Object[] EMPTY = new Object[0];

    private boolean flat;

    public ProfilingContentProvider(boolean flat) {
        this.flat = flat;
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

    public Object[] getChildren(Object parentElement) {
        if (flat) {
            return EMPTY;
        } else {
            return ((IInvocation) parentElement).getInvocations().toArray();
        }
    }

    public Object getParent(Object element) {
        return ((IInvocation) element).getCaller();
    }

    public boolean hasChildren(Object element) {
        return getChildren(element).length != 0;
    }

    public Object[] getElements(Object inputElement) {
        IInvocation root = null;
        if (inputElement instanceof IProfilingSession) {
            root = ((IProfilingSession) inputElement).getInvocation();
        } else {
            root = (IInvocation) inputElement;
        }
        
        return flat ? aggregate(root.flatten(null)) : getChildren(root);
    }

    private IInvocation[] aggregate(Collection<IInvocation> invocations) {
        LinkedHashMap<FunctionDesc, IInvocation> result = new LinkedHashMap<FunctionDesc, IInvocation>();

        for (IInvocation invocation : invocations) {
            IInvocation aggregate = result.get(invocation.getProfiledEntity());
            if (aggregate == null) {
                result.put(invocation.getProfiledEntity(), invocation);
            } else {
                Invocation aggregated = Invocation.aggregate(aggregate, invocation);
                result.put(invocation.getProfiledEntity(), aggregated);
            }
        }

        return result.values().toArray(new IInvocation[0]);
    }
}