package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

public class ExpandAllAction extends Action {

	protected TreeViewer viewer;

	public ExpandAllAction() {
		setImageDescriptor(MosyncUIPlugin.getDefault().getImageRegistry().getDescriptor(MosyncUIPlugin.EXPAND_ALL));
		setEnabled(false);
	}
	
	public void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}
	
	public void run() {
		if (viewer != null) {
			viewer.expandAll();
		}
	}
}
