package com.mobilesorcery.sdk.profiling.ui.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingListener;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;

public class ProfilingView extends ViewPart {
    
	public class ProfilingListener implements IProfilingListener {
        public void handleEvent(ProfilingEventType eventType, final IProfilingSession session) {
            if (profileTree != null && eventType == ProfilingEventType.STOPPED) {
                profileTree.getControl().getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        profileTree.setInput(session.getInvocation());
                    }                    
                });
            }
        }
    }

	class ProfilingContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		
        public Object[] getChildren(Object parentElement) {
            return ((IInvocation) parentElement).getInvocations().toArray();
        }

        public Object getParent(Object element) {
            return ((IInvocation) element).getCaller();
        }

        public boolean hasChildren(Object element) {
            return getChildren(element).length != 0;
        }

        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }
	}
	
	class ProfilingLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}
	
    private TreeViewer profileTree;
    private ProfilingListener profilingEventListener;

	public ProfilingView() {
	    profilingEventListener = new ProfilingListener();
	}
	
	public void createPartControl(Composite parent) {
	    profileTree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	    profileTree.setContentProvider(new ProfilingContentProvider());
	    profileTree.setLabelProvider(new ProfilingLabelProvider());
		profileTree.setInput(IInvocation.EMPTY);
		
		ProfilingPlugin.getDefault().addProfilingListener(profilingEventListener);
	}
	
	public void dispose() {
	    ProfilingPlugin.getDefault().removeProfilingListener(profilingEventListener);
	    super.dispose();
	}
	
	public void setFocus() {
		profileTree.getControl().setFocus();
	}
}