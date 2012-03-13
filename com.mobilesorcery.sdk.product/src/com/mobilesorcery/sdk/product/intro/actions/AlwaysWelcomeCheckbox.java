package com.mobilesorcery.sdk.product.intro.actions;

import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroXHTMLContentProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.w3c.dom.Element;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class AlwaysWelcomeCheckbox extends
		org.eclipse.ui.intro.contentproviders.AlwaysWelcomeCheckbox implements
		IIntroXHTMLContentProvider {

	private synchronized static void eschewStandbyMode() {
		// MOSYNC-1870; very ugly fix.
		// That pesky problem view is the problem (the irony of it!);
		// the IDE Workbench Plugin initializes
		// the view upon bundle startup, which in its turn will set the intro
		// in standby mode. Which we do not want.
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		boolean doEschew = false;
		for (StackTraceElement stackFrame : stackTrace) {
			doEschew |= stackFrame.getClassName().contains("IDEWorkbenchPlugin");
		}
		if (doEschew) {
			Runnable eschewRunnable = new Runnable() {
				@Override
				public void run() {
					final IIntroManager im = getIntroManager();
					final IIntroPart intro = im.getIntro();
					if (intro != null && im.isIntroStandby(intro)) {
						im.setIntroStandby(intro, false);
						if (CoreMoSyncPlugin.getDefault().isDebugging()) {
							CoreMoSyncPlugin.trace("Auto-disabled intro standby");
						}
					}
				}
			};
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
					.getDisplay().asyncExec(eschewRunnable);
		}
	}

	protected static IIntroManager getIntroManager() {
		return PlatformUI.getWorkbench()
		.getIntroManager();
	}

	@Override
	public void createContent(String s, Element element) {
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			IIntroManager im = getIntroManager();
			boolean isStandby = im.isIntroStandby(im.getIntro());
			CoreMoSyncPlugin.trace("Intro showing." + (isStandby ? " (Standby)" : ""));
		}
		boolean alwaysShowIntro = getAlwaysShowIntroPref();
		eschewStandbyMode();

		Element divElement = element.getOwnerDocument().createElement("div");
		Element inputElement = element.getOwnerDocument()
				.createElement("input");
		inputElement.setAttribute("type", "checkbox");
		inputElement.setAttribute("onClick",
				"window.location=\"http://org.eclipse.ui.intro/runAction?"
						+ "pluginId=com.mobilesorcery.sdk.product&class="
						+ getClass().getName() + "\"");
		element.appendChild(divElement);
		divElement.appendChild(inputElement);
		divElement.appendChild(element.getOwnerDocument().createTextNode(
				getText()));
		if (alwaysShowIntro) {
			inputElement.setAttribute("checked", "checked");
			PlatformUI.getPreferenceStore().setValue(
					IWorkbenchPreferenceConstants.SHOW_INTRO, alwaysShowIntro);
		}
	}

	@Override
	public String getText() {
		return "Show this screen at startup";
	}
}
