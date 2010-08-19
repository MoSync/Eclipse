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
package com.mobilesorcery.sdk.profiling.internal;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mobilesorcery.sdk.profiling.FunctionDesc;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.Invocation;

class ProfilingDataParserHandler extends DefaultHandler {

    final static String FUNC_TAG = "f";
    final static String FUNC_ADDR_ATTR = "a";
    final static String FUNC_NAME_ATTR = "n";
    final static String AGG_TIME_ATTR = "t";
    final static String SELF_TIME_ATTR = "lt";
    final static String INVOCATION_COUNT_ATTR = "c";
    
    private Invocation current;

    public ProfilingDataParserHandler(Invocation root) {
        this.current = root;
    }
    
    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (FUNC_TAG.equals(name)) {
            Invocation invocation = new Invocation(current);
            String functionName = atts.getValue(FUNC_NAME_ATTR);
            String functionAddrStr = atts.getValue(FUNC_ADDR_ATTR);
            int functionAddr = parseAddr(functionAddrStr);
            FunctionDesc fd = new FunctionDesc(functionAddr, functionName);
            int count = parseInt(atts.getValue(INVOCATION_COUNT_ATTR), 0);
            float selfTime = parseFloat(atts.getValue(SELF_TIME_ATTR), 0);
            float aggTime = parseFloat(atts.getValue(AGG_TIME_ATTR), 0);
            
            invocation.setProfiledEntity(fd);
            invocation.setCount(count);
            invocation.setSelfTime(selfTime);
            invocation.setAggregateTime(aggTime);
            
            current.addInvocation(invocation);
            current = invocation;
        }
    }
    
    private float parseFloat(String value, float invalidValue) {
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                // Return default
            }
        }
        
        return invalidValue;
    }

    private int parseInt(String value, int invalidValue) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Return default
            }
        }
        
        return invalidValue;
    }

    private int parseAddr(String addrStr) {
        if (addrStr != null && addrStr.startsWith("0x")) {
            try { 
                return Integer.parseInt(addrStr.substring(2), 16);
            } catch (NumberFormatException e) {
                // return no addr
            }
        }
        
        return FunctionDesc.NO_ADDR;
    }

    public void endElement(String uri, String name, String qName) throws SAXException {
        if (FUNC_TAG.equals(name)) {
            current = (Invocation) current.getCaller();
        }
    }
}
