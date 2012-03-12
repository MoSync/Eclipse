package com.mobilesorcery.sdk.core.templates;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class ProjectTemplateExtension implements IProjectTemplateExtension {

	public static final String EXTENSION_POINT_ID = "com.mobilesorcery.sdk.templates.type.extension";

	private final IConfigurationElement element;
	private boolean inited;
	private IProjectTemplateExtensionDelegate configurer;

	public ProjectTemplateExtension(IConfigurationElement element) {
		this.element = element;
	}

	@Override
	public ImageDescriptor getImage() {
		String ns = element.getDeclaringExtension().getNamespaceIdentifier();
		String icon = element.getAttribute("image");
		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(ns, icon);
		return imageDescriptor;
	}

	@Override
	public String getName() {
		return element.getAttribute("name");
	}

	@Override
	public void configureProject(MoSyncProject project) throws CoreException {
		if (!inited) {
			inited = true;
			try {
				if (element.getAttribute("configurer") != null) {
					configurer = (IProjectTemplateExtensionDelegate) element.createExecutableExtension("configurer");
				}
			} catch (CoreException e) {
				CoreMoSyncPlugin.getDefault().log(e);
				// Never mind.
			}
		}
		if (configurer != null) {
			configurer.configureProject(project);
		}
	}

	@Override
	public String getType() {
		return element.getAttribute("type");
	}
}
