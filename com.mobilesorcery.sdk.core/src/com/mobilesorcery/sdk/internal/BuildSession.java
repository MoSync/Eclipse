package com.mobilesorcery.sdk.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.core.build.LinkBuildStep;
import com.mobilesorcery.sdk.core.build.PackBuildStep;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;

public class BuildSession implements IBuildSession {

    public final static int DO_CLEAN = 1 << 1;
    public final static int DO_LINK = 1 << 2;
    public final static int DO_BUILD_RESOURCES = 1 << 3;
    public final static int DO_PACK = 1 << 4;
    public final static int DO_SAVE_DIRTY_EDITORS = 1 << 5;
    public static final int ALL = DO_PACK | DO_CLEAN | DO_LINK | DO_BUILD_RESOURCES | DO_SAVE_DIRTY_EDITORS;
    
    private List<IBuildVariant> variants;
    private Map<String, Object> properties = new HashMap<String, Object>();
    private int flags;

    public BuildSession(List<IBuildVariant> variants, int flags) {
        this.variants = variants;
        this.flags = flags;
    }
    
    public List<IBuildVariant> getBuilds() {
        return variants;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public boolean doBuildResources() {
        return (flags & DO_BUILD_RESOURCES) != 0;
    }

    public boolean doClean() {
        return (flags & DO_CLEAN) != 0;
    }

    public boolean doLink() {
        return (flags & DO_LINK) != 0;
    }

    public boolean doPack() {
        return (flags & DO_PACK) != 0;
    }

    public boolean doSaveDirtyEditors() {
        return (flags & DO_SAVE_DIRTY_EDITORS) != 0;
    }

}
