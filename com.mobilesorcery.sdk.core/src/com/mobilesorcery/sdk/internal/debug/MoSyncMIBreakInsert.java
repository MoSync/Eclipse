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
import org.eclipse.cdt.debug.mi.core.command.MIBreakInsert;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.cdt.debug.mi.core.output.MIOutput;

public class MoSyncMIBreakInsert extends MIBreakInsert {

	public MoSyncMIBreakInsert(String miVersion, boolean isTemporary,
			boolean isHardware, String condition, int ignoreCount, String line,
			int tid) {
		super(miVersion, isTemporary, isHardware, condition, ignoreCount, line, tid);
	}

	public MoSyncMIBreakInsert(String miVersion, String line) {
		super(miVersion, line);
	}
	
	public MIInfo getMIInfo() throws MIException {
		MoSyncMIBreakInsertInfo info = null;
		MIOutput out = getMIOutput();
		if (out != null) {
			info = new MoSyncMIBreakInsertInfo(out);

			try {
				int originalLine = parseLineFromLineInfo(getParameters()[0]);				
				info.setOriginalLine(originalLine);
			} catch (NumberFormatException e) {
				info.setOriginalLine(-1);
			}
			
			if (info.isError()) {
				throwMIException(info, out);
			}
		}
		return info;
	}

	private int parseLineFromLineInfo(String lineInfo) throws NumberFormatException {
		// Seems to be of format file:lineno, so we'll parse anything
		// beyond the last colon as an integer
		int ixLineNo = lineInfo.lastIndexOf(':');
		if (ixLineNo == -1 || lineInfo.length() == ixLineNo - 1) {
			throw new NumberFormatException();
		}
		
		return Integer.parseInt(lineInfo.substring(ixLineNo + 1).trim());
	}

}

