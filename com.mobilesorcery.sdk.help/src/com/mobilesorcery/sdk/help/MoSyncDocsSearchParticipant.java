package com.mobilesorcery.sdk.help;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.search.IHelpSearchIndex;
import org.eclipse.help.search.ISearchDocument;
import org.eclipse.help.search.SearchParticipant;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;

public class MoSyncDocsSearchParticipant extends SearchParticipant
{
	private HashSet<String> m_allDocs = null;

	public MoSyncDocsSearchParticipant()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public IStatus addDocument(IHelpSearchIndex index, String pluginId,
			String name, URL url, String id, ISearchDocument doc)
	{
		return index.addSearchableDocument( pluginId, name, url, id, doc );
	}

	public Set<String> getAllDocuments(String locale)
	{
		if(m_allDocs != null)
		{
			return m_allDocs;
		}
		
		m_allDocs = new HashSet<String>( );
		String path = super.resolveVariables( MoSyncDocsActivator.PLUGIN_ID,
				"docs/html", locale );
		
		@SuppressWarnings("unchecked")
		Enumeration<String> docs = Platform.getBundle( "com.mobilesorcery.sdk.help" )
				.getEntryPaths( path );

		if( docs != null )
		{
			while( docs.hasMoreElements( ) )
			{
				String doc = docs.nextElement( );
				
				// We are only interested in the html documents
				if( !doc.endsWith( ".html" ) )
				{
					continue;
				}
				
				String id = createId( doc );
				String url = "/com.mobilesorcery.sdk.help/" + doc;
				m_allDocs.add( url + "?id=" + id );
			}
		}
		else
		{
			CoreMoSyncPlugin.getDefault( ).getLog( ).log(
					new Status( IStatus.WARNING, MoSyncDocsActivator.PLUGIN_ID, "Could not find MoSync help bundle" ) );
		}
		
		return m_allDocs;
	}

	private String createId(String name)
	{
		return Util.replaceExtension( new Path( name ).lastSegment( ), "" );
	}
	
	public boolean open(String id)
	{
		// TODO: Argh, lousy support for integrating with help system - use build script instead?
		// Also, is there a bug in eclipse - it ignores whatever is returned here!
		PlatformUI.getWorkbench( )
			.getHelpSystem( )
			.displayHelpResource( "/com.mobilesorcery.sdk.help/docs/html/" + id + ".html" );
		
		return true;
	}
	
	public Set<String> getContributingPlugins()
	{
		HashSet<String> result = new HashSet<String>( );
		// TODO: Should we allow platforms to contribute?
		result.add( MoSyncDocsActivator.PLUGIN_ID );
		
		return result;
	}

	public void clear()
	{
		m_allDocs = null;
	}
}
