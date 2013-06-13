package com.mobilesorcery.sdk.builder.android.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.MITTYAdapter;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.builder.android.Activator;
import com.mobilesorcery.sdk.builder.android.AndroidPackager;
import com.mobilesorcery.sdk.builder.android.NdkToolchain;
import com.mobilesorcery.sdk.builder.android.PropertyInitializer;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.internal.debug.MoSyncDebugger;

public class AndroidNDKDebugger extends MoSyncDebugger {
	
	private String serialNumber;

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	protected IPath getGDBPath(ILaunch launch) throws CoreException {
		NdkToolchain toolchain = Activator.getDefault().getPreferredNdkToolchain();
		if (toolchain == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No ndk toolchain found!"));
		}
		IPath gdb = toolchain.getTool(null, "gdb");
		return gdb;
	}
	
	protected String[] getGDBCommandLine(String gdb, String[] extraArgs, File executable, IProject project, IBuildVariant variant, CommandFactory factory, boolean usePty) throws CoreException {
		gdb = getGDBPath(null).toOSString();	
		
		IMITTY pty = null;

		if (usePty) {
			try {
				PTY pseudo = new PTY();
				pty = new MITTYAdapter(pseudo);
			} catch (IOException e) {
				// Should we not print/log this ?
			}
		}

		ArrayList<String> argList = new ArrayList<String>(extraArgs.length + 8);
		argList.add(gdb);
		argList.add("-q"); //$NON-NLS-1$
		argList.add("-nw"); //$NON-NLS-1$
		argList.add("-i"); //$NON-NLS-1$
		argList.add(factory.getMIVersion());
		if (pty != null) {
			argList.add("-tty"); //$NON-NLS-1$
			argList.add(pty.getSlaveName());
		}
		return argList.toArray(new String[argList.size()]);
	}
	
	protected Session createSession(IBuildVariant variant, int sessionType, String gdb, CommandFactory factory, File program, String[] extraArgs, boolean usePty, IProject project, IProgressMonitor monitor) throws IOException, MIException, CoreException {
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		String packageName = mosyncProject.getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME);
		try {
			ADB.getExternal().setupGdb(serialNumber, packageName, 5039);
		} catch (CoreException e) {
			throw new IOException("Could not start debugging on-device", e);
		}
		return super.createSession(variant, sessionType, gdb, factory, program, extraArgs, usePty, project, monitor);
	}
	
	protected CommandFactory getCommandFactory(ILaunchConfiguration config) throws CoreException {
		return getDefaultCommandFactory(config);
	}
	
	protected MISession createMISession0(int type, MIProcess process, CommandFactory commandFactory, IMITTY pty, int timeout) throws MIException {
		AndroidNDKMISession session = new AndroidNDKMISession(process, pty, type, commandFactory, timeout);
		session.setSerialNumber(serialNumber);
		return session;
	}

	public static List<String> getLibraryPaths(MoSyncProject project, IBuildVariant variant, String serialNumber) throws CoreException {
		// NOTE NOTE NOTE: The .so with debug info is located in a different place!
		ArrayList<String> solibPaths = new ArrayList<String>();
		ADB adb = ADB.getDefault();
		String arch = adb.matchAbi(serialNumber, ADB.ABIS);
		String buildPath = AndroidPackager.computeNativeDebugLib(project, variant, arch).getParentFile().getAbsolutePath();
		solibPaths.add(buildPath);
		String libsPath = MoSyncTool.getDefault().getMoSyncLib().append("android_" + arch + "_debug").toOSString();
		solibPaths.add(libsPath);
		return solibPaths;
	}

}
