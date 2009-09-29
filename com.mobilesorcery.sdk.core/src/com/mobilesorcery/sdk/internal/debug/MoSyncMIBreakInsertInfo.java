package com.mobilesorcery.sdk.internal.debug;

import org.eclipse.cdt.debug.mi.core.output.MIBreakInsertInfo;
import org.eclipse.cdt.debug.mi.core.output.MIOutput;

public class MoSyncMIBreakInsertInfo extends MIBreakInsertInfo {

	private int line;

	public MoSyncMIBreakInsertInfo(MIOutput record) {
		super(record);
	}

	public void setOriginalLine(int line) {
		this.line = line;
	}
	
	public int getOriginalLine() {
		return line;
	}
}
