package com.mobilesorcery.sdk.core;

public interface IDeltaResourceSet {

    public abstract IResourceSet getDeletedResources();

    public abstract IResourceSet getChangedOrAddedResources();

}