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
package com.mobilesorcery.sdk.finalizer.ui;

import java.io.StringReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.finalizer.core.FinalizerParser;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class FinalizerView extends ViewPart {

	private Text scriptText;

	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));

		Button propagate = new Button(main, SWT.PUSH);
		propagate.setText(Messages.FinalizerView_Propagate);

		Label scriptLabel = new Label(main, SWT.NONE);
		scriptLabel.setText(Messages.FinalizerView_FinalizerScript);

		scriptText = new Text(main, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		GridData scriptData = new GridData(GridData.FILL_BOTH);
		scriptText.setLayoutData(scriptData);

		Button finalize = new Button(main, SWT.PUSH);
		finalize.setText(Messages.FinalizerView_Finalize);

		propagate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				final MoSyncProject project = MosyncUIPlugin.getDefault()
						.getCurrentlySelectedProject(
								getSite().getWorkbenchWindow());

				if (project != null) {
					Job job = new Job(Messages.FinalizerView_GeneratingScript) {
						protected IStatus run(IProgressMonitor monitor) {
							final FinalizerParser parser = new FinalizerParser(
									project.getWrappedProject());
							scriptText.getDisplay().asyncExec(new Runnable() {
								public void run() {
									scriptText.setText(parser
											.extractScriptFromProject(project));
								}
							});

							return Status.OK_STATUS;
						}
					};

					job.setUser(true);
					job.schedule();
				} else {
					scriptText.setText(Messages.FinalizerView_NoProjectSelected);
				}
			}
		});

		final IWorkbenchWindow window = getSite().getWorkbenchWindow();

		finalize.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				final StringReader script = new StringReader(scriptText
						.getText());
				MoSyncProject project = MosyncUIPlugin.getDefault().getCurrentlySelectedProject(window);
				FinalizeJob job = new FinalizeJob(project, scriptText.getText());
				job.setUser(true);
				job.schedule();
			}
		});
	}

	public void setFocus() {
	}

}
