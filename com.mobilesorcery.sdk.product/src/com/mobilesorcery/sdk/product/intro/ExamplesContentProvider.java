package com.mobilesorcery.sdk.product.intro;

import org.w3c.dom.Element;

import com.mobilesorcery.sdk.product.intro.actions.ImportExampleAction;

public class ExamplesContentProvider extends LinkListContentProvider {

	protected String getLoadErrorMessage() {
		return "[Could not load examples]";
	}

	protected void createMoreLink(Element parent) {
		createMoreLink("More examples...", "examples", parent);
	}

	protected String getLinkType() {
		return IntroParser.EXAMPLE_TYPE;
	}

	protected void createActionLink(IntroLink link, Element parent) {
		createActionLink(ImportExampleAction.class.getName(), link
				.getHref(), link.getDesc(), parent, 1);
	}
}
