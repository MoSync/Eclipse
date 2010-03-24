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
package com.mobilesorcery.sdk.core;


/**
 * Merges two or more <code>IResourceFilter</code>s.
 * Basically a substitute for common list operations found
 * in better languages (flame bait...)
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public class MergeFilter<T> implements IFilter<T> {

    public final static int OR = 1;
    public final static int AND = 2;
    private IFilter<T>[] filters;
    private int operation;
    
    public MergeFilter(int operation, IFilter<T>... filters) {
        this.operation = operation;
        this.filters = filters;
    }
    
    public boolean accept(T obj) {
        boolean result = operation == AND;
        
        for (int i = 0; i < filters.length; i++) {
            switch (operation) {
            case OR:
                if (filters[i].accept(obj)) {
                    return true;
                }
                break;
            case AND:
                if (!filters[i].accept(obj)) {
                    return false;
                }
                break;
            }
        }

        return result;
    }

}
