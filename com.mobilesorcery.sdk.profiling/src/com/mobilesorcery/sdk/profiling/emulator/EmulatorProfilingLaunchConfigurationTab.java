package com.mobilesorcery.sdk.profiling.emulator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ParseException;
import com.mobilesorcery.sdk.profiling.filter.NameFilter;
import com.mobilesorcery.sdk.profiling.filter.NameFilter.MatchType;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class EmulatorProfilingLaunchConfigurationTab extends AbstractLaunchConfigurationTab implements IUpdatableControl {
	
	private Text funcFilter;
	private Text fileFilter;
	private Button useRegExp;

	@Override
	public void createControl(Composite parent) {
		Group filterGroup = new Group(parent, SWT.NONE);
		filterGroup.setText("&Filters");
		filterGroup.setLayout(new GridLayout(2, false));
		Label funcFilterLabel = new Label(filterGroup, SWT.NONE);
		funcFilterLabel.setText("Name filters");
		funcFilter = new Text(filterGroup, SWT.SINGLE | SWT.BORDER);
		funcFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label fileFilterLabel = new Label(filterGroup, SWT.NONE);
		fileFilterLabel.setText("File filters");
		fileFilter = new Text(filterGroup, SWT.SINGLE | SWT.BORDER);
		fileFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		useRegExp = new Button(filterGroup, SWT.CHECK);
		useRegExp.setText("Use regular e&xpressions");
		
		UpdateListener updateListener = new UpdateListener(this);
		funcFilter.addListener(SWT.Modify, updateListener);
		fileFilter.addListener(SWT.Modify, updateListener);
		useRegExp.addListener(SWT.Selection, updateListener);
		
		setControl(filterGroup);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			funcFilter.setText(configuration.getAttribute(EmulatorProfilingLaunchConfigurationDelegate.FD_FILTER, ""));
			fileFilter.setText(configuration.getAttribute(EmulatorProfilingLaunchConfigurationDelegate.FILE_FILTER, ""));
			useRegExp.setSelection(configuration.getAttribute(EmulatorProfilingLaunchConfigurationDelegate.USE_REG_EXP, false));
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(EmulatorProfilingLaunchConfigurationDelegate.FD_FILTER, funcFilter.getText());
		configuration.setAttribute(EmulatorProfilingLaunchConfigurationDelegate.FILE_FILTER, fileFilter.getText());
		configuration.setAttribute(EmulatorProfilingLaunchConfigurationDelegate.USE_REG_EXP, useRegExp.getSelection());
	}

	@Override
	public String getName() {
		return "Profiling";
	}

	@Override
	public void updateUI() {
		// Validate
		try {
			MatchType matchType = useRegExp.getSelection() ? MatchType.REGEXP : MatchType.CONTAINS;
			NameFilter.create(funcFilter.getText(), NameFilter.Criteria.NAME, matchType, true);
			NameFilter.create(fileFilter.getText(), NameFilter.Criteria.FILE, matchType, true);
			setErrorMessage(null);
		} catch (ParseException e) {
			setErrorMessage(e.getMessage());
		}
		updateLaunchConfigurationDialog();
	}

}
