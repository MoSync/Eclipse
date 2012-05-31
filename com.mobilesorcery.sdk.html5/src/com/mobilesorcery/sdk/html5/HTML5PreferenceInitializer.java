package com.mobilesorcery.sdk.html5;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;


public class HTML5PreferenceInitializer extends AbstractPreferenceInitializer {

	public HTML5PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {
		Html5Plugin.getDefault().getPreferenceStore().setDefault(Html5Plugin.SOURCE_CHANGE_STRATEGY_PREF, Html5Plugin.RELOAD);
		Html5Plugin.getDefault().getPreferenceStore().setDefault(Html5Plugin.RELOAD_STRATEGY_PREF, RedefinitionResult.UNDETERMINED);
		Html5Plugin.getDefault().getPreferenceStore().setDefault(Html5Plugin.SHOULD_FETCH_REMOTELY_PREF, true);
	}

}
