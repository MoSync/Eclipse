package com.mobilesorcery.sdk.internal.launch;

import java.util.Map;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;

/**
 * <p>
 * A class that we may remove once a proper process destroy is in place - this
 * process factory just signals to eclipse that we cannot terminate the
 * emulator.
 * </p>
 * <p>(Make sure to remove this line from MoreLaunchShortCut:
 * wc.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID,
 * "cm.mobilesorcery.sdk.builder.nonterminableprocessfactory");)</p>
 * 
 * @author Mattias
 * 
 */
public class NonTerminableProcessFactory implements IProcessFactory {

    public class NonTerminableProcess extends RuntimeProcess {
        
        public NonTerminableProcess(ILaunch launch, Process process, String name, Map attributes) {
            super(launch, process, name, attributes);
        }

        public boolean canTerminate() {
            return false;
        }
        
    }
    

    public IProcess newProcess(ILaunch launch, Process process, String label, Map attributes) {
        return new NonTerminableProcess(launch, process, label, attributes);
    }

}
