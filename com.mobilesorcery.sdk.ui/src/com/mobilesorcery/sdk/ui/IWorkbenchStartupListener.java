package com.mobilesorcery.sdk.ui;

import org.eclipse.ui.IWorkbenchListener;

/**
 * <p>An interface that complements the Eclipse {@link IWorkbenchListener},
 * which does not have any startup event.</p>
 * @see UIUtils#awaitWorkbenchStartup(IWorkbenchStartupListener)
 * @author Mattias Bybro
 *
 */
public interface IWorkbenchStartupListener {

	/**
	 * Callback method for this listener that will be called
	 * once the workbench is started, or (almost) at once if already started
	 */
	public void started();
}
