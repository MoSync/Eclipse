package com.mobilesorcery.sdk.profiling.ui.views;

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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingListener;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;
import com.mobilesorcery.sdk.profiling.ui.ProfilingUiPlugin;

public class ProfilingView extends ViewPart {
    
    public final static String ID = "com.mobilesorcery.sdk.profiling.ui.profiling";

    public class ProfilingListener implements IProfilingListener {
        public void handleEvent(ProfilingEventType eventType, final IProfilingSession session) {
            if (hotspotProfilingComposite != null && eventType == ProfilingEventType.STOPPED) {
                hotspotProfilingComposite.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        hotspotProfilingComposite.setInput(session);
                        callTreeProfilingComposite.setInput(session);
                    }                    
                });
            }
        }
    }

	private ProfilingComposite hotspotProfilingComposite;
    private ProfilingListener profilingEventListener;
    private ProfilingComposite callTreeProfilingComposite;

	public ProfilingView() {
	    profilingEventListener = new ProfilingListener();
	}
	
	public void createPartControl(Composite parent) {
	    TabFolder main = new TabFolder(parent, SWT.BOTTOM);
	    TabItem hotSpotTab = new TabItem(main, SWT.NONE);
	    hotSpotTab.setText("Hotspots");
	    TabItem callTreeTab = new TabItem(main, SWT.NONE);
	    callTreeTab.setText("Call Tree");
	    hotspotProfilingComposite = new ProfilingComposite(main, SWT.FLAT);
	    hotSpotTab.setControl(hotspotProfilingComposite);
	    hotSpotTab.setImage(ProfilingUiPlugin.getDefault().getImageRegistry().get(ProfilingUiPlugin.HOTSPOTS_TAB_IMG));
        callTreeProfilingComposite = new ProfilingComposite(main, SWT.NONE);
	    callTreeTab.setControl(callTreeProfilingComposite);
	    callTreeTab.setImage(ProfilingUiPlugin.getDefault().getImageRegistry().get(ProfilingUiPlugin.CALL_TREE_TAB_IMG));
        ProfilingPlugin.getDefault().addProfilingListener(profilingEventListener);
	}

    public void dispose() {
	    ProfilingPlugin.getDefault().removeProfilingListener(profilingEventListener);
	    super.dispose();
	}
	
	public void setFocus() {
		hotspotProfilingComposite.setFocus();
	}
}