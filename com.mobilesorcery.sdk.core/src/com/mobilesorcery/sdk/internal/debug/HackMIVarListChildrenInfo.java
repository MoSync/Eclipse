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
package com.mobilesorcery.sdk.internal.debug;

import org.eclipse.cdt.debug.mi.core.output.MIOutput;
import org.eclipse.cdt.debug.mi.core.output.MIVar;
import org.eclipse.cdt.debug.mi.core.output.MIVarListChildrenInfo;

public class HackMIVarListChildrenInfo extends MIVarListChildrenInfo {

	private boolean didHack = false;
	private MIVar[] hackOffspring;

	public HackMIVarListChildrenInfo(MIOutput record) {
		super(record);
	}

	public MIVar[] getMIVars() {
		if (!didHack) {
			didHack = true;
			MIVar[] unhackedChildren = super.getMIVars();
			hackOffspring = new MIVar[unhackedChildren.length];
			for (int i = 0; i < unhackedChildren.length; i++) {
				hackOffspring[i] = new HackMIVar(unhackedChildren[i]);
			}
		}
		
		return hackOffspring;
	}
}
