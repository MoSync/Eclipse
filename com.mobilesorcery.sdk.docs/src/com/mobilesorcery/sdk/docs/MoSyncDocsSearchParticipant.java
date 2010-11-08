package com.mobilesorcery.sdk.docs;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.search.ISearchIndex;
import org.eclipse.help.search.LuceneSearchParticipant;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;

public class MoSyncDocsSearchParticipant extends LuceneSearchParticipant {

	private HashSet allDocs = null;

	public IStatus addDocument(ISearchIndex index, String pluginId,
			String name, URL url, String id, Document doc) {
		return index.addDocument(pluginId, name, url, id, doc);
	}

	public Set getAllDocuments(String locale) {
		if (allDocs == null) {
			allDocs = new HashSet<String>();
			String path = super.resolveVariables(MoSyncDocsActivator.PLUGIN_ID,
					"docs/html", locale);
			Enumeration docs = Platform.getBundle("com.mobilesorcery.sdk.help")
					.getEntryPaths(path);
			if (docs != null) {
				while (docs.hasMoreElements()) {
					String doc = (String) docs.nextElement();
					String id = createId(doc);
					String url = "/com.mobilesorcery.sdk.help/" + doc;
					allDocs.add(url + "?id=" + id);
				}
			} else {
				CoreMoSyncPlugin.getDefault().getLog().log(
						new Status(IStatus.WARNING, MoSyncDocsActivator.PLUGIN_ID, "Could not find MoSync help bundle"));
			}
		}
		return allDocs;
	}

	private String createId(String name) {
		return Util.replaceExtension(new Path(name).lastSegment(), "");
	}
	
	public boolean open(String id) {
		// TODO: Argh, lousy support for integrating with help system - use build script instead?
		// Also, is there a bug in eclipse - it ignores whatever is returned here!
		PlatformUI.getWorkbench().getHelpSystem().displayHelpResource("/com.mobilesorcery.sdk.help/docs/html/" + id + ".html");
		return false;
	}
	
	public Set getContributingPlugins() {
		HashSet<String> result = new HashSet<String>();
		// TODO: Should we allow platforms to contribute?
		// This is a workaround since eclipse just refused
		// to recognize this class in com.mobilesorcery.sdk.help!!!!
		// Quicker to just create a new plugin. For now.
		result.add("com.mobilesorcery.sdk.help");
		result.add(MoSyncDocsActivator.PLUGIN_ID);
		return result;
	}

	public void clear() {
		allDocs = null;
	}
}
