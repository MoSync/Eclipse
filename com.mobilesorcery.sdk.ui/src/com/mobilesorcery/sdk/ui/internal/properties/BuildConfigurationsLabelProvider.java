package com.mobilesorcery.sdk.ui.internal.properties;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
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
