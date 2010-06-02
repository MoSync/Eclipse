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
package com.mobilesorcery.sdk.core;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * A simple class for handling icon and their injection for Linux 
 * platforms. At the time this was written, the icon-injector tool
 * in the MoSync source tree did have the required behavior for
 * use on Linux platforms, thus this class was written. 
 * Note: This class is dependent on the eternal tool ImageMagick.
 * 
 * @author Ali Mosavian 
 */
public class IconManager 
{
	/**
	 * Internal class that is used to keep track of icons
	 * 
	 * @author Ali Mosavian
	 */
	static class Icon 
	{
		private	int	m_width;
		private	int	m_height;
		private File m_path;
		
		/**
		 * Constructor
		 * 
		 * @param w width
		 * @param h height
		 * @param p Path to icon 
		 */
		public Icon ( int w, 
					  int h, 
					  String p )
		{
			m_width = w;
			m_height= h;
			m_path  = new File( p );			
		}
		
		
		/**
		 * Score this icon against the requested size. 
		 * The algorithm scoring algorithm works as such,
		 *  - If the size are equal, then Integer.MAX_VALUE is returned
		 *  
		 *  - If this icon is larger, a positive score is returned. 
		 *    The larger it is, the higher the score is.
		 *    
		 *  - If this icon is smaller (on any side), a negative score is 
		 *    returned. The smaller it is, the less the score will be.
		 * 
		 * @param w Requested width
		 * @param h Requested height
		 * 
		 * @return Score, higher score means better fit
		 */
		public int getScore ( int w,
						   	  int h )
		{
			int dw = w-m_width;
			int dh = h-m_height;
			int score2 = Math.abs( dw )+Math.abs( dh );
			
			if ( dw < 0 || dh < 0 )
				score2 = -score2;
			
			return (score2 == 0 ? Integer.MAX_VALUE : score2);
		}
		
		/**
		 * Returns File instance of icon
		 * 
		 * @return File object
		 */
		public File getFile ( )
		{
			return m_path;
		}
		
		/**
		 * Checks if this is SVG icon
		 * 
		 */
		public boolean isSVG ( )
		{
			return m_path.getName( ).endsWith( ".svg" );
		}
	}	

	
	
	private Map<String, List<Icon>>	m_iconMap;
	private DefaultPackager			m_internal;
	
	
	/**
	 * Constructor, will go through the project directory for any
	 * files ending with ".icon" and parse them.
	 * 
	 * @param p Used to execute commands
	 * @param b Project base directory
	 * 
	 * @throws Exception 
	 * @throws CoreException 
	 */
	public IconManager ( DefaultPackager p,
						 File b )
	throws Exception, CoreException
	{
		m_internal = p;
		m_iconMap  = new HashMap<String, List<Icon>>( );
		
		// Go through icon resources
		for ( File f : b.listFiles( ) )
		{
			if ( f.getName( ).endsWith( ".icon" ) == false )
				continue;
			
			loadIconMetaData( f );
		}
	}
	
	/**
	 * Returns wether or not an icon type exists
	 * 
	 * @param type Icon type, either "svg" of some bitmap format
	 * 			   such as PNG.
	 * 
	 * @return True on success, false otherwise
	 */
	public boolean hasIcon ( String type )
	{
		if ( type.equals( "svg" ) == true && 
			 m_iconMap.containsKey( "vector" ) == false )
			return false;
		
		else if ( m_iconMap.containsKey( "bitmap" ) == false )
			return false;
		
		
		return true;
	}	
	
