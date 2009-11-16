package com.mobilesorcery.sdk.product.intro;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IntroLink {

	public static final String SHORT_LIST = "short";
	public static final String FULL_LIST = "full";
	
	private String type;
	private String href;
	private String desc;
	private Set<String> scopes;

	public IntroLink(String type, String href, String desc, HashSet<String> scopes) {
		this.type = type;
		this.href = href;
		this.desc = desc;
		this.scopes = Collections.unmodifiableSet(scopes);
	}
	
	public String getType() {
		return type;
	}
	
	public String getHref() {
		return href;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public Set<String> getScopes() {
		return scopes;
	}
}
