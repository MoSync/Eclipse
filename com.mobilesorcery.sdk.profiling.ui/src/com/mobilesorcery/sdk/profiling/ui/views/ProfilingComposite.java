package com.mobilesorcery.sdk.profiling.ui.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeColumnViewerLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.ILocationProvider;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.ui.UIUtils;

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
    private IProfilingSession session;
    PercentageBarLabelProvider percentageLabelProvider;
    private FunctionNameLabelProvider functionNameLabelProvider;
    private ProfilingLabelProvider labelProvider;
	private IStatusLineManager statusLine;
   
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
        labelProvider = new ProfilingLabelProvider();
        TreeColumnViewerLabelProvider cellLabelProvider = new TreeColumnViewerLabelProvider(labelProvider);
        functionNameLabelProvider = new FunctionNameLabelProvider();
        percentageLabelProvider = new PercentageBarLabelProvider(!isFlat());
        
        TreeViewerColumn functionNameCol = new TreeViewerColumn(profileTreeViewer, SWT.NONE);
        functionNameCol.getColumn().setText("Function");
        functionNameCol.getColumn().setData(ProfilingLabelProvider.FUNCTION_COL);
        functionNameCol.setLabelProvider(functionNameLabelProvider);
        prepareColumn(functionNameCol, IInvocation.SORT_BY_FUNC_NAME);
        columns.add(functionNameCol.getColumn());
        
        TreeViewerColumn percentageCol = new TreeViewerColumn(profileTreeViewer, SWT.NONE);
        percentageCol.getColumn().setText("[%]");
        percentageCol.getColumn().setData(ProfilingLabelProvider.PERCENTAGE_TIME_COL);
        percentageCol.getColumn().setAlignment(SWT.RIGHT);
        percentageCol.setLabelProvider(percentageLabelProvider);
        prepareColumn(percentageCol, isFlat() ? IInvocation.SORT_BY_SELF_TIME : IInvocation.SORT_BY_AGG_TIME);
        layout.setColumnData(percentageCol.getColumn(), new ColumnWeightData(1));
        columns.add(percentageCol.getColumn());
        
        if (isFlat()) {
            TreeViewerColumn selfTimeCol = new TreeViewerColumn(profileTreeViewer, SWT.NONE);
            selfTimeCol.getColumn().setText("Self time");
            selfTimeCol.getColumn().setData(ProfilingLabelProvider.SELF_TIME_COL);
            selfTimeCol.getColumn().setAlignment(SWT.RIGHT);
            selfTimeCol.setLabelProvider(cellLabelProvider);
            prepareColumn(selfTimeCol, IInvocation.SORT_BY_SELF_TIME);
            layout.setColumnData(selfTimeCol.getColumn(), new ColumnWeightData(1));
            columns.add(selfTimeCol.getColumn());
        } else {
            TreeViewerColumn aggTimeCol = new TreeViewerColumn(profileTreeViewer, SWT.NONE);
            aggTimeCol.getColumn().setText("Aggregate time");
            aggTimeCol.getColumn().setData(ProfilingLabelProvider.AGG_TIME_COL);
            aggTimeCol.getColumn().setAlignment(SWT.RIGHT);
            aggTimeCol.setLabelProvider(cellLabelProvider);
            prepareColumn(aggTimeCol, IInvocation.SORT_BY_AGG_TIME);
            layout.setColumnData(aggTimeCol.getColumn(), new ColumnWeightData(1));
            columns.add(aggTimeCol.getColumn());
        }
        
        TreeViewerColumn callsCol = new TreeViewerColumn(profileTreeViewer, SWT.NONE);
        callsCol.getColumn().setText("Calls");
        callsCol.getColumn().setData(ProfilingLabelProvider.CALLS_COL);
        callsCol.getColumn().setAlignment(SWT.RIGHT);
        callsCol.setLabelProvider(cellLabelProvider);
        prepareColumn(callsCol, IInvocation.SORT_BY_INVOCATION_COUNT);
        columns.add(callsCol.getColumn());
        
        layout.setColumnData(functionNameCol.getColumn(), new ColumnWeightData(3));
        layout.setColumnData(callsCol.getColumn(), new ColumnWeightData(1));
        
        labelProvider.setColumns(columns.toArray(new TreeColumn[0]));
        
        profileTreeViewer.setFilters(new ViewerFilter[] {
                new ViewerFilter() {
                    public boolean select(Viewer viewer, Object parentElement, Object element) {
                        IInvocation invocation = (IInvocation) element;
                        return !isFlat() || (session != null && session.getFilter().accept(invocation));
                    }
                }
        });
        
        profileTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();
				if (element instanceof IInvocation) {
					IInvocation invocation = (IInvocation) element;
					String filename = invocation.getProfiledEntity().getFileName();
					filename = mapToLocalFilename(filename);
					int linenumber = invocation.getProfiledEntity().getLineNumber();
					if (filename != null && new File(filename).exists()) {
						UIUtils.openResource(new Path(filename), linenumber);
						if (statusLine != null) {
							statusLine.setErrorMessage(null);
						}
					} else {
						if (statusLine != null) {
							statusLine.setErrorMessage(String.format("Could not open file %s; it does not exist?", filename));
						}
					}
				}
			}
		});
    }
    
    protected String mapToLocalFilename(String filename) {
		if (session != null) {
			ILocationProvider locationProvider = session.getLocationProvider();
			IFile workspaceRelativeLocation = locationProvider.getLocation(filename);
			if (workspaceRelativeLocation != null) {
				return workspaceRelativeLocation.getLocation().toOSString();
			}
		}
		
		return filename;
	}

	public void setStatusLineManager(IStatusLineManager statusLine) {
    	this.statusLine = statusLine;
    }

    public boolean isFlat() {
        return (getStyle() & SWT.FLAT) != 0;
    }
    
    private void prepareColumn(final TreeViewerColumn functionNameCol, final Comparator<IInvocation> comparator) {
        //if (isFlat()) {
            functionNameCol.getColumn().addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    int sortDir = SWT.DOWN;
                    if (profileTreeViewer.getTree().getSortColumn() == functionNameCol.getColumn()) {
                        sortDir = profileTreeViewer.getTree().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
                    }
                    
                    profileTreeViewer.getTree().setSortColumn(functionNameCol.getColumn());
                    profileTreeViewer.getTree().setSortDirection(sortDir);
                    profileTreeViewer.setSorter(new ComparatorViewerSorter(comparator, sortDir == SWT.UP));
                    profileTreeViewer.refresh();
                }
            });
        //}
    }
    
    public boolean setFocus() {
        return profileTreeViewer.getControl().setFocus();
    }

    public TreeViewer getViewer() {
    	return profileTreeViewer;
    }
    
    public void setInput(IProfilingSession session) {
        this.session = session;
        functionNameLabelProvider.setSession(session);
        percentageLabelProvider.setSession(session);
        labelProvider.setSession(session);
        profileTreeViewer.setInput(session);
    }

	public void setFilter(IFilter<IInvocation> filter, boolean recursive) {
		profileTreeViewer.setFilters(new ViewerFilter[] {
			new InvocationViewerFilter(filter, recursive)	
		});
	}
}
