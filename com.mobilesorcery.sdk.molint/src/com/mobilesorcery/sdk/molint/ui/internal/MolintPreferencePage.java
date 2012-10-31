package com.mobilesorcery.sdk.molint.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.molint.IMolintRule;
import com.mobilesorcery.sdk.molint.MolintPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class MolintPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IUpdatableControl {

	private static final String[] SEVERITY_LABELS = new String[] { "Ignore", "Warning", "Error" };
	private static final Integer[] SEVERITY_VALUES = new Integer[] { IMarker.SEVERITY_INFO, IMarker.SEVERITY_WARNING, IMarker.SEVERITY_ERROR };
	
	private List<IMolintRule> rules;
	
	private ArrayList<Combo> severityCombos = new ArrayList<Combo>();
	private Button enableButton;

	@Override
	public void init(IWorkbench workbench) {
		rules = MolintPlugin.getDefault().getAllRules();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(2));
		enableButton = new Button(main, SWT.CHECK);
		enableButton.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false, 2, 1));
		enableButton.setText("Enable &Molint");
		enableButton.setSelection(MolintPlugin.getDefault().isMolintEnabled());
		enableButton.addListener(SWT.Selection, new UpdateListener(this));
		
		int ix = 0;
		for (IMolintRule rule : rules) {
			String ruleText = rule.getName();
			Label ruleLabel = new Label(main, SWT.NONE);
			ruleLabel.setText(ruleText);
			int initialSeverity = rule.getSeverity();
			createSeverityCombo(main, ix, initialSeverity);
			ix++;
		}
		
		updateUI();
		
		return main;
	}
	
	public void updateUI() {
		for (Combo severityCombo : severityCombos) {
			severityCombo.setEnabled(enableButton.getSelection());
		}
	}

	private void createSeverityCombo(Composite main, int ix, int initialValue) {
		Combo combo = new Combo(main, SWT.READ_ONLY);
		combo.setItems(SEVERITY_LABELS);
		for (int i = 0; i < SEVERITY_VALUES.length; i++) {
			if (SEVERITY_VALUES[i] == initialValue) {
				combo.select(i);
			}
		}
		severityCombos.add(combo);
	}
	
	public boolean performOk() {
		MolintPlugin.getDefault().setMolintEnabled(enableButton.getSelection());
		for (int i = 0; i < severityCombos.size(); i++) {
			Combo severityCombo = severityCombos.get(i);
			int ix = severityCombo.getSelectionIndex();
			rules.get(i).setSeverity(SEVERITY_VALUES[ix]);
		}
		return super.performOk();
	}

}
