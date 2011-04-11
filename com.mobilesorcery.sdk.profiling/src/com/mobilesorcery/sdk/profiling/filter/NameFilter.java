package com.mobilesorcery.sdk.profiling.filter;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MergeFilter;
import com.mobilesorcery.sdk.core.ParseException;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.profiling.CppFiltName;
import com.mobilesorcery.sdk.profiling.FunctionDesc;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.filter.NameFilter.Criteria;

public class NameFilter implements IFilter<IInvocation> {

	public enum MatchType {
		EXACT_MATCH,
		REGEXP,
		CONTAINS
	}
	
	public enum Criteria {
		NAME,
		FILE
	}
	
    private Pattern pattern;
    private String match;
	private MatchType type;
	private Criteria criteria;
	private boolean include;

	public static IFilter<IInvocation> create(String match, Criteria criteria, MatchType matchType, boolean include) throws ParseException {
		String[] matchStrings = PropertyUtil.toStrings(match);
		ArrayList<IFilter<IInvocation>> result = new ArrayList<IFilter<IInvocation>>();
		for (int i = 0; i < matchStrings.length; i++) {
			result.add(new NameFilter(matchStrings[i], criteria, matchType, include));
		}
		return new MergeFilter<IInvocation>(MergeFilter.AND, result.toArray(new IFilter[0]));
	}
	
    private NameFilter(String match, Criteria criteria, MatchType matchType, boolean include) throws ParseException {
    	switch (matchType) {
    	case EXACT_MATCH:
    	case CONTAINS:
    		this.match = match;
    		break;
    	case REGEXP:
    		try {
    			pattern = Pattern.compile(match);
    		} catch (Exception e) {
    			throw new ParseException(String.format("Invalid regexp: %s", match));
    		}
    		break;
    	}
    	this.criteria = criteria;
    	this.type = matchType;
    	this.include = include;
    }
    
    public boolean accept(IInvocation obj) {
        if (obj != null && obj.getProfiledEntity() != null) {
        	String matched = getNameToMatch(obj);
            
            if (pattern != null) {
                return pattern.matcher(matched).matches();
            } else if (match != null) {
                return type == MatchType.EXACT_MATCH ?
                		matched.equalsIgnoreCase(match) :
                        // English locale will be fine for C identifiers.
                		matched.toUpperCase(Locale.ENGLISH).contains(match.toUpperCase(Locale.ENGLISH));
            }
        }
        
        boolean result =  pattern == null && match == null;
        return include ? result : !result;
    }

	private String getNameToMatch(IInvocation invocation) {
		FunctionDesc fd = invocation.getProfiledEntity();
		String matched = "";
		switch (criteria) {
		case NAME:
	        CppFiltName name = CppFiltName.parse(fd.getName());
	        matched = name.getFullName();
	        break;
		case FILE:
			matched = fd.getFileName();
			break;
		}
		
        matched = matched == null ? "" : matched;
        return matched;
	}

}
