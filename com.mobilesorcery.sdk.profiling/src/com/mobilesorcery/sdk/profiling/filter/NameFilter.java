package com.mobilesorcery.sdk.profiling.filter;

import java.util.Locale;
import java.util.regex.Pattern;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.profiling.CppFiltName;
import com.mobilesorcery.sdk.profiling.IInvocation;

public class NameFilter implements IFilter<IInvocation> {

	public enum Type {
		EXACT_MATCH,
		REGEXP,
		CONTAINS
	}
	
    private Pattern pattern;
    private String match;
	private Type type;

    public NameFilter(String match, Type matchType) {
    	switch (matchType) {
    	case EXACT_MATCH:
    	case CONTAINS:
    		this.match = match;
    		break;
    	case REGEXP:
    		pattern = Pattern.compile(match);
    		break;
    	}
    	this.type = matchType;
    }
    
    public boolean accept(IInvocation obj) {
        if (obj != null && obj.getProfiledEntity() != null) {
            CppFiltName name = CppFiltName.parse(obj.getProfiledEntity().getName());
            String fullName = name.getFullName();
            fullName = fullName == null ? "" : fullName;
            
            if (pattern != null) {
                return pattern.matcher(fullName).matches();
            } else if (match != null) {
                return type == Type.EXACT_MATCH ?
                		fullName.equalsIgnoreCase(match) :
                        // English locale will be fine for C identifiers.
                		fullName.toUpperCase(Locale.ENGLISH).contains(match.toUpperCase(Locale.ENGLISH));
            }
        }
        
        return pattern == null && match == null;
    }

}
