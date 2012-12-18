package com.mobilesorcery.sdk.extensions;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.core.MoSyncExtension;
import com.mobilesorcery.sdk.core.MoSyncExtensionManager;
import com.mobilesorcery.sdk.core.MoSyncExtensionManager.ExtensionAlreadyExistsException;
import com.mobilesorcery.sdk.extensionsupport.ExtensionSupportPlugin;
import com.mobilesorcery.sdk.ui.SimpleListEditor;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ExtensionsPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	class InstalledExtensionsEditor extends SimpleListEditor<MoSyncExtension> {

		public InstalledExtensionsEditor(Composite parent, int style) {
			super(parent, style);
		}

		protected int createButtons(Composite main) {
			add = createButton(main, "&Install...");
			remove = createButton(main, "&Uninstall");

			return 2;
		}

		public MoSyncExtension add(Object nextObject) {
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			dialog.setFilterExtensions(new String[] { "*.ext" });
			String result = dialog.open();
			MoSyncExtension extension = null;
			if (result != null) {
				try {
					try {
						extension = MoSyncExtensionManager.getDefault()
								.install(new File(result), false);
					} catch (ExtensionAlreadyExistsException e) {
						MoSyncExtension ext = e.getExisting();
						if (MessageDialog
								.openQuestion(
										getShell(),
										"Extension already exists",
										MessageFormat
												.format("The extension {0} already exists.\nInstalled version: {1}\n\nDo you want to update the extension?",
														ext.getName(),
														ext.getVersion()))) {
							extension = MoSyncExtensionManager.getDefault()
									.install(new File(result), true);
						}
					}
				} catch (Exception e) {
					handleError(e, "Could not install extension");
				}
			}
			refresh();
			return extension;
		}

		public boolean remove(Object[] selection) {
			boolean doRemove = MessageDialog
					.openConfirm(
							getShell(),
							"Uninstall",
							MessageFormat
									.format("This will remove {0} extension(s).\nNOTE: This operation cannot be undone.",
											selection.length));
			if (doRemove) {
				try {
					for (int i = 0; i < selection.length; i++) {
						MoSyncExtensionManager.getDefault().uninstall(
								(MoSyncExtension) selection[i]);
					}
				} catch (Exception e) {
					handleError(e, "Could not uninstall extension");
					doRemove = false;
				}
			}
			return doRemove;
		}

		private void refresh() {
			setInput(MoSyncExtensionManager.getDefault().getExtensions());
		}

		private void handleError(Exception e, String msg) {
			ErrorDialog.openError(getShell(), msg, msg,
					new Status(IStatus.ERROR, ExtensionSupportPlugin.PLUGIN_ID,
							e.getMessage(), e));
		}
	}

	private class ExtensionLabelProvider extends StyledCellLabelProvider {

		private Styler vendorStyler;

		public ExtensionLabelProvider() {
			Color vendorColor = Display.getCurrent().getSystemColor(
					SWT.COLOR_GRAY);
			vendorStyler = UIUtils.createStyler(null, vendorColor);
		}

		public void update(ViewerCell cell) {
			MoSyncExtension ext = (MoSyncExtension) cell.getElement();
			StyledString extensionDesc = new StyledString();
			extensionDesc.append(ext.getName() + " ");
			extensionDesc.append(ext.getVersion() + " - " + ext.getVendor(),
					vendorStyler);
			cell.setStyleRanges(extensionDesc.getStyleRanges());
			cell.setText(extensionDesc.getString());
		}

	}

	public ExtensionsPreferencePage() {
		this("Extensions");
	}

	public ExtensionsPreferencePage(String title) {
		super(title);
	}

	public ExtensionsPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		InstalledExtensionsEditor extensions = new InstalledExtensionsEditor(
				main, SWT.NONE);
		extensions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		extensions.setLabelProvider(new ExtensionLabelProvider());
		extensions.refresh();
		return main;
	}

}
