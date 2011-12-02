package com.mobilesorcery.sdk.builder.blackberry.ui.preferences;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.mobilesorcery.sdk.builder.blackberry.BlackBerryPlugin;
import com.mobilesorcery.sdk.builder.blackberry.GuessVersionJob;
import com.mobilesorcery.sdk.builder.blackberry.JDE;
import com.mobilesorcery.sdk.ui.SimpleListEditor;
import com.mobilesorcery.sdk.ui.UIUtils;

public class JDEPreferencePage extends PreferencePage implements IWorkbenchPreferencePage{

	private SimpleListEditor<JDE> editor;

	public JDEPreferencePage() {
        super();
        setPreferenceStore(BlackBerryPlugin.getDefault().getPreferenceStore());
    }

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));

		editor = new SimpleListEditor<JDE>(main, SWT.NONE) {
			@Override
			public boolean edit(Object selection, boolean add) {
				return openEditor((JDE) selection, add);
			}

			@Override
			public JDE createObject() {
				return new JDE(new Path(""), null);
			}
		};

		editor.setInput(getJDEs());
		editor.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1));
		return main;
	}

	private List getJDEs() {
		// TODO Parse prefs!
		return BlackBerryPlugin.getDefault().getJDEs();
	}

	protected boolean openEditor(JDE selection, boolean add) {
		Shell shell = getShell();
		JDEEditorDialog dialog = new JDEEditorDialog(shell);
		dialog.setShowVersionField(!add);
		dialog.setInitial(selection);
		int result = dialog.open();
		if (result == JDEEditorDialog.OK) {
			JDE newJDE = dialog.getEdited();
			if (add) {
				GuessVersionJob job = new GuessVersionJob();
				job.setJDE(newJDE);
				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					BlackBerryPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, BlackBerryPlugin.PLUGIN_ID, "Timeout"));
				}
				newJDE.apply(new JDE(newJDE.getLocation(), job.getVersion()));
			}
			selection.apply(newJDE);
			return true;
		}

		return false;
	}

	@Override
	public boolean performOk() {
		BlackBerryPlugin.getDefault().setJDEs(editor.getEditedInput());
		return super.performOk();
	}

}
