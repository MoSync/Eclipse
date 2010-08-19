package com.mobilesorcery.sdk.profiling;

import java.util.List;

public interface IInvocation {

    public final static IInvocation EMPTY = new Invocation(null);

    public abstract FunctionDesc getProfiledEntity();

    public abstract IInvocation getCaller();

    public abstract List<IInvocation> getInvocations();

    public abstract float getSelfTime();
    
    public abstract float getAggregateTime();

    public abstract int getCount();

}