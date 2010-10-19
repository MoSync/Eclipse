package com.mobilesorcery.sdk.profiling.ui.views;

import java.util.IdentityHashMap;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.profiling.IInvocation;

public class InvocationViewerFilter extends ViewerFilter {

	private IFilter<IInvocation> filter;

	private boolean recursive;
	
	public InvocationViewerFilter(IFilter<IInvocation> filter, boolean recursive) {
		setDelegatingFilter(filter);
		this.recursive = recursive;
	}
	
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IInvocation) {
			IInvocation invocation = (IInvocation) element;
			boolean result = element != null && (filter == null || filter.accept(invocation));
			if (recursive && !result) {
				for (IInvocation child : invocation.getInvocations()) {
					if (select(viewer, element, child)) {
						return true;
					}
				}
			} else {
				return result;
			}
		}
		
		return false;
	}

	public void setDelegatingFilter(IFilter<IInvocation> filter) {
		this.filter = filter;
	}
}
