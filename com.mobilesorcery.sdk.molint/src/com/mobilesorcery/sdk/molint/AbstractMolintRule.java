package com.mobilesorcery.sdk.molint;

import org.eclipse.core.resources.IMarker;


public abstract class AbstractMolintRule implements IMolintRule {

	private String id;
	private int severity;
	private String name;

	protected AbstractMolintRule(String id, String name) {
		this.id = id;
		this.name = name;
		MolintPlugin.getDefault().getPreferenceStore().setDefault(getPrefName(), getDefaultSeverity());
		this.severity = getDefaultSeverity();
	}
	
	private String getPrefName() {
		return "rule." + getId();
	}

	@Override
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public void setSeverity(int severity) {
		this.severity = severity;
		MolintPlugin.getDefault().getPreferenceStore().setValue(getPrefName(), severity);
	}
	
	public int getSeverity() {
		return MolintPlugin.getDefault().getPreferenceStore().getInt(getPrefName());
	}
	
	protected int getDefaultSeverity() {
		return IMarker.SEVERITY_ERROR;
	}
	
	protected int getSeverity(int originalSeverity) {
		return Math.min(originalSeverity, severity);
	}

}
