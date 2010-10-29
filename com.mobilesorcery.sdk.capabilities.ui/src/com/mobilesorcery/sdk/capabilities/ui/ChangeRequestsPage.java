package com.mobilesorcery.sdk.capabilities.ui;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.mobilesorcery.sdk.capabilities.core.AbstractChangeRequest;
import com.mobilesorcery.sdk.capabilities.core.CompoundChangeRequest;
import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;
import com.mobilesorcery.sdk.capabilities.ui.ChangeRequestsPage.ChangeRequestContentProvider;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class ChangeRequestsPage extends WizardPage {

	public class ChangeRequestCheckStateProvider implements ICheckStateProvider {

		private Map<IProject, IChangeRequest> changeRequests;

		public ChangeRequestCheckStateProvider(Map<IProject, IChangeRequest> changeRequests) {
			this.changeRequests = changeRequests;
		}
		
		public boolean isChecked(Object element) {
			if (element instanceof IProject) {
				IChangeRequest changeRequest = changeRequests.get(element);
				return isChecked(changeRequest);
			} else if (element instanceof CompoundChangeRequest) {
				CompoundChangeRequest compoundChangeRequest = (CompoundChangeRequest) element;
				for (IChangeRequest changeRequest : compoundChangeRequest.getChangeRequests()) {
					if (!isChecked(changeRequest)) {
						return false;
					}
					return true;
				}
			} else if (element instanceof IChangeRequest) {
				IChangeRequest changeRequest = (IChangeRequest) element;
				MoSyncProject project = changeRequest.getProject();
				CompoundChangeRequest changeRequestParent = (CompoundChangeRequest) changeRequests.get(project.getWrappedProject());
				return changeRequestParent != null && changeRequestParent.shouldApply(changeRequest);
			}
			
			return false;
		}

		public boolean isGrayed(Object element) {
			if (element instanceof IProject) {
				IChangeRequest changeRequest = changeRequests.get(element);
				return isGrayed(changeRequest);
			} else if (element instanceof CompoundChangeRequest) {
				CompoundChangeRequest compoundChangeRequest = (CompoundChangeRequest) element;
				IChangeRequest[] changeRequests = compoundChangeRequest.getChangeRequests();
				int checkedCount = 0;
				for (IChangeRequest changeRequest : changeRequests) {
					if (isChecked(changeRequest)) {
						checkedCount++;
					}
				}
				return checkedCount > 0 && changeRequests.length > checkedCount;
			}
			return false;
		}

	}

	public class ChangeRequestLabelProvider extends LabelProvider {
		public String getText(Object element) {
			if (element instanceof IProject) {
				return MessageFormat.format("Project: {0}", ((IProject) element).getName());
			}
			
			return super.getText(element);
		}
	}
	
	public class ChangeRequestContentProvider implements ITreeContentProvider {

		private Map<IProject, IChangeRequest> input;

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.input = (Map<IProject, IChangeRequest>) newInput;
		}

		public Object[] getElements(Object inputElement) {
			return input.keySet().toArray();
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IProject) {
				return getChangeRequests(input.get(parentElement));
			} else if (parentElement instanceof CompoundChangeRequest) {
				return getChangeRequests((IChangeRequest) parentElement);
			}
			return new Object[0];
		}

		private Object[] getChangeRequests(IChangeRequest changeRequest) {
			if (changeRequest instanceof CompoundChangeRequest) {
				return ((CompoundChangeRequest) changeRequest).getChangeRequests();
			} else {
				return new Object[] { changeRequest };
			}
		}

		public Object getParent(Object element) {
			return (element instanceof AbstractChangeRequest) ? 
					((AbstractChangeRequest) element).getParent():
					null;
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
		
	}

	private Map<IProject, IChangeRequest> changeRequests;

	protected ChangeRequestsPage(Map<IProject, IChangeRequest> changeRequests) {
		super("change.requests", "Change Project Properties", null);
		this.changeRequests = changeRequests;
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		Label desc = new Label(main, SWT.NONE);
		desc.setText("Select the changes to apply to the project(s)");
		final CheckboxTreeViewer changeRequestsTree = new CheckboxTreeViewer(main);
		changeRequestsTree.setLabelProvider(new ChangeRequestLabelProvider());
		final ChangeRequestContentProvider contentProvider = new ChangeRequestContentProvider();
		changeRequestsTree.setContentProvider(contentProvider);
		changeRequestsTree.setCheckStateProvider(new ChangeRequestCheckStateProvider(changeRequests));
		changeRequestsTree.setInput(changeRequests);
		changeRequestsTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object parent = contentProvider.getParent(event.getElement());
				if (parent instanceof CompoundChangeRequest) {
					CompoundChangeRequest changeRequest = (CompoundChangeRequest) parent;
					changeRequest.setShouldApply((IChangeRequest) event.getElement(), event.getChecked());
					changeRequestsTree.refresh(true);
				}
			}
		});
		changeRequestsTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		setControl(main);
	}

}
