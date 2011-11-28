package com.mobilesorcery.sdk.capabilities.core;

import java.text.MessageFormat;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;

public class AddDeviceFilterChangeRequest extends AbstractChangeRequest {

	private final IDeviceFilter filter;
	private String message = "Remove {0} profiles";

	public AddDeviceFilterChangeRequest(MoSyncProject project, IDeviceFilter filter) {
		super(project);
		this.filter = filter;
	}

	/**
	 * Sets a message that will be returned by <code>toString</code>.
	 * Optionally, a <code>{0}</code> parameter may be used to
	 * insert the number of filtered out profiles.
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public void apply() {
		getProject().getDeviceFilter().addFilter(filter);
	}

	public IDeviceFilter getFilter() {
		return filter;
	}

	public boolean isApplicable() {
		return getRemovedCount() > 0;
	}

	private int getRemovedCount() {
		int filterCount = ProfileManager.filterProfiles(getProject().getFilteredProfiles(), filter).length;
		int totalProfileCount = getProject().getFilteredProfiles().length;
		int removedCount = totalProfileCount - filterCount;
		return removedCount;
	}

	@Override
	public String toString() {
		int removedCount = getRemovedCount();
		return MessageFormat.format(message, removedCount);
	}

}
