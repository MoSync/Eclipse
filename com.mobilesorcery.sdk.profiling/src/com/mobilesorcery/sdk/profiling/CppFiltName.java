/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.profiling;


public class CppFiltName {

    private String prefix;
    private String fullName;
    private String shortName;
    private String signature;
    private String suffix;
    private String namespace;

    private CppFiltName() {
        
    }
    
    public static CppFiltName parse(String fn) {
        CppFiltName result = new CppFiltName();
        
        // Parse stuff
        int firstParenIx = fn.indexOf('(');
        int lastSpaceIx = Math.max(0, (firstParenIx == -1 ? fn : fn.substring(0, firstParenIx)).lastIndexOf(' '));
        String prefixText = fn.substring(0, lastSpaceIx);
        
        String unmangledSymbol = fn.substring(lastSpaceIx);
        firstParenIx = unmangledSymbol.indexOf('('); 
        int secondParenIx = Math.max(unmangledSymbol.indexOf(')'), unmangledSymbol.length());
        String unmangleSymbolNoSignature = firstParenIx == -1 ? unmangledSymbol : unmangledSymbol.substring(0, firstParenIx);
        String signatureString = firstParenIx == -1 ? "" : unmangledSymbol.substring(firstParenIx, secondParenIx);
        String suffix = unmangledSymbol.substring(secondParenIx);

        int lastDoubleColonIx = unmangleSymbolNoSignature.lastIndexOf("::");
        String lastSegment = lastDoubleColonIx == -1 ? unmangleSymbolNoSignature : unmangleSymbolNoSignature.substring(lastDoubleColonIx);
        String otherSegments = unmangleSymbolNoSignature.substring(0, unmangleSymbolNoSignature.length() - lastSegment.length());
        
        result.prefix = prefixText;
        result.namespace = otherSegments;
        result.fullName = otherSegments + lastSegment;
        result.shortName = lastSegment.startsWith("::") ? lastSegment.substring(2) : lastSegment;
        result.signature = signatureString;
        result.suffix = suffix;
        return result;
    }

    /**
     * Returns any cpp-filt-specific prefix (such as 'operator')
     * @return
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns any cpp-filt-specific suffix (such as 'const')
     * @return
     */
    public String getSuffix() {
        return suffix;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public String toString() {
        return prefix + fullName + signature + suffix;
    }
}
