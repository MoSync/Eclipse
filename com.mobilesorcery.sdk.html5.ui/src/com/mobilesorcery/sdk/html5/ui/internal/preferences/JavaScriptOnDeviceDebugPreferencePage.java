package com.mobilesorcery.sdk.html5.ui.internal.preferences;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

public class JavaScriptOnDeviceDebugPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage, IUpdatableControl {

	private LinkedHashMap<String, Integer> reloadStrategyMap;
	private LinkedHashMap<String, Integer> sourceChangeStrategyMap;
	private HashMap<Integer, String> reverseReloadStrategyMap;
	private HashMap<Integer, String> reverseSourceChangeStrategyMap;
	private ComboViewer sourceChangeStrategyCombo;
	private ComboViewer reloadStrategyCombo;
	private Text timeoutInSecsText;
	private Label reloadStrategyLabel;

	public JavaScriptOnDeviceDebugPreferencePage() {
		super("JavaScript On-Device Debug");
		this.reloadStrategyMap = new LinkedHashMap<String, Integer>();
		reloadStrategyMap.put("Ask me", RedefinitionResult.UNDETERMINED);
		reloadStrategyMap.put("Do nothing", RedefinitionResult.CONTINUE);
		// Disabled until 3.1.1
		//reloadStrategyMap.put("Reload", RedefinitionResult.RELOAD);
		reloadStrategyMap.put("Terminate", RedefinitionResult.TERMINATE);
		reverseReloadStrategyMap = Util.reverseMap(reloadStrategyMap);
		this.sourceChangeStrategyMap = new LinkedHashMap<String, Integer>();
		sourceChangeStrategyMap.put("Do nothing", Html5Plugin.DO_NOTHING);
		sourceChangeStrategyMap.put("Reload", Html5Plugin.RELOAD);
		// Disabled until 3.1.1
		//sourceChangeStrategyMap.put("Hot code replace", Html5Plugin.HOT_CODE_REPLACE);
		reverseSourceChangeStrategyMap = Util.reverseMap(sourceChangeStrategyMap);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Html5Plugin.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		Group connectionGroup = new Group(main, SWT.NONE);
		connectionGroup.setText("Connection");
		connectionGroup.setLayout(new GridLayout(2, false));
		Label timeoutInSecsLabel = new Label(connectionGroup, SWT.NONE);
		timeoutInSecsLabel.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		timeoutInSecsLabel.setText("Connection timeout (s):");
		timeoutInSecsText = new Text(connectionGroup, SWT.SINGLE | SWT.BORDER);
		int timeoutInSecs = Html5Plugin.getDefault().getTimeout();
		timeoutInSecsText.setText(Integer.toString(timeoutInSecs));
		timeoutInSecsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		connectionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Group executionGroup = new Group(main, SWT.NONE);
		executionGroup.setLayout(new GridLayout(2, false));
		executionGroup.setText("Execution");
		executionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label sourceChangeStrategyLabel = new Label(executionGroup, SWT.NONE);
		sourceChangeStrategyLabel.setText("When source changes:");
		sourceChangeStrategyLabel.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		sourceChangeStrategyCombo = new ComboViewer(executionGroup, SWT.READ_ONLY);
		sourceChangeStrategyCombo.setLabelProvider(new LabelProvider());
		sourceChangeStrategyCombo.setContentProvider(new ArrayContentProvider());
		sourceChangeStrategyCombo.setInput(sourceChangeStrategyMap.keySet().toArray());
		int sourceChangeStrategy = Html5Plugin.getDefault().getSourceChangeStrategy();
		String sourceChangeStrategyStr = reverseSourceChangeStrategyMap.get(sourceChangeStrategy);
		if (sourceChangeStrategyStr != null) {
			sourceChangeStrategyCombo.setSelection(new StructuredSelection(sourceChangeStrategyStr));	
		}
		sourceChangeStrategyCombo.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		reloadStrategyLabel = new Label(executionGroup, SWT.NONE);
		reloadStrategyLabel.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		
		reloadStrategyCombo = new ComboViewer(executionGroup, SWT.READ_ONLY);
		reloadStrategyCombo.setLabelProvider(new LabelProvider());
		reloadStrategyCombo.setContentProvider(new ArrayContentProvider());
		reloadStrategyCombo.setInput(reloadStrategyMap.keySet().toArray());
		int reloadStrategy = Html5Plugin.getDefault().getReloadStrategy();
		String strategyStr = reverseReloadStrategyMap.get(reloadStrategy);
		if (strategyStr != null) {
			reloadStrategyCombo.setSelection(new StructuredSelection(strategyStr));	
		}
		reloadStrategyCombo.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		UpdateListener listener = new UpdateListener(this);
		reloadStrategyCombo.getCombo().addListener(SWT.Selection, listener);
		sourceChangeStrategyCombo.getCombo().addListener(SWT.Selection, listener);
		updateUI();
		
		return main;
	}
	
	@Override
	public void updateUI() {
		boolean requiresRemoteFetch = Util.equals(getSourceChangeStrategy(), Html5Plugin.RELOAD) || Util.equals(getSourceChangeStrategy(), Html5Plugin.HOT_CODE_REPLACE);
		String op = sourceChangeStrategyCombo.getCombo().getText();
		// For 3.1.1: update with hot code replace as well
		reloadStrategyLabel.setText(MessageFormat.format("When reload fails:", op));
		
		if (requiresRemoteFetch) {
			setMessage("This will cause the client to fetch files from the debug server.", IMessageProvider.INFORMATION);
		} else {
			setMessage(null, IMessageProvider.NONE);
		}
	}
	
	private Integer getReloadStrategy() {
		IStructuredSelection reloadStrategySelection = (IStructuredSelection) reloadStrategyCombo.getSelection();
		String reloadStrategySelectionStr = (String) reloadStrategySelection.getFirstElement();
		Integer reloadStrategy = reloadStrategyMap.get(reloadStrategySelectionStr);
		return reloadStrategy;
	}
	
	private Integer getSourceChangeStrategy() {
		IStructuredSelection sourceChangeStrategySelection = (IStructuredSelection) sourceChangeStrategyCombo.getSelection();
		String sourceChangeStrategySelectionStr = (String) sourceChangeStrategySelection.getFirstElement();
		Integer sourceChangeStrategy = sourceChangeStrategyMap.get(sourceChangeStrategySelectionStr);
		return sourceChangeStrategy;
	}
	
	public boolean performOk() {
		Integer reloadStrategy = getReloadStrategy();
		if (reloadStrategy != null) {
			Html5Plugin.getDefault().setReloadStrategy(reloadStrategy);
		}
		
		Integer sourceChangeStrategy = getSourceChangeStrategy();
		if (sourceChangeStrategy != null) {
			Html5Plugin.getDefault().setSourceChangeStrategy(sourceChangeStrategy);
		}
		try {
			Html5Plugin.getDefault().setTimeout(Integer.parseInt(timeoutInSecsText.getText()));
		} catch (Exception e) {
			Html5Plugin.getDefault().setTimeout(Html5Plugin.DEFAULT_TIMEOUT);
		}
		return super.performOk();
	}

}