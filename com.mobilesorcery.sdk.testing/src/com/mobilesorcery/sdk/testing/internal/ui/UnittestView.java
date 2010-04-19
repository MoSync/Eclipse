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

import java.util.Random;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.mobilesorcery.sdk.testing.IRelaunchableTestSession;
import com.mobilesorcery.sdk.testing.ITest;
import com.mobilesorcery.sdk.testing.ITestSession;
import com.mobilesorcery.sdk.testing.ITestSessionListener;
import com.mobilesorcery.sdk.testing.Test;
import com.mobilesorcery.sdk.testing.TestManager;
import com.mobilesorcery.sdk.testing.TestResult;
import com.mobilesorcery.sdk.testing.TestSessionEvent;
import com.mobilesorcery.sdk.testing.emulator.EmulatorTestSession;

public class UnittestView extends ViewPart implements ITestSessionListener {

	class RerunAction extends Action {

		public RerunAction() {
			setText("Run Again");
			setToolTipText("Run Again");
		}

		public void run() {
			ITestSession session = testRun.getTestSession();
			if (session instanceof IRelaunchableTestSession) {
				ILaunchConfiguration launchConfiguration = ((IRelaunchableTestSession) session).getLaunchConfiguration();
				DebugUITools.launch(launchConfiguration, "run");
			}
		}		
	}
	
	class GotoLineAction extends Action {
		
		private ISelection selection;

		public GotoLineAction() {
			setText("Goto");			
		}
		
		public void setSelection(ISelection selection) {
			this.selection = selection;
			this.setEnabled(computeEnabled());
		}
		
		private boolean computeEnabled() {
			return run(true);
		}

		public void run() {
			run(false);
		}
		
		public boolean run(boolean noAction) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection sSelection = (IStructuredSelection) selection;
				Object o = sSelection.getFirstElement();
				if (o instanceof ITest) {
					ITest test = (ITest) o;
					TestResult result = testRun.getTestSession().getTestResult();
					Object fileObj = result.getProperty(test, EmulatorTestSession.FILE_KEY);
					Object lineObj = result.getProperty(test, EmulatorTestSession.LINE_KEY);
	                if (fileObj != null) {
		                IWorkspace ws = ResourcesPlugin.getWorkspace();
		                IFile[] files = ws.getRoot().findFilesForLocation(new Path(fileObj.toString()));

		                boolean hasFileInWorkspace = files.length > 0;
		                if (hasFileInWorkspace && !noAction) {
		                	try {
								IEditorPart part = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), files[0]);
								if (part instanceof ITextEditor) {
									ITextEditor textEditor = (ITextEditor) part;
									IDocumentProvider provider= textEditor.getDocumentProvider();
									IDocument document= provider.getDocument(textEditor.getEditorInput());
									int start = document.getLineOffset(Integer.parseInt(lineObj.toString()) - 1);
									textEditor.selectAndReveal(start, 0);
								}
							} catch (PartInitException e) {
								throw new RuntimeException(e);
							} catch (NumberFormatException e) {
								// Ignore, just don't show.
							} catch (org.eclipse.jface.text.BadLocationException e) {
								// Ignore, just don't show.
							}
		                }
		                
		                return hasFileInWorkspace;
	                }
				}
			}
			
			return false;
		}		
	}

	class ExpandAllAction extends Action {
		public ExpandAllAction() {
			setText("E&xpand All");
			setToolTipText("Expand All");
		}

		public void run() {
			testRun.expandAll();
		}
	}

	class DummyTest extends Test {
		private boolean fail;

		public DummyTest(String name, boolean fail) {
			super(name);
			this.fail = fail;
		}

		public void run(TestResult result) throws Exception {
			Thread.sleep(new Random(System.currentTimeMillis()).nextInt(2000));
			if (fail) {
				throw new IllegalArgumentException();
			}
		}
	}

	private TestRunComposite testRun;
	private GotoLineAction gotoLineAction;
	public final static String ID = "com.mobilesorcery.sdk.testing.view";

	public void createPartControl(Composite parent) {
		testRun = new TestRunComposite(parent, SWT.NONE);
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				handleMenuAboutToShow(manager);
			}
		});

		getSite().registerContextMenu(menuMgr, testRun.getSelectionProvider());
		Menu menu = menuMgr.createContextMenu(testRun);
		// TODO: Fixme.
		testRun.setMenu(menu);
		
		TreeViewer viewer = testRun.getTreeViewer();		
		gotoLineAction = new GotoLineAction();
		
		viewer.getControl().setMenu(menu);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				gotoLineAction.setSelection(event.getSelection());
				gotoLineAction.run();
			}
		});

		TestManager.getInstance().addSessionListener(this);
		testRun.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TestManager.getInstance().removeSessionListener(
						UnittestView.this);
			}
		});

		testRun.setCurrentTestSession(TestManager.getInstance().getLastSession());
	}

	public void setFocus() {
		// TODO Auto-generated method stub

	}

	void handleMenuAboutToShow(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) testRun
				.getSelectionProvider().getSelection();
        ITestSession session = testRun.getTestSession();
        
        if (session instanceof IRelaunchableTestSession) {
            manager.add(new RerunAction());
            manager.add(new Separator());
        }
        
		if (!selection.isEmpty()) {
			ITest testElement = (ITest) selection.getFirstElement();
			
			gotoLineAction.setSelection(selection);
			manager.add(gotoLineAction);
			
			if (session.getTestCount() > 0) {
				manager.add(new ExpandAllAction());
			}

		}

		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS
				+ "-end")); //$NON-NLS-1$
	}

	public void handleEvent(final TestSessionEvent event) {
		if (event.type == TestSessionEvent.SESSION_STARTED) {
			getSite().getWorkbenchWindow().getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					getSite().getPage().activate(getSite().getPart());
					testRun.setCurrentTestSession(event.session);
				}				
			});
		}
	}

}
