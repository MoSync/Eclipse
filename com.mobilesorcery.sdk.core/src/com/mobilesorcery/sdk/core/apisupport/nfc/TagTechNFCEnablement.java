package com.mobilesorcery.sdk.core.apisupport.nfc;

import java.util.ArrayList;
import java.util.List;

public class TagTechNFCEnablement implements INFCEnablement {

	private final ArrayList<String> techs;

	public TagTechNFCEnablement(ArrayList<String> techs) {
		this.techs = techs;
	}

	public List<String> getTechList() {
		return techs;
	}

}
