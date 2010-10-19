package com.mobilesorcery.sdk.ui;

public class CollapseAllAction extends ExpandAllAction {

	public CollapseAllAction() {
		setImageDescriptor(MosyncUIPlugin.getDefault().getImageRegistry().getDescriptor(MosyncUIPlugin.COLLAPSE_ALL));
	}
	
	public void run() {
		viewer.collapseAll();
	}
}
