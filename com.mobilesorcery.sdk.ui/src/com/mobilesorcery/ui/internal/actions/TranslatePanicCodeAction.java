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
package com.mobilesorcery.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

public class TranslatePanicCodeAction extends Action implements IWorkbenchWindowActionDelegate {

    public class PanicCodesLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0) {
                return element.toString();
            } else if (columnIndex == 1) {
                return CoreMoSyncPlugin.getDefault().getPanicMessage((Integer) element);
            }

            return "";
        }

    }

    public class PanicCodeFilter extends ViewerFilter {

        private String filterText;

        public boolean select(Viewer viewer, Object parentElement, Object element) {
            String filterText = this.filterText;
            if (filterText == null || element == null) {
                return true;
            }
            
            int errorCode = (Integer) element;
            String errorMsg = CoreMoSyncPlugin.getDefault().getPanicMessage(errorCode);
            // I18n - who cares.
            boolean errorMsgAccept = errorMsg == null || errorMsg.toLowerCase().contains(filterText.toLowerCase());
            boolean errorCodeAccept = Integer.toString(errorCode).contains(filterText);
            
            return element == null || errorCodeAccept || errorMsgAccept;
        }

        public void setFilterText(String filterText) {
            this.filterText = filterText;
        }

    }

    class TranslatePanicCodeDialog extends Dialog {

        protected TranslatePanicCodeDialog(Shell parentShell) {
            super(parentShell);            
        }

        public Control createDialogArea(Composite parent) {
            getShell().setText("Translate Panic Code");
            Composite main = (Composite) super.createDialogArea(parent);
            Composite mainWithoutMargins = new Composite(main, SWT.NONE);
            GridLayout mainWithoutMarginsLayout = new GridLayout(2, false);
            mainWithoutMarginsLayout.marginHeight = 0;
            mainWithoutMarginsLayout.marginWidth = 0;
            mainWithoutMargins.setLayout(mainWithoutMarginsLayout);            
            
            Label errorCodeLabel = new Label(mainWithoutMargins, SWT.NONE);
            errorCodeLabel.setText("&Panic Code:");
            
            final Text errorCodeText = new Text(mainWithoutMargins, SWT.BORDER | SWT.SINGLE);
            errorCodeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            Label panicCodesLabel = new Label(mainWithoutMargins, SWT.NONE);
            panicCodesLabel.setText("Panic &Codes:");
            panicCodesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
            
            final TableViewer panicCodesTable = new TableViewer(mainWithoutMargins, SWT.BORDER | SWT.FULL_SELECTION);
            GridData panicCodesTableData = new GridData(SWT.DEFAULT, 350);
            panicCodesTableData.horizontalSpan = 2;
            panicCodesTableData.grabExcessVerticalSpace = true;
            panicCodesTable.getTable().setLayoutData(panicCodesTableData);
            panicCodesTable.getTable().setHeaderVisible(false);
            panicCodesTable.getTable().setLinesVisible(true);            
            
            TableViewerColumn codeCol = new TableViewerColumn(panicCodesTable, SWT.NONE);
            codeCol.getColumn().setWidth(120);
            
            TableViewerColumn msgCol = new TableViewerColumn(panicCodesTable, SWT.NONE);
            msgCol.getColumn().setWidth(300);
            
            panicCodesTable.setContentProvider(new ArrayContentProvider());
            panicCodesTable.setLabelProvider(new PanicCodesLabelProvider());
            panicCodesTable.setInput(CoreMoSyncPlugin.getDefault().getAllPanicErrorCodes());

            final PanicCodeFilter filter = new PanicCodeFilter();
            
            panicCodesTable.setFilters(new ViewerFilter[] {
                    filter
            });

            errorCodeText.addListener(SWT.Modify, new Listener() {
                public void handleEvent(Event event) {
                    String filterText = errorCodeText.getText();
                    filter.setFilterText(filterText);
                    panicCodesTable.refresh();
                }
            });

            return main;
        }
        
        public void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }

    }

    
    private IWorkbenchWindow window;

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    public void run(IAction action) {
    	if (CoreMoSyncPlugin.getDefault().getAllPanicErrorCodes().length == 0) {
    		MessageDialog.openError(window.getShell(), "No panic codes", "How ironic. Could not load panic code file!\nArgh. Panic. Panic. PANIC!!");
    	} else {
    		TranslatePanicCodeDialog dialog = new TranslatePanicCodeDialog(window.getShell());
    		dialog.open();
    	}
    }

    public void selectionChanged(IAction action, ISelection selection) {

    }

}
