package com.mobilesorcery.sdk.internal.debug;

import java.io.IOException;

import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MIProcessAdapter;
import org.eclipse.cdt.debug.mi.core.command.CLIInfoThreads;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MIBreakInsert;
import org.eclipse.cdt.debug.mi.core.command.MIExecInterrupt;
import org.eclipse.cdt.debug.mi.core.command.MIExecNext;
import org.eclipse.cdt.debug.mi.core.command.MIExecRun;
import org.eclipse.cdt.debug.mi.core.command.MIExecStep;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSet;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class MoSyncCommandFactory extends CommandFactory {

	public MoSyncCommandFactory(String miVersion) {
		super(miVersion);
	}
	
	public MIProcess createMIProcess(String[] args, int launchTimeout, IProgressMonitor monitor) throws IOException {
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("MDB command line: " + Util.join(Util.ensureQuoted(args), " "));		
		}
				
		return new MoSyncMIProcessAdapter(args, launchTimeout, monitor);
	}
	
	public MIExecInterrupt createMIExecInterrupt() {
		return new MIExecInterrupt(getMIVersion());
	}

	public CLIInfoThreads createCLIInfoThreads() {
		// Temporary.
		return new CLIInfoThreads() {
			public String getOperation() {
				return "-thread-info";
			}
			
			public String toString(){
				String str = getToken() + getOperation(); //$NON-NLS-1$
				if (str.endsWith("\n")) //$NON-NLS-1$
					return str;
				return str + "\n"; //$NON-NLS-1$
			}
		};
	}

	public MIExecStep createMIExecStep(int count) {
		// MDB prefers no-args to -exec-step
		if (count != 1) {
			return super.createMIExecStep(count);
		} else {
			return new MIExecStep(getMIVersion());
		}
	}
	
	public MIExecNext createMIExecNext(int count) {
		// Same as for MIExecStep goes here, no-args to -exec-next
		if (count != 1) {
			return super.createMIExecNext(count);
		} else {
			return new MIExecNext(getMIVersion());
		}
	}
	
	public MIExecRun createMIExecRun(String[] args) {
		return new MIExecRun(getMIVersion(), args) {
			public String getOperation() {
				return "-exec-continue";
			}
		};
	}
	
	public MIBreakInsert createMIBreakInsert(boolean isTemporary, boolean isHardware,
			 String condition, int ignoreCount, String line, int tid) {
		return new MoSyncMIBreakInsert(getMIVersion(), isTemporary, isHardware, condition, ignoreCount, line, tid);
	}

	
}
