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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.mobilesorcery.sdk.core.Util;

public interface IInvocation {

    public static final Comparator<IInvocation> SORT_BY_SELF_TIME = new Comparator<IInvocation>() {
        public int compare(IInvocation first, IInvocation second) {
            int result = new Float(first.getSelfTime()).compareTo(second.getSelfTime());
            if (result == 0) {
                result = System.identityHashCode(first) - System.identityHashCode(second);
            }
            
            return result;
        }        
    };
    

    public static final Comparator<IInvocation> SORT_BY_AGG_TIME = new Comparator<IInvocation>() {
        public int compare(IInvocation first, IInvocation second) {
            int result = new Float(first.getAggregateTime()).compareTo(second.getAggregateTime());
            if (result == 0) {
                result = System.identityHashCode(first) - System.identityHashCode(second);
            }
            
            return result;
        }
    };
    
    public static final Comparator<IInvocation> SORT_BY_INVOCATION_COUNT = new Comparator<IInvocation>() {
        public int compare(IInvocation first, IInvocation second) {
            int result = new Integer(first.getCount()).compareTo(second.getCount());
            if (result == 0) {
                result = System.identityHashCode(first) - System.identityHashCode(second);
            }
            
            return result;
        }
    };
    

    public static final Comparator<IInvocation> SORT_BY_FUNC_NAME = new Comparator<IInvocation>() {
        public int compare(IInvocation first, IInvocation second) {
            String firstName = first.getProfiledEntity().toString();
            String secondName = second.getProfiledEntity().toString();
            int result = firstName.compareTo(secondName);
            if (result == 0) {
                result = System.identityHashCode(first) - System.identityHashCode(second);
            }
            
            return result;
        }        
    };
    
    
    /**
     * An 'empty' invocation, or <code>null</code> invocation.
     */
    public final static IInvocation EMPTY = new Invocation(null);

    /**
     * Returns the profiled entity (such as function name, etc)
     * @return
     */
    public abstract FunctionDesc getProfiledEntity();

    /**
     * For a call tree, returns the parent invocation
     * @return
     */
    public abstract IInvocation getCaller();

    /**
     * Returns all the 'child' invocations of this invocation.
     * @return
     */
    public abstract List<IInvocation> getInvocations();

    /**
     * Returns the self time of this function, ie the time
     * <i>not</i> spent in any child invocation.
     * @return
     */
    public abstract float getSelfTime();
    
    /**
     * Returns the time spent in this invocation <i>and</i>
     * it's child invocations.
     * @return
     */
    public abstract float getAggregateTime();

    /**
     * Returns the number of times the profiled entity
     * of this invocation was accessed (executed).
     * @return
     */
    public abstract int getCount();
    
    /**
     * <p>Flattens the call tree of this invocation and returns
     * a set of invocations, sorted according to <code>comparator</code></p>
     * @param comparator A comparator used for sorting, or <code>null</code>
     * to return the list in it's natural order.
     * @return
     */
    public Collection<IInvocation> flatten(Comparator<IInvocation> comparator);

}