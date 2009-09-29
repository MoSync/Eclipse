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

