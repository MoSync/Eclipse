package com.mobilesorcery.sdk.core;

import org.eclipse.core.resources.IResource;

/**
 * Makeshift API to move UI/IDE specific things off the core plugin.
 * @author mattias
 *
 */
public interface ISavePolicy {

	String EXTENSION_POINT = "com.mobilesorcery.core.buildsupport";
	ISavePolicy NULL = new ISavePolicy() {

		@Override
		public boolean isSaveAllSet() {
			return false;
		}

		@Override
		public boolean saveAllEditors(IResource[] resources, boolean confirm) {
			return true;
		}

	};

	public boolean isSaveAllSet();

	public boolean saveAllEditors(IResource[] resources, boolean confirm);

}