	/**
	 * Will "inject", that is copy or convert the best fitting 
	 * icon to the output file. Conversion is done with the
	 * ImageMagick tool which is assumed to be in %mosync-bin%.
	 * 
	 * @param o The file where the icon will be written to.	 * 
	 * @param w The width of the output icon
	 * @param h The height of the output icon 
	 * @param type Output type, either "svg" of some bitmap format
	 * 			   such as PNG.
	 * 
	 * @return True on success, false otherwise
	 * @throws Exception If file copying fails, for instance.
	 */
	public boolean inject ( File o,
							int w,
							int h,
							String type ) 
	throws Exception
	{
		Icon ico;
		char sep = File.separatorChar;
		
		// Can't do SVG output without source SVG
		if ( type.equals( "svg" ) == true )
		{
			 if ( m_iconMap.containsKey( "vector" ) == false )
				 return false;
			 
			 ico = m_iconMap.get( "vector" ).get( 0 );			 

			 if ( ico.getFile( ).exists( ) == false )
				 return false;
			 Util.copyFile( new NullProgressMonitor( ), ico.getFile( ), o );			
		}
		else
		{
			ico = findBestMatch( w, h );
			
			// Check if size is the same
			if ( ico.getScore( w, h ) == Integer.MAX_VALUE )				
				Util.copyFile( new NullProgressMonitor( ), ico.getFile( ), o );
			else
			{	
				// Convert
				String bin = MoSyncTool.getDefault( ).getBinary( "ImageMagick/convert" ).toOSString( );
				if ( m_internal.runCommandLineWithRes( bin,
										   			   ico.getFile( ).getAbsolutePath( ), 
										   			   "-resize", w + "x" + h,
										   			   o.getAbsolutePath( ) ) != 0 )
					return false;
			}
		}
		
		
		return true;
	}
	
	
	/**
	 * This method finds the best matching bitmap icon for the 
	 * requested with and height. The resulting icon does not
	 * have to be the same size or even the same format as you
	 * think it is. So make sure you convert it to the right 
	 * size and format. 
	 * 
	 * @param w The requested with
	 * @param h The requested height.
	 * 
	 * @return A bitmap icon which is the closest to the requested
	 * 		   resolution.
	 * 
	 * @throws Exception If there aren't any icons at all.
	 */
	private Icon findBestMatch ( int w, int h )
	throws Exception
	{
		Icon icon = null;
		int score = Integer.MIN_VALUE;
		
		// SVG always has preference
		/*
		 * Unfortunately, ImageMagick doesn't seem to have proper 
		 * SVG support, so although this would have been ideal,
		 * it can not be used.
		 * 
		if ( m_iconMap.containsKey( "vector" ) )
			return m_iconMap.get( "vector" ).get( 0 );
		*/
				
		if (  m_iconMap.containsKey( "bitmap" ) == false )
			throw new Exception( "No icons" );
		
		for ( Icon i : m_iconMap.get( "bitmap" ) )
		{
			if ( i.getScore( w, h ) < score )
				continue;
			
			icon  = i;
			score = i.getScore( w, h );				
		}
		
		return icon;
	}
	
	/**
	 * Loads icon meta data from XML
	 * Note: It is assumed that the input XML(s) have the following
	 * 		 XML format
	 * 
	 * 		 <icon>
	 * 			<instance src="someicon.svg"/>
	 * 			<instance src="someicon1.png" size="w1xh1"/>
	 * 			<instance src="someicon2.bmp" size="w2xh2"/>
	 * 			.
	 * 			.
	 * 			.
	 * 			<instance src="someiconN.jpg" size="wNxhN"/>
	 * 		 </icon>
	 * 
	 * 		If multiple SVG files are defined, the first one will
	 * 		always be chosen. For bitmap formats ( non-vector ) 
	 * 		that is, the one that is closest in size to the output
	 * 		will be chosen for conversion.
	 * 
	 * @param f XML file
	 * 
	 * @throws Exception Occurs when the XML is badly formated.
	 */
	private void loadIconMetaData ( File f )
	throws Exception
	{
		SAXParser 			saxParse;
		XMLHandler 			iconParser = new XMLHandler( );
		SAXParserFactory	saxFact = SAXParserFactory.newInstance( );
		
		try 
		{			
			saxParse = saxFact.newSAXParser( );
			saxParse.parse( f, iconParser );
		} 
		catch ( Exception e ) 
		{
			throw new Exception( "Failed to parse icon xml", e );
		}
		
		// Go through icons
		for ( Entry<String, Entry<Integer, Integer>> e : iconParser.getIconList( ) )
		{	
			List<Icon> list;
			String  type = "bitmap";
			int	   	wdth = e.getValue( ).getKey( );
			int	   	hght = e.getValue( ).getValue( );
			String  path = f.getParent( ) + File.separatorChar + e.getKey( );			
			Icon 	icon = new Icon( wdth, hght, path );
			
			
			if ( icon.isSVG( ) == true )
				type = "vector";
			
			if ( m_iconMap.containsKey( "type" ) == true )
				list = m_iconMap.get( type );
			else
				list = new LinkedList<Icon>( );
			
			list.add( icon );			
			m_iconMap.put( type, list );	
		}		
	}
}


