/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.testing.internal.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

import com.mobilesorcery.sdk.testing.AssertionFailed;
import com.mobilesorcery.sdk.testing.IAssertionFailed;

public class FailureDetailsLabelProvider extends CellLabelProvider {

	public void update(ViewerCell cell) {
		cell.setText(getText(cell.getElement()));
	}

	private String getText(Object element) {
		if (element instanceof IAssertionFailed) {
			IAssertionFailed assertionFailed = (IAssertionFailed) element;
			return AssertionFailed.toString(assertionFailed);
		} else {
			return "" + element;
		}
	}
	

}
