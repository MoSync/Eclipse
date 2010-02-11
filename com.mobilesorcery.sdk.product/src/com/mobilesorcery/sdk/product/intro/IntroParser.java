package com.mobilesorcery.sdk.product.intro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class IntroParser extends DefaultHandler {

	public final static String LINK_TAG = "link";
	public final static String TYPE_ATTR = "type";
	public final static String HREF_ATTR = "href";	
	public static final String DESC_ATTR = "desc";
	public static final String SCOPE_ATTR = "scope";
	
	public static final String EXAMPLE_TYPE = "example";
	public static final String RECOMMENDED_READING_TYPE = "reading";
	public static final String GUIDE_TYPE = "guide";
	public static final String SCREENCAST_TYPE = "screencast";
	
	private HashMap<String, List<IntroLink>> links = new HashMap<String, List<IntroLink>>();
	
	public void startElement(String uri, String name, String qName, Attributes atts) {
		if (LINK_TAG.equals(qName)) {
			String type = atts.getValue(TYPE_ATTR);
			String href = atts.getValue(HREF_ATTR);
			String desc = atts.getValue(DESC_ATTR);
			String scope = atts.getValue(SCOPE_ATTR);
			
			if (type != null && href != null && desc != null) {
				List<IntroLink> linksForType = links.get(type);
				if (linksForType == null) {
					linksForType = new ArrayList<IntroLink>();
					links.put(type, linksForType);
				}
				
				HashSet<String> scopes = new HashSet<String>();
				if (scope == null) scope = "";
				scopes.addAll(Arrays.asList(scope.split(",")));
				IntroLink link = new IntroLink(type, href, desc, scopes);
				linksForType.add(link);
			}
		}
	}
	
	public List<IntroLink> getLinks(String type) {
		return links.get(type);
	}
}