/**
 * SAX event handler for parsing icon meta XML
 * 
 * @author Ali Mosavian
 *
 */
class XMLHandler 
extends DefaultHandler
{	
	Stack<String> m_parseStack;
	List<Entry<String, Entry<Integer, Integer>>> m_iconList;
	
	
	/**
	 * Constructor
	 * Inits parse stack and other internal data structures.
	 * 
	 */
	public XMLHandler ( )
	{
		m_parseStack = new Stack<String>( );
		m_iconList   = new LinkedList<Entry<String, Entry<Integer, Integer>>>( );		
	}	
	
	
	/**
	 * Invoked by SAX parser at the start of an XML tag
	 * 
	 * @param uri The Namespace URI, or the empty string if the element 
	 * 			  has no Namespace URI or if Namespace processing is not 
	 * 			  being performed.
	 * 
	 * @param localName The local name (without prefix), or the empty string 
	 * 				    if Namespace processing is not being performed.
	 * 
	 * @param qName The qualified name (with prefix), or the empty string if 
	 * 				qualified names are not available.
	 * 
	 * @param attributes The attributes attached to the element. If there are 
	 * 					 no attributes, it shall be an empty Attributes object.
	 * 
	 * @exception RuntimeException Occurs if the XML isn't properly formated.
	 */
	public void startElement( String uri, 
							  String localName, 
							  String qName, 
							  Attributes atts )
	{		
		// New icon ?
		if ( qName.equals( "instance" ) == true )
		{
			if ( m_parseStack.empty( ) == false && 
				 m_parseStack.peek( ).equals( "icon" ) == true  )
			{
				int	w = 0;
				int h = 0;
				String size = "";
				String path = "";
				Entry<Integer, Integer> iconSize;				
				
				// Parse attributes
				for ( int i = 0; i < atts.getLength( ); i++ )
				{
					String n = atts.getQName( i );
					if ( n.equals( "size" ) == true )
						size = atts.getValue( i );
					else if ( n.equals( "src" ) == true )
						path = atts.getValue( i );						
				}
				
				// Semantic check
				if ( path.isEmpty( ) == true )
					throw new RuntimeException( "Badly formated icon xml: no src" );
				
				if ( size.isEmpty( ) == true && 
					 path.endsWith( ".svg" ) == false )
					throw new RuntimeException( "Badly formated icon xml: src, but no size" );
				
				// Parse size
				if ( size.isEmpty( ) == false )
				{
					if ( size.equalsIgnoreCase( "default" ) == false )
					{
						String[] tmp = size.split( "x" );				
						if ( tmp.length != 2 )
							throw new RuntimeException( "Badly formated icon xml: bad size format" );
						
						try {
							w = Integer.parseInt( tmp[0] );
							h = Integer.parseInt( tmp[1] );					
						} catch ( NumberFormatException e ) {
							throw new RuntimeException( "Badly formated icon xml, size attribute", e );
						}					
					}
				}

				// Add icon to list
				iconSize = new SimpleEntry<Integer, Integer>( w, h );
				m_iconList.add( new SimpleEntry<String, Entry<Integer,Integer>>( path, iconSize ) );
			}				
		}

		m_parseStack.push( qName );
	}

	
	/**
	 * Invoked by SAX parser at the end of an XML tag.
	 * 
	 * @param uri The Namespace URI, or the empty string if the element 
	 * 			  has no Namespace URI or if Namespace processing is not 
	 * 			  being performed.
	 * 
	 * @param localName The local name (without prefix), or the empty string 
	 * 				    if Namespace processing is not being performed.
	 * 
	 * @param qName The qualified name (with prefix), or the empty string if 
	 * 				qualified names are not available. 
	 */
	public void endElement ( String uri, 
							 String localName, 
							 String qName )
	{
		if ( m_parseStack.empty( ) == true )
			return;

		if ( m_parseStack.peek( ).equals( qName ) == true )
			m_parseStack.pop( );
	}
	
	
	/**
	 * Returns icon list
	 * 
	 * @return Every entry is <Path, <Width, Height>>
	 */
	public List<Entry<String, Entry<Integer, Integer>>> getIconList ( )
	{
		return m_iconList;
	}
	
		
}
