package com.mobilesorcery.sdk.ui.internal.handlers;

import org.eclipse.swt.SWT;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class FindProfileForDeviceHandler extends ChangeActiveProfileHandler {

	@Override
	protected int getStyle() {
		return SWT.SEARCH;
	}

	@Override
	protected int getMode() {
		return MoSyncTool.LEGACY_PROFILE_TYPE;
	}

	@Override
	protected boolean isValid(MoSyncProject project) {
		return super.isValid(project) && project.getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE;
	}
}
