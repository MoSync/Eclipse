package com.mobilesorcery.sdk.product.intro;

import org.w3c.dom.Element;

public class RecommendedReadingContentProvider extends LinkListContentProvider {

	protected String getLoadErrorMessage() {
		return "[Could not load recommended reading]";
	}

	protected void createMoreLink(Element parent) {
		createMoreLink("More reading...", "reading", parent);
	}

	protected String getLinkType() {
		return IntroParser.RECOMMENDED_READING_TYPE;
	}

	protected void createActionLink(IntroLink link, Element parent) {
		createHelpLink(link.getDesc(), link.getHref(), parent);
		createBR(parent, 1);
	}
}
