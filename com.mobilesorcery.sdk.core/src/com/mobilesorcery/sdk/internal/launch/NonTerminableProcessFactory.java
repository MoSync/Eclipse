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
