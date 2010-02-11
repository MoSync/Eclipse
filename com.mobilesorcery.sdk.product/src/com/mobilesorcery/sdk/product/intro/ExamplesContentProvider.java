package com.mobilesorcery.sdk.product.intro;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.mobilesorcery.sdk.product.intro.actions.ImportExampleAction;

public class ExamplesContentProvider extends LinkListContentProvider {

	protected String getLoadErrorMessage() {
		return "[Could not load examples]";
	}

	protected void createMoreLink(Element parent) {
		//createMoreLink("More examples...", "examples", parent);
		createBR(parent, 2);
		Text text = parent.getOwnerDocument().createTextNode("For more examples, visit ");
		parent.appendChild(text);
		createLink("http://www.mosync.com", "http://www.mosync.com", parent);
		createBR(parent, 1);
	}

	protected String getLinkType() {
		return IntroParser.EXAMPLE_TYPE;
	}

	protected void createActionLink(IntroLink link, Element parent) {
		createActionLink(ImportExampleAction.class.getName(), link
				.getHref(), link.getDesc(), parent, 1);
	}
}
