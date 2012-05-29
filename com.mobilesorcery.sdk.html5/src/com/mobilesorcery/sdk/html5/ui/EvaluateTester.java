package com.mobilesorcery.sdk.html5.ui;

import java.util.Collection;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.ITextSelection;

import com.mobilesorcery.sdk.html5.Html5Plugin;


public class EvaluateTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof Collection) {
			Object[] recieverAr = ((Collection) receiver).toArray();
			if (recieverAr.length > 0) {
				receiver = recieverAr[0];
			}
		}
		if (receiver instanceof ITextSelection && !Html5Plugin.getDefault().getReloadServer().getVMs(false).isEmpty()) {
			return true;
		}
		return false;
	}

}
