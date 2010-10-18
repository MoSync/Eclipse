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

public class FunctionDesc {

    public static final int NO_ADDR = -1;
    
    private int addr;
    private String name;

    private String toString;

	private String filename;

	private int lineno;

    public FunctionDesc(int addr) {
        init(addr, null, null, NO_ADDR);
    }

    public FunctionDesc(String name) {
        init(NO_ADDR, name, null, NO_ADDR);
    }
    
    public FunctionDesc(int addr, String name) {
        init(addr, name, null, NO_ADDR);
    }

    public FunctionDesc(int functionAddr, String functionName, String filename,
			int lineno) {
    	init(functionAddr, functionName, filename, lineno);
	}

	private void init(int addr, String name, String filename, int lineno) {
        this.addr = addr;
        this.name = name;
        this.filename = filename;
        this.lineno = lineno;
    }
    
    public int getAddr() {
        return addr;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFileName() {
    	return filename;
    }
    
    public int getLineNumber() {
    	return lineno;
    }
    
    public boolean equals(Object o) {
        return (o instanceof FunctionDesc) ? toString().equals(o.toString()) : null;
    }
    
    public int hashCode() {
        return toString().hashCode();
    }
    
    /**
     * Returns a user-understandable string representing this <code>FunctionDesc</code>.
     */
    public String toString() {
        if (toString == null) {
            toString = internalToString();
        }
        
        return toString;
    }
    
    public String internalToString() {
        return name == null ? "0x" + Integer.toHexString(addr) : name;
    }
}
