package com.mobilesorcery.sdk.html5.ui.internal.preferences;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
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
	private Button shouldFetchRemotely;
	private Text timeoutInSecsText;
	private Label reloadStrategyLabel;
	private Text serverAddress;
	private Button useDefaultServerAddress;
	private Label sourceChangeStrategyLabel;

	public JavaScriptOnDeviceDebugPreferencePage() {
		super("JavaScript On-Device Debug");
		noDefaultAndApplyButton();
		this.reloadStrategyMap = new LinkedHashMap<String, Integer>();
		reloadStrategyMap.put("Ask me", RedefinitionResult.UNDETERMINED);
		reloadStrategyMap.put("Terminate", RedefinitionResult.TERMINATE);
		reloadStrategyMap.put("Do nothing", RedefinitionResult.CONTINUE);
		this.sourceChangeStrategyMap = new LinkedHashMap<String, Integer>();
		sourceChangeStrategyMap.put("Do nothing", Html5Plugin.DO_NOTHING);
		sourceChangeStrategyMap.put("Reload", Html5Plugin.RELOAD);

		if (Html5Plugin.getDefault().isFeatureSupported(JSODDSupport.EDIT_AND_CONTINUE)) {
			reloadStrategyMap.put("Reload", RedefinitionResult.RELOAD);
			sourceChangeStrategyMap.put("Hot code replace", Html5Plugin.HOT_CODE_REPLACE);
		}
		
		reverseReloadStrategyMap = Util.reverseMap(reloadStrategyMap);
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
		executionGroup.setText("Debugging");
		executionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		sourceChangeStrategyLabel = new Label(executionGroup, SWT.NONE);
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
		
		shouldFetchRemotely = new Button(executionGroup, SWT.CHECK);
		shouldFetchRemotely.setText("Load source code and resources from debug server");
		shouldFetchRemotely.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));
		shouldFetchRemotely.setSelection(Html5Plugin.getDefault().shouldFetchRemotely());
		
		Label serverAddressLabel = new Label(executionGroup, SWT.NONE);
		serverAddressLabel.setText("Server address:");
		serverAddressLabel.setLayoutData(new GridData(UIUtils.getDefaultFieldSize(), SWT.DEFAULT));
		
		serverAddress = new Text(executionGroup, SWT.SINGLE | SWT.BORDER);
		serverAddress.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label spacer = new Label(executionGroup, SWT.NONE);
		useDefaultServerAddress = new Button(executionGroup, SWT.CHECK);
		useDefaultServerAddress.setText("Default");
		useDefaultServerAddress.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				try {
					serverAddress.setText(Html5Plugin.getDefault().getDefaultServerURL().toExternalForm());
				} catch (IOException e) {
					serverAddress.setText(e.getMessage());
				}
			}
		});
		
		try {
			serverAddress.setText(Html5Plugin.getDefault().getServerURL().toExternalForm());
			useDefaultServerAddress.setSelection(Html5Plugin.getDefault().getServerURL().equals(Html5Plugin.getDefault().getDefaultServerURL()));
		} catch (IOException e) {
			serverAddress.setText(e.getMessage());
		}
		
		UpdateListener listener = new UpdateListener(this);
		reloadStrategyCombo.getCombo().addListener(SWT.Selection, listener);
		sourceChangeStrategyCombo.getCombo().addListener(SWT.Selection, listener);
		serverAddress.addListener(SWT.Modify, listener);
		useDefaultServerAddress.addListener(SWT.Selection, listener);
		
		updateUI();
		
		return main;
	}
	
	@Override
	public void updateUI() {
		boolean requiresRemoteFetch = Util.equals(getSourceChangeStrategy(), Html5Plugin.RELOAD) || Util.equals(getSourceChangeStrategy(), Html5Plugin.HOT_CODE_REPLACE);
		String op = requiresRemoteFetch ? 
				sourceChangeStrategyCombo.getCombo().getText().toLowerCase() :
				"this";

		reloadStrategyLabel.setText(MessageFormat.format("When {0} fails:", op));
		reloadStrategyCombo.getCombo().setEnabled(requiresRemoteFetch);
		reloadStrategyLabel.setEnabled(requiresRemoteFetch);
		
		if (requiresRemoteFetch) {
			shouldFetchRemotely.setSelection(true);
		}
		shouldFetchRemotely.setEnabled(!requiresRemoteFetch);
		
		serverAddress.setEnabled(!useDefaultServerAddress.getSelection());
		
		validate();
	}
	
	private void validate() {
		String errorMessage = null;
		try {
			// Validate.
			URL serverURL = new URL(serverAddress.getText());
			if (!"http".equals(serverURL.getProtocol())) {
				errorMessage = "Server address must use 'http' protocol";
			}
		} catch (MalformedURLException e) {
			errorMessage = "Invalid server address";
		}
		if (Util.equals(getSourceChangeStrategy(), Html5Plugin.RELOAD) &&
			Util.equals(getReloadStrategy(), RedefinitionResult.RELOAD)) {
			errorMessage = "Circular choice; please select other reload strategy.";
		}
		setErrorMessage(errorMessage);
		setValid(errorMessage == null);
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
		
		Html5Plugin.getDefault().setShouldFetchRemotely(shouldFetchRemotely.getSelection());
		
		try {
			Html5Plugin.getDefault().setTimeout(Integer.parseInt(timeoutInSecsText.getText()));
		} catch (Exception e) {
			Html5Plugin.getDefault().setTimeout(Html5Plugin.DEFAULT_TIMEOUT);
		}
		
		try {
			Html5Plugin.getDefault().setServerURL(serverAddress.getText() , useDefaultServerAddress.getSelection());
		} catch (IOException e) {
			return false;
		}
		
		return super.performOk();
	}

}
