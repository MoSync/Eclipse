/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class BuildConfigurationsLabelProvider extends LabelProvider implements ITableLabelProvider {

	private MoSyncProject project;

	public BuildConfigurationsLabelProvider(MoSyncProject project) {
		this.project = project;
	}

	public String getText(Object element) {
		IBuildConfiguration cfg = project.getActiveBuildConfiguration();
		boolean isActive = cfg != null && element.equals(cfg.getId());
		return MessageFormat.format("{0} {1}", element.toString(), isActive ? " [Active]" : "");
	}

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		return getText(element);
	}
}
