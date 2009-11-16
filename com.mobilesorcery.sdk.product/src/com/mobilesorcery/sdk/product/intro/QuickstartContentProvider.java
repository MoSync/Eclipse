package com.mobilesorcery.sdk.product.intro;

import java.util.List;

import org.w3c.dom.Element;

public class QuickstartContentProvider extends LinkListContentProvider {

	protected void onFetch(Element parent, String id, boolean shortList) {
		if (IntroParser.SCREENCAST_TYPE.equals(id)) {
			List<IntroLink> screencast = IntroInitializer.getInstance()
					.getLinks(IntroParser.SCREENCAST_TYPE);
			if (screencast.size() > 0) {
				createLink(screencast.get(0).getHref(), screencast.get(0)
						.getDesc(), parent);
			}
		} else if (IntroParser.GUIDE_TYPE.equals(id)) {
			List<IntroLink> guide = IntroInitializer.getInstance().getLinks(
					IntroParser.GUIDE_TYPE);
			if (guide.size() > 0) {
				createLink(guide.get(0).getHref(), guide.get(0).getDesc(),
						parent);
			}
		}
	}

	protected void createActionLink(IntroLink link, Element parent) {
		throw new UnsupportedOperationException();
	}

	protected void createMoreLink(Element parent) {
		throw new UnsupportedOperationException();
	}

	protected String getLinkType() {
		throw new UnsupportedOperationException();
	}

	protected String getLoadErrorMessage() {
		return "[Could not load quick start]";
	}
}