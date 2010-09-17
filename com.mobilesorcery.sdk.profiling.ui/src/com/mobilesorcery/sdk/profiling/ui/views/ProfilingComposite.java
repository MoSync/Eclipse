package com.mobilesorcery.sdk.profiling.ui.views;

import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.IInvocation;

public class ProfilingComposite extends Composite {
    
    private final class ComparatorViewerSorter extends ViewerSorter {
        private final Comparator<IInvocation> comparator;

        private ComparatorViewerSorter(Comparator<IInvocation> comparator, boolean ascending) {
            this.comparator = ascending ? comparator : Util.reverseComparator(comparator);
        }

        public int compare(Viewer viewer, Object o1, Object o2) {
            return comparator.compare((IInvocation) o1, (IInvocation) o2);
        }
    }
    private TreeViewer profileTreeViewer;

    public ProfilingComposite(Composite parent, int style) {
        super(parent, style);
        Composite treeContainer = this;
        TreeColumnLayout layout = new TreeColumnLayout();
        treeContainer.setLayout(layout);
        
        profileTreeViewer = new TreeViewer(treeContainer, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        Tree profileTree = profileTreeViewer.getTree();
        profileTree.setHeaderVisible(true);
        profileTree.setLinesVisible(true);
        profileTreeViewer.setContentProvider(new ProfilingContentProvider(isFlat()));
        profileTreeViewer.setInput(IInvocation.EMPTY);
        
        ArrayList<TreeColumn> columns = new ArrayList<TreeColumn>();
        
        TreeColumn functionNameCol = new TreeColumn(profileTree, SWT.NONE);
        functionNameCol.setText("Function");
        functionNameCol.setData(ProfilingLabelProvider.FUNCTION_COL);
        prepareColumn(functionNameCol, IInvocation.SORT_BY_FUNC_NAME);
        columns.add(functionNameCol);
        
        if (isFlat()) {
            TreeColumn selfTimeCol = new TreeColumn(profileTree, SWT.NONE);
            selfTimeCol.setText("Self time");
            selfTimeCol.setData(ProfilingLabelProvider.SELF_TIME_COL);
            prepareColumn(selfTimeCol, IInvocation.SORT_BY_SELF_TIME);
            layout.setColumnData(selfTimeCol, new ColumnWeightData(1));
            columns.add(selfTimeCol);
        } else {
            TreeColumn aggTimeCol = new TreeColumn(profileTree, SWT.NONE);
            aggTimeCol.setText("Aggregate time");
            aggTimeCol.setData(ProfilingLabelProvider.AGG_TIME_COL);
            prepareColumn(aggTimeCol, IInvocation.SORT_BY_AGG_TIME);
            layout.setColumnData(aggTimeCol, new ColumnWeightData(1));
            columns.add(aggTimeCol);
        }
        
        TreeColumn callsCol = new TreeColumn(profileTree, SWT.NONE);
        callsCol.setText("Calls");
        callsCol.setData(ProfilingLabelProvider.CALLS_COL);
        prepareColumn(callsCol, IInvocation.SORT_BY_INVOCATION_COUNT);
        columns.add(callsCol);
        
        profileTreeViewer.setLabelProvider(new ProfilingLabelProvider(columns.toArray(new TreeColumn[0])));
        
        layout.setColumnData(functionNameCol, new ColumnWeightData(3));
        layout.setColumnData(callsCol, new ColumnWeightData(1));
    
    }
    
    private boolean isFlat() {
        return (getStyle() & SWT.FLAT) != 0;
    }
    
    private void prepareColumn(final TreeColumn column, final Comparator<IInvocation> comparator) {
        if (isFlat()) {
            column.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    int sortDir = SWT.DOWN;
                    if (profileTreeViewer.getTree().getSortColumn() == column) {
                        sortDir = profileTreeViewer.getTree().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
                    }
                    
                    profileTreeViewer.getTree().setSortColumn(column);
                    profileTreeViewer.getTree().setSortDirection(sortDir);
                    profileTreeViewer.setSorter(new ComparatorViewerSorter(comparator, sortDir == SWT.UP));
                }
            });
        }
    }
    
    public boolean setFocus() {
        return profileTreeViewer.getControl().setFocus();
    }

    public void setInput(IInvocation invocation) {
        profileTreeViewer.setInput(invocation);
    }
}
