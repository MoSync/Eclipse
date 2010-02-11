/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
/**
 * 
 */
package com.mobilesorcery.sdk.core;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * <p>A simple filter class. Clients need to implement
 * the <code>accept</code> method, but there are other utility methods
 * there as well.</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 * @param <T> The class this <code>Filter</code> is applicable to. 
 */
public abstract class Filter<T> {
    class InverseFilter extends Filter<T> {
        private Filter<T> wrappedFilter;

        public InverseFilter(Filter<T> wrappedFilter) {
            this.wrappedFilter = wrappedFilter;
        }
        
        public boolean accept(T obj) {
            return !wrappedFilter.accept(obj);
        }
    }

    /**
     * <p>A method determining whether to filter out
     * this object</p>
     * @param obj The object to filter
     * @return <code>true</code> if the object is
     * accepted by this filter, <code>false</code>
     * if it should be filtered out.
     */
    public abstract boolean accept(T obj);
    
    /**
     * <p>Returns an inverse filter of this one.</p>
     * The invariant is that for every object X and filter
     * F, F.accept(X) != F.inverse().accept(X) holds true.</p>
     * 
     * @return An inverse filter
     */
    public Filter<T> inverse() {
        return new InverseFilter(this);
    }
    
    /**
     * <p>A utility method for filtering an entire map;
     * the filtering is done with respect to the keys
     * of the map.</p>
     * @param <E> The type of the map's keys
     * @param <F> The type of the map's values
     * @param map The map to filter
     * @param filter The filter to use
     * @return A map, the keys of which are all accepted by <code>filter</code>. 
     */
    public static <E extends Comparable, F> Map<E, F> filterMap(Map<E, F> map, Filter<E> filter) {
        Map<E, F> result = new TreeMap<E, F>();
        for (Iterator<Entry<E, F>> allIterator = map.entrySet().iterator(); allIterator.hasNext(); ) {
            Entry<E, F> entry = allIterator.next();
            if (filter.accept(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
}