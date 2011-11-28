package com.mobilesorcery.sdk.deployment.internal.ui.ftp;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;
import com.mobilesorcery.sdk.profiles.ui.DeviceFilterComposite;

public class AssignProfilesPage extends WizardPage {

	private DeviceFilterComposite filterComposite;
	private IDeviceFilter filter = new CompositeDeviceFilter();

	protected AssignProfilesPage(String pageName, String title) {
		super(pageName, title, null);
	}

	public void setDeviceFilter(IDeviceFilter filter) {
		this.filter = filter == null ? new CompositeDeviceFilter() : filter;
	}

	public IDeviceFilter getDeviceFilter() {
		return filter;
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		filterComposite = new DeviceFilterComposite(main, SWT.NONE);

		//filterComposite.setCurrentProject(filter);
		filterComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(main);
		throw new UnsupportedOperationException("NOT UPDATED WITH THE NOW PROFILE MANAGER CONCEPT");
	}

}
