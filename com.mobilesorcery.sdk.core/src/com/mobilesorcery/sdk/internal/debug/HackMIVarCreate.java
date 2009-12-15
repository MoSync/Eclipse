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

import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.command.MIVarCreate;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.cdt.debug.mi.core.output.MIOutput;
import org.eclipse.cdt.debug.mi.core.output.MIVarCreateInfo;

public class HackMIVarCreate extends MIVarCreate {

	public HackMIVarCreate(String miVersion, String expression) {
		super(miVersion, "-", "*", expression); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public HackMIVarCreate(String miVersion, String name, String expression) {
		super(miVersion, name, "*", expression); //$NON-NLS-1$
	}

	public HackMIVarCreate(String miVersion, String name, String frameAddr,
			String expression) {
		super(miVersion, name, frameAddr, expression);
	}
	
	public MIInfo getMIInfo() throws MIException {
		MIInfo info = null;
		MIOutput out = getMIOutput();
		if (out != null) {
			info = new HackMIVarCreateInfo(out);
			if (info.isError()) {
				throwMIException(info, out);
			}
		}
		return info;
	}

}
