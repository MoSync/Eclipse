package com.mobilesorcery.sdk.profiling.ui.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.profiling.IProfilingListener;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;
import com.mobilesorcery.sdk.profiling.ProfilingSessionEditorInput;
import com.mobilesorcery.sdk.profiling.ProfilingSessionParser;
import com.mobilesorcery.sdk.profiling.filter.NameFilter;
import com.mobilesorcery.sdk.profiling.ui.ProfilingUiPlugin;
import com.mobilesorcery.sdk.ui.CollapseAllAction;
import com.mobilesorcery.sdk.ui.DescriptiveTextFieldListener;
import com.mobilesorcery.sdk.ui.ExpandAllAction;
import com.mobilesorcery.sdk.ui.UIUtils;

public class ProfilingEditor extends EditorPart {
    
    public final static String ID = "com.mobilesorcery.sdk.profiling.ui.profiling";

    public class ProfilingListener implements IProfilingListener {
        public void handleEvent(final ProfilingEventType eventType, final IProfilingSession session) {
            if (hotspotProfilingComposite != null) {
            	hotspotProfilingComposite.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                    	if (eventType == ProfilingEventType.STARTED) {
                    		initSessionsCombo();	
                    		sessionsList.setSelection(new StructuredSelection(session), true);
                    	} else if (eventType == ProfilingEventType.STOPPED) {
                    		updateProfilingComposites(session);
                    	}
                    }
                });
            }
        }
    }

	private ProfilingComposite hotspotProfilingComposite;
    private ProfilingListener profilingEventListener;
    private ProfilingComposite callTreeProfilingComposite;
	private ComboViewer sessionsList;
	private Composite toolComposite;
	private ExpandAllAction expandAllAction;
	private CollapseAllAction collapseAllAction;
	protected ProfilingComposite currentProfilingView;
	private Text filter;
	
	private List<IProfilingSession> sessions = new ArrayList<IProfilingSession>();
	
	private boolean trackProfilingHistory = false;

	public ProfilingEditor() {
	    profilingEventListener = new ProfilingListener();
	}
	
	public void createPartControl(Composite parent) {
	    Composite main = new Composite(parent, SWT.NONE);
	    main.setLayout(new GridLayout(2, false));
	    toolComposite = new Composite(main, SWT.NONE);
	    toolComposite.setLayout(new GridLayout(2, false));
	    toolComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    Composite sessionComposite = new Composite(toolComposite, SWT.NONE);
	    sessionComposite.setLayout(new GridLayout(2, false));
	    Label sessionsLabel = new Label(sessionComposite, SWT.NONE);
	    sessionsLabel.setText("&Sessions:");
	    sessionsList = new ComboViewer(sessionComposite);
	    sessionsList.setLabelProvider(new ProfilingSessionLabelProvider());
	    sessionsList.setContentProvider(new ArrayContentProvider());
	    sessionsList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IProfilingSession session = (IProfilingSession) ((IStructuredSelection)event.getSelection()).getFirstElement();
                updateProfilingComposites(session);
                if (filter != null) {
                	filter.setText("");
                }
			}
		});
	    
	    filter = new Text(toolComposite, SWT.BORDER | SWT.SINGLE);
	    GridData filterData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
	    filterData.widthHint = UIUtils.getDefaultFieldSize();
	    filter.setLayoutData(filterData);
	    final DescriptiveTextFieldListener filterDescription = new DescriptiveTextFieldListener(filter, "Quick Filter");
	    
	    CTabFolder results = new CTabFolder(main, SWT.BOTTOM | SWT.BORDER);
	    results.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

	    final CTabItem hotSpotTab = new CTabItem(results, SWT.NONE);
	    hotSpotTab.setText("Hotspots");
	    final CTabItem callTreeTab = new CTabItem(results, SWT.NONE);
	    callTreeTab.setText("Call Tree");
	    hotspotProfilingComposite = new ProfilingComposite(results, SWT.FLAT);
	    hotspotProfilingComposite.setStatusLineManager(getEditorSite().getActionBars().getStatusLineManager());
	    hotSpotTab.setControl(hotspotProfilingComposite);
	    hotSpotTab.setImage(ProfilingUiPlugin.getDefault().getImageRegistry().get(ProfilingUiPlugin.HOTSPOTS_TAB_IMG));
	    callTreeProfilingComposite = new ProfilingComposite(results, SWT.NONE);
	    callTreeProfilingComposite.setStatusLineManager(getEditorSite().getActionBars().getStatusLineManager());
	    callTreeTab.setControl(callTreeProfilingComposite);
	    callTreeTab.setImage(ProfilingUiPlugin.getDefault().getImageRegistry().get(ProfilingUiPlugin.CALL_TREE_TAB_IMG));

	    results.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				if (e.item == hotSpotTab) {
					currentProfilingView = hotspotProfilingComposite;
				} else if (e.item == callTreeTab) {
					currentProfilingView = callTreeProfilingComposite;
				}
				updateActionBars();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}	    	
	    });
	    
	    results.setSelection(hotSpotTab);

	    filter.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				NameFilter nameFilter = new NameFilter(filterDescription.getText(), NameFilter.Type.CONTAINS);
				hotspotProfilingComposite.setFilter(nameFilter, false);
				callTreeProfilingComposite.setFilter(nameFilter, true);
			}	    	
	    });
	    
	    ProfilingPlugin.getDefault().addProfilingListener(profilingEventListener);
	    initSessionsCombo();
	    initActionBars();
	}

	public void doSave(IProgressMonitor monitor) {
	}

	public void doSaveAs() {
		FileDialog dialog = new FileDialog(getEditorSite().getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "prof" });
		dialog.setFilterNames(new String[] { "MoSync Profiling Data Files" });
		String file = dialog.open();
		if (file != null) {
			ProfilingSessionParser parser = new ProfilingSessionParser();
			try {
				parser.unparse(sessions, new File(file));
			} catch (Exception e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, ProfilingUiPlugin.PLUGIN_ID, e.getMessage(), e), StatusManager.SHOW);
			}
		}
	}

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		if (input instanceof ProfilingSessionEditorInput) {
			sessions = Arrays.asList(((ProfilingSessionEditorInput) input).getSession());
		} else if (input instanceof IAdaptable) {
			ILocationProvider location = (ILocationProvider) ((IAdaptable) input).getAdapter(ILocationProvider.class);
			IPath path = location == null ? null : location.getPath(input);
			ProfilingSessionParser parser = new ProfilingSessionParser();
			try {
				sessions = Arrays.asList(parser.parse(path.toFile()));
			} catch (Exception e) {
				throw new PartInitException(new Status(IStatus.ERROR, ProfilingUiPlugin.PLUGIN_ID, e.getMessage(), e));
			}
		}
		

		setPartName(computePartName());
	}
	
	private String computePartName() {
		if (sessions.size() == 1) {
			// Support for more soon
			return sessions.get(0).getName();
		} else {
			return getPartName();
		}
	}

	public int getSessionCount() {
		return sessions.size();
	}
	
	public boolean isDirty() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		return true;
	}
	
    private void initActionBars() {
    	IToolBarManager tbMgr = getEditorSite().getActionBars().getToolBarManager();
		expandAllAction = new ExpandAllAction();
		collapseAllAction = new CollapseAllAction();
		tbMgr.add(expandAllAction);
		tbMgr.add(collapseAllAction);
    }
    
    private void updateActionBars() {
    	if (currentProfilingView != null) {
    		expandAllAction.setViewer(currentProfilingView.getViewer());
    		collapseAllAction.setViewer(currentProfilingView.getViewer());
    		expandAllAction.setEnabled(!currentProfilingView.isFlat());
    		collapseAllAction.setEnabled(!currentProfilingView.isFlat());
    	}
    }

	protected void updateProfilingComposites(IProfilingSession session) {
		hotspotProfilingComposite.setInput(session);
        callTreeProfilingComposite.setInput(session);
	}

	private void initSessionsCombo() {
		if (trackProfilingHistory) {
			sessions = ProfilingPlugin.getDefault().getSessions();
		}
		sessionsList.setInput(sessions);
		if (sessionsList.getSelection().isEmpty() && !sessions.isEmpty()) {
			sessionsList.setSelection(new StructuredSelection(sessions.get(0)));
		}
		toolComposite.layout();
	}

	public void dispose() {
	    ProfilingPlugin.getDefault().removeProfilingListener(profilingEventListener);
	    super.dispose();
	}
	
	public void setFocus() {
		hotspotProfilingComposite.setFocus();
	}

	
}