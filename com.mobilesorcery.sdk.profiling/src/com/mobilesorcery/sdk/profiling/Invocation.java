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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Invocation implements IInvocation {

    private FunctionDesc profiledEntity;
    private IInvocation caller;
    private List<IInvocation> invocations = new ArrayList<IInvocation>();
    private float selfTime;
    private float aggregateTime;

    private int count;

    public Invocation(IInvocation caller) {
        this.caller = caller;
    }

    public FunctionDesc getProfiledEntity() {
        return profiledEntity;
    }
    
    public void setProfiledEntity(FunctionDesc profiledEntity) {
        this.profiledEntity = profiledEntity;
    }

    public IInvocation getCaller() {
        return caller;
    }
    
    public List<IInvocation> getInvocations() {
        return invocations;
    }
    
    public float getSelfTime() {
        return selfTime;
    }
    
    public void setSelfTime(float selfTime) {
        this.selfTime = selfTime;
    }
    
    public float getAggregateTime() {
        return aggregateTime;
    }

    public void setAggregateTime(float aggregateTime) {
        this.aggregateTime = aggregateTime;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public void addInvocation(IInvocation callee) {
        invocations.add(callee);
    }
    
    public String toString() {
        return String.format("%s - %d calls - %d (ms, self time)", profiledEntity, count, (int) Math.round(1000 * selfTime));
    }

    public Collection<IInvocation> flatten(Comparator<IInvocation> comparator) {
        List<IInvocation> flattenedInvocations = flattenNaturally();
        Collection<IInvocation> result = flattenedInvocations;
        if (comparator != null) {
            result = new TreeSet(comparator);
            result.addAll(flattenedInvocations);
        }
        
        return result;
    }
    
    private List<IInvocation> flattenNaturally() {
        ArrayList<IInvocation> unaggregated = new ArrayList<IInvocation>();
        for (IInvocation invocation : invocations) {
            unaggregated.add(invocation);
            unaggregated.addAll(invocation.flatten(null));
        }
        
        return unaggregated;
    }

    /**
     * <p>Returns a flat invocation (with no parent nor any childen)
     * with the aggregated counts, self time, etc of the input
     * arguments.</p>
     * <p>The profiled entity will be set to the profiled entity of
     * the first invocation.</p>
     * @param invocations
     * @return
     */
    public static Invocation aggregate(IInvocation... invocations) {
        Invocation result = new Invocation(null);
        for (int i = 0; i < invocations.length; i++) {
            IInvocation invocation = invocations[i];
            result.aggregateTime += invocation.getAggregateTime();
            result.count += invocation.getCount();
            result.selfTime += invocation.getSelfTime();
            result.profiledEntity = invocations[0].getProfiledEntity();
        }
        
        return result;
    }

}
