package com.mobilesorcery.sdk.ui.internal.navigationext;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IsReleasePackageTester;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;

public class MoSyncNavigatorContentProvider implements ICommonContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IProject) {
				IResource[] members = ((IContainer) parentElement).members();
				for (IResource member : members) {
					if (member instanceof IContainer
							&& IsReleasePackageTester
									.isReleasePackageFolder(member)) {
						return new Object[] { new ReleasePackage(
								(IContainer) member) };
					}
				}
			} else if (parentElement instanceof ReleasePackage) {
				return ((ReleasePackage) parentElement).getUnderlyingResource()
						.members();
			}
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().logOnce(e, getClass().getName());
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof ReleasePackage);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	@Override
	public void restoreState(IMemento aMemento) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveState(IMemento aMemento) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		// TODO Auto-generated method stub

	}

}
