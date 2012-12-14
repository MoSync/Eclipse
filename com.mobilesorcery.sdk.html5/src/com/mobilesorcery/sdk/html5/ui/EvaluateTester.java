package com.mobilesorcery.sdk.html5.ui;

import java.util.Collection;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.wst.xml.core.internal.document.TextImpl;

import com.mobilesorcery.sdk.html5.Html5Plugin;


public class EvaluateTester extends ActiveDebugSessionsTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (super.test(receiver, property, args, expectedValue)) {
			if (receiver instanceof Collection) {
				Object[] recieverAr = ((Collection) receiver).toArray();
				if (recieverAr.length > 0) {
					receiver = recieverAr[0];
				}
			}
			if (receiver instanceof ITextSelection) {
				return true;
			}
			if (receiver.getClass().getName().equals("org.eclipse.wst.xml.core.internal.document.TextImpl")) {
				// Very non-future-proof HTML check.
				return true;
			}
		}
		return false;
	}

}
