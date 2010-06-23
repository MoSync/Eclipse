package com.mobilesorcery.sdk.product.intro;

import java.io.File;
import java.io.PrintWriter;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.w3c.dom.Element;

import com.mobilesorcery.sdk.core.MoSyncTool;

public class RecentProjectsContentProvider extends LinkContentProvider {

	private static final int MAX_WS_COUNT = 3;

	public void createContent(String id, Element parent) {
		ChooseWorkspaceData data = new ChooseWorkspaceData("");
		String[] recentWS = data.getRecentWorkspaces();
		int wsCount = 0;

		addBreak(parent);
		
		createWorkspaceLink(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString(), parent);
		
		for (int i = 0; i < recentWS.length; i++) {
			if (recentWS[i] != null && wsCount < MAX_WS_COUNT && !isExampleWorkspace(recentWS[i]) && !isCurrent(recentWS[i])) {
				createWorkspaceLink(recentWS[i], parent);
				wsCount++;
			}
		}

		// Always add the example workspace at the bottom.
		if (wsCount > 0) {
			addBreak(parent);
		}
		createWorkspaceLink(getExampleWorkspace().getAbsolutePath(), parent);

	}

	private void addBreak(Element parent) {
		Element br = parent.getOwnerDocument().createElement("br");
		parent.appendChild(br);	
	}
	
	private void createWorkspaceLink(String ws, Element parent) {
	    if (isCurrent(ws)) {
            // Just close the welcome screen
            createActionLink("com.mobilesorcery.sdk.product.intro.actions.CloseIntroAction", ws, getWorkspaceName(ws), parent, 1);    
	    } else {
            createActionLink("com.mobilesorcery.sdk.product.intro.actions.SwitchWorkspaceAction", ws, getWorkspaceName(ws), parent, 1);    
	    }
	}


	private boolean isCurrent(String ws) {
        boolean isCurrent = ResourcesPlugin.getWorkspace().getRoot().getLocation().equals(new Path(ws));
        return isCurrent;
    }

    private String getWorkspaceName(String ws) {
		if (isExampleWorkspace(ws)) {
			return "Example Workspace";
		} else {
		    Path wsPath = new Path(ws);
		    String name = wsPath.lastSegment();
		    if ("workspace".equals(name)) {
		        // A very common name, then we'll try to use the
		        // second to last segment instead.
		        name = wsPath.segment(Math.max(0, wsPath.segmentCount() - 2));
		    }
		    
			return name + (isCurrent(ws) ? " (Current)" : "");
		}
	}

	private File getExampleWorkspace() {
		return MoSyncTool.getDefault().getMoSyncExamplesWorkspace().toFile();	
	}
	
	private boolean isExampleWorkspace(String ws) {
		File wsFile = new File(ws);
		File exampleWSFile = getExampleWorkspace(); 	
		return wsFile.equals(exampleWSFile);
	}

	public void createContent(String id, PrintWriter out) {
	}

	public void dispose() {
	}

	public void init(IIntroContentProviderSite site) {
	}

	public void createContent(String id, Composite parent, FormToolkit toolkit) {
	}

}
