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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.eclipse.cdt.debug.core.cdi.ICDISessionConfiguration;
import org.eclipse.cdt.debug.mi.core.GDBCDIDebugger2;
import org.eclipse.cdt.debug.mi.core.IMIConstants;
import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;

public class MoSyncDebugger extends GDBCDIDebugger2 {
	
	protected IPath getGDBPath(ILaunch launch) throws CoreException {
		return MoSyncTool.getDefault().getBinary("mdb");
		/*ILaunchConfiguration config = launch.getLaunchConfiguration();
		return new Path( config.getAttribute( IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, IMILaunchConfigurationConstants.DEBUGGER_DEBUG_NAME_DEFAULT ) );*/
	}

	// Copied 'n' modified from AbstractGDBCDIDebugger
	protected Session createGDBSession(ILaunch launch, File executable, IProgressMonitor monitor) throws CoreException {
		Session session = null;
		IPath gdbPath = getGDBPath( launch );
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		CommandFactory factory = getCommandFactory( config );
		String[] extraArgs = getExtraArguments( config );
		boolean usePty = usePty( config );
		try {
			//session = MIPlugin.getDefault().createSession( getSessionType( config ), gdbPath.toOSString(), factory, executable, extraArgs, usePty, monitor );
			IProject project = EmulatorLaunchConfigurationDelegate.getProject(launch.getLaunchConfiguration());
			IBuildVariant variant = EmulatorLaunchConfigurationDelegate.getVariant(config, "debug");
			session = createSession(variant, getSessionType(config), gdbPath.toOSString(), factory, executable, extraArgs, usePty, project, monitor);
			ICDISessionConfiguration sessionConfig = getSessionConfiguration( session );
			if ( sessionConfig != null ) {
				session.setConfiguration( sessionConfig );
			}
		}
		catch( OperationCanceledException e ) {
		}
		catch( Exception e ) {
			// Catch all wrap them up and rethrow
			if ( e instanceof CoreException ) {
				throw (CoreException)e;
			}
			throw newCoreException( e );
		}
		return session;
	}

	// Copied 'n' modified from MIPlugin
	protected Session createSession(IBuildVariant variant, int sessionType, String gdb, CommandFactory factory, File program, String[] extraArgs, boolean usePty, IProgressMonitor monitor) throws IOException, MIException, CoreException {
		return createSession(variant, sessionType, gdb, factory, program, extraArgs, usePty, null, monitor);
	}
	
	protected String[] getGDBCommandLine(String gdb, String[] extraArgs, File executable, IProject project, IBuildVariant variant, CommandFactory factory, boolean usePty) throws CoreException {
		if (gdb == null || gdb.length() == 0) {
			gdb = MoSyncTool.getDefault().getBinary("mdb").toOSString();
		}

		ArrayList<String> argList = new ArrayList<String>(extraArgs.length + 8);
		argList.add(gdb);
		
		if (project != null) {
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			IBuildConfiguration buildConfiguration = mosyncProject.getBuildConfiguration(variant.getConfigurationId());
			IPath sldPath = mosyncProject.getSLD(buildConfiguration).getSLDPath();

			if (executable != null) {
				argList.add("-p");
				argList.add(executable.getAbsolutePath());
			}
			
			if (sldPath.toFile().exists()) {
				argList.add("-sld");
				argList.add(sldPath.toOSString());
			}

			IPath stabsPath = mosyncProject.getStabsPath(buildConfiguration);
			if (supportsStabs(mosyncProject) && stabsPath.toFile().exists()) {
				argList.add("-stabs");
				argList.add(stabsPath.toOSString());
			}
		}
		
		return (String[])argList.toArray(new String[argList.size()]);
	}
	
	protected Session createSession(IBuildVariant variant, int sessionType, String gdb, CommandFactory factory, File program, String[] extraArgs, boolean usePty, IProject project, IProgressMonitor monitor) throws IOException, MIException, CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		//argList.add("-q"); //$NON-NLS-1$
		//argList.add("-nw"); //$NON-NLS-1$
		//argList.add("-i"); //$NON-NLS-1$
		//argList.add(factory.getMIVersion());
		/*if (pty != null) {
			argList.add("-tty"); //$NON-NLS-1$
			argList.add(pty.getSlaveName());
		}*/
		//argList.addAll(Arrays.asList(extraArgs));

		String[] args = getGDBCommandLine(gdb, extraArgs, program, project, variant, factory, usePty);
		int launchTimeout = MIPlugin.getDefault().getPluginPreferences().getInt(IMIConstants.PREF_REQUEST_LAUNCH_TIMEOUT);

		IMITTY pty = null;

		/*if (usePty) {
			try {
				PTY pseudo = new PTY();
				pty = new MITTYAdapter(pseudo);
			} catch (IOException e) {
				// Should we not print/log this ?
			}
		}*/
		
		MISession miSession = null;
		MIProcess pgdb = null;
		boolean failed = false;
		try {
			pgdb = factory.createMIProcess(args, launchTimeout, monitor);
	
			if (MIPlugin.getDefault().isDebugging()) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < args.length; ++i) {
					sb.append(args[i]);
					sb.append(' ');
				}
				MIPlugin.getDefault().debugLog(sb.toString());
			}
		
			//miSession = createMISession0(sessionType, pgdb, factory, pty, getCommandTimeout());
			miSession = createMISession0(sessionType, pgdb, factory, pty, MIPlugin.getCommandTimeout());
		} catch (MIException e) {
			failed = true;
			throw e;
		} catch(IOException e ) {
			failed = true;
			throw e;
		} finally {
			if (failed) {
				// Kill gdb
				if ( pgdb != null )
					pgdb.destroy();
				// Shutdown the pty console.
				if (pty != null) {
					try {
						OutputStream out = pty.getOutputStream();
						if (out != null) {
							out.close();
						}
						InputStream in = pty.getInputStream();
						if (in != null) {
							in.close();
						}
					} catch (IOException e) {
					}
				}
			}
		}
		
		return new Session(miSession);
	}

	private boolean supportsStabs(MoSyncProject mosyncProject) {
		// Should always b true? Or, target conf must return very few capabilities, see TargetConfiguration
		return true;
	}

	protected MISession createMISession0(int type, MIProcess process, CommandFactory commandFactory, IMITTY pty, int timeout) throws MIException {
		return new MoSyncMISession(process, pty, type, commandFactory, timeout);
	}
	
	protected void doStartSession( ILaunch launch, Session session, IProgressMonitor monitor ) throws CoreException {
		initializeLibraries(launch.getLaunchConfiguration(), session);
		session.getSharedLibraryManager().setAutoUpdate(false);
	}

	protected CommandFactory getDefaultCommandFactory(ILaunchConfiguration config) throws CoreException {
		return super.getCommandFactory(config);
	}
	
	protected CommandFactory getCommandFactory(ILaunchConfiguration config) throws CoreException {
		String miVersion = getMIVersion(config);
		return new MoSyncCommandFactory(miVersion);
	}
	
	protected boolean getBreakpointsWithFullNameAttribute( ILaunchConfiguration config ) {
		return true;
	}

}
