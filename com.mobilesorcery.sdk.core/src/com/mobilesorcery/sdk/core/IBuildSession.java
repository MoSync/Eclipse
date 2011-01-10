package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.core.build.LinkBuildStep;
import com.mobilesorcery.sdk.core.build.PackBuildStep;
import com.mobilesorcery.sdk.core.build.ResourceBuildStep;

/**
 * Represents one build execution, but may span several individual build variants.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IBuildSession {

    /**
     * Returns the list of variants to be built during this session
     * @return
     */
    public List<IBuildVariant> getBuilds();
    
    /**
     * Returns a <emph>modifiable</emph> set of properties for
     * this build session. The intention of this method is to provide
     * a means to share data that last only during the session.
     * @return
     */
    public Map<String, Object> getProperties();
    
    /**
     * Returns <code>true</code> if a clean should be performed before
     * the build
     * @deprecated Not really deprecated, but move this method
     */
    public boolean doClean();
    
    /**
     * Returns <code>true</code> if linking should be performed
     * @deprecated Not really deprecated, but move this method
     */
    public boolean doLink();
 
    /**
     * Returns <code>true</code> if packaging should be performed
     * @deprecated Not really deprecated, but move this method
     */
    public boolean doPack();
    
    /**
     * Returns <code>true</code> if a resources should be built
     * @deprecated Not really deprecated, but move this method
     */
    public boolean doBuildResources();
    
    /**
     * Returns <code>true</code> if a dirty editors should savedc
     * @deprecated Not really deprecated, but move this method
     */
    public boolean doSaveDirtyEditors();
    
}
