package com.mobilesorcery.sdk.profiling.filter;

import java.util.regex.Pattern;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.profiling.CppFiltName;
import com.mobilesorcery.sdk.profiling.IInvocation;

public class NameFilter implements IFilter<IInvocation> {

    private Pattern pattern;
    private String match;

    public NameFilter(String match, boolean regexp) {
        if (regexp) {
            pattern = Pattern.compile(match);
        } else {
            this.match = match;
        }
    }
    
    public boolean accept(IInvocation obj) {
        if (obj != null && obj.getProfiledEntity() != null) {
            CppFiltName name = CppFiltName.parse(obj.getProfiledEntity().getName());
            if (pattern != null) {
                return pattern.matcher(name.getFullName()).matches();
            } else {
                return name.getFullName().equalsIgnoreCase(match);
            }
        }
        
        return false;
    }

}
