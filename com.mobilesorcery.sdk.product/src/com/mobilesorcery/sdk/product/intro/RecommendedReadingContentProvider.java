package com.mobilesorcery.sdk.product.intro;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class RecommendedReadingContentProvider extends LinkListContentProvider {

	protected String getLoadErrorMessage() {
		return "[Could not load recommended reading]";
	}

	protected void createMoreLink(Element parent) {
		//createMoreLink("More reading...", "reading", parent);
		createBR(parent, 2);
		Text text = parent.getOwnerDocument().createTextNode("For more reading, visit ");
		parent.appendChild(text);
		createLink("http://www.mosync.com", "http://www.mosync.com", parent);
		createBR(parent, 1);
	}

	protected String getLinkType() {
		return IntroParser.RECOMMENDED_READING_TYPE;
	}

	protected void createActionLink(IntroLink link, Element parent) {
		createLink(link.getHref(), link.getDesc(), parent);
		createBR(parent, 1);
	}
}
