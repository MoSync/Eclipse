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
import java.util.List;

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
}
