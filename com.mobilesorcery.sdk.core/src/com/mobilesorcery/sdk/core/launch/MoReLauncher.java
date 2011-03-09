/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core.launch;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.internal.core.model.CModelManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.IProcessUtil;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.SpawnedProcess;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.EmulatorOutputParser;
import com.mobilesorcery.sdk.internal.OSPipeInputStream;
import com.mobilesorcery.sdk.internal.debug.MoSyncCDebugTarget;
import com.mobilesorcery.sdk.internal.debug.MoSyncDebugger;
import com.mobilesorcery.sdk.internal.launch.EmulatorLaunchConfigurationDelegate;
import com.mobilesorcery.sdk.internal.launch.EmulatorParseEventHandler;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * This is the app launcher for the (default) MoRe emulator.
 * @author Mattias Bybro, mattias@bybro.com
 * TODO: Quite a bunch of remains from the emulatorlaunchconfigurationdelegate class.
 */
public class MoReLauncher implements IEmulatorLauncher{

	public final static String ID = "default";
	
	public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor) throws CoreException {
    	boolean debug = EmulatorLaunchConfigurationDelegate.isDebugMode(mode);
    	
    	String width = launchConfig.getAttribute(ILaunchConstants.SCREEN_SIZE_WIDTH, "176");
        String height = launchConfig.getAttribute(ILaunchConstants.SCREEN_SIZE_HEIGHT, "220");
        
        IProject project = EmulatorLaunchConfigurationDelegate.getProject(launchConfig);
        IBuildVariant variant = EmulatorLaunchConfigurationDelegate.getVariant(launchConfig, mode);

        MoSyncProject mosyncProject = MoSyncProject.create(project);

    	IBuildConfiguration buildConfiguration = mosyncProject.getBuildConfiguration(variant.getConfigurationId());

		if (launchConfig.getAttribute(ILaunchConstants.SCREEN_SIZE_OF_TARGET, true)) {
            IProfile profile = mosyncProject.getTargetProfile();
            if (profile != null) {
                Object profileWidth = profile.getProperties().get(IProfile.SCREEN_SIZE_X);
                Object profileHeight = profile.getProperties().get(IProfile.SCREEN_SIZE_Y);
                if (profileWidth instanceof Number && profileHeight instanceof Number) {
                    width = "" + profileWidth;
                    height = "" + profileHeight;
                }
            }
        }
        
        IProcessUtil pu = CoreMoSyncPlugin.getDefault().getProcessUtil();
        int fds[] = new int[2];
        pu.pipe_create(fds);
        int readFd = fds[0];
        int writeFd = fds[1];
        int dupWriteFd = pu.pipe_dup(writeFd);
        pu.pipe_close(writeFd);

        String[] cmdline = getCommandLine(project, variant, width, height, dupWriteFd, emulatorId, debug);

        final EmulatorParseEventHandler handler = new EmulatorParseEventHandler(mosyncProject, buildConfiguration);
        PipedOutputStream messageOutputStream = new PipedOutputStream();
        PipedInputStream messageInputStream = null;

        try {
            messageInputStream = new PipedInputStream(messageOutputStream) {
            	public int read() throws IOException {
            		return super.read();
            	}
            };
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage()));
        }

        handler.setMessageOutputStream(messageOutputStream);
        handler.setEmulatorId(emulatorId);

        if (CoreMoSyncPlugin.getDefault().isDebugging()) {
        	CoreMoSyncPlugin.trace("Emulator command line:\n    " + Util.join(Util.ensureQuoted(cmdline), " "));
        }

        IPath outputPath = EmulatorLaunchConfigurationDelegate.getLaunchDir(mosyncProject, variant);
        File dir = outputPath.toFile();
        
        String command = Util.join(Util.ensureQuoted(cmdline), " ");
        final SpawnedProcess process = new SpawnedProcess(getMoREExe(), command, dir);

        final EmulatorOutputParser parser = new EmulatorOutputParser(emulatorId, handler);
        startEmulatorListener(process, parser, readFd, dupWriteFd);
        
        process.setInputStream(messageInputStream);

    	process.setShutdownHook(new Runnable() {
			public void run() {
				parser.awaitParseEventsToBeHandled(2000);
			}    		
    	});

        try {
            process.start();
			CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().processStarted(emulatorId);

            IProcess p = DebugPlugin.newProcess(launch, process, project.getName());
     
            IPath program = outputPath.append("program");
            
            if (debug) {
            	attachDebugger(launch, p, program);
            }

            try {
                int errcode = process.waitFor();
                if (errcode != 0) {
                    handler.setExitMessage(getErrorMessage(errcode));
                }                
            } catch (InterruptedException e) {
                CoreMoSyncPlugin.getDefault().log(e);
            } finally {
				CoreMoSyncPlugin.getDefault().getEmulatorProcessManager().processStopped(emulatorId);
            }
        } catch (Exception e) {
           throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
        }

        pu.pipe_close(dupWriteFd);
	}
    
    private void attachDebugger(ILaunch launch, IProcess process, IPath program) throws CoreException {
    	IFile[] programFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(program);
    	IProject project = EmulatorLaunchConfigurationDelegate.getProject(launch.getLaunchConfiguration());
    	IFile programFile = null;
    	for (int i = 0; programFile == null && i < programFiles.length; i++) {
    		if (programFiles[i].getProject().equals(project)) {
    			programFile = programFiles[i];
    		}
    	}

    	MoSyncDebugger dbg = new MoSyncDebugger();
    	ICDISession targetSession = dbg.createSession(launch, program.toFile(), new NullProgressMonitor());
    	IBinaryObject binaryFile = (IBinaryObject) CModelManager.getDefault().createBinaryFile(programFile);    	
    	//IDebugTarget debugTarget = CDIDebugModel.newDebugTarget(launch, project, targetSession.getTargets()[0], launch.getLaunchConfiguration().getName(), process, binaryFile, true, false, true);
    	IDebugTarget debugTarget = MoSyncCDebugTarget.newDebugTarget(launch, project, targetSession.getTargets()[0], launch.getLaunchConfiguration().getName(), process, binaryFile, true, false, null, true);
	}
    
    private void addBinaryParser(IProject project) throws CoreException {
    	  ICDescriptor cDescriptor = CCorePlugin.getDefault().getCDescriptorManager().getDescriptor(project);
    	  cDescriptor.create("org.eclipse.cdt.core.BinaryParser", "org.eclipse.cdt.core.ELF");
    }
	private ICProject getCProject(IProject project) {
		return CModelManager.getDefault().getCModel().findCProject(project);
	}

    private String getErrorMessage(int errcode) throws IOException {
        String msg = MessageFormat.format("Exited with error code {0}: {1}", errcode, findErrorMessage(errcode));
        return msg;
    }

    private String findErrorMessage(int errcode) {
        String panicMessage = CoreMoSyncPlugin.getDefault().getPanicMessage(errcode);
        return panicMessage == null ? "<No error message>" : panicMessage;
    }

    private void startEmulatorListener(SpawnedProcess process, final EmulatorOutputParser parser, final int readFd, final int writeFd) {
    	final OSPipeInputStream input = new OSPipeInputStream(readFd);
        Runnable emulatorListener = new Runnable() {
            public void run() {                
                try {
                    parser.parse(input);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    input.close();
                }
            }
        };

        Thread thread = new Thread(emulatorListener, "Reading from pipe");
        thread.setDaemon(true);
        thread.start();        
    }

    private String[] getCommandLine(IProject project, IBuildVariant variant, String width, String height, int fd, int id, boolean debug) throws CoreException {
        IPath outputPath = EmulatorLaunchConfigurationDelegate.getLaunchDir(MoSyncProject.create(project), variant);
        IPath program = outputPath.append("program");
        IPath resources = outputPath.append("resources");
         
        if (!program.toFile().exists()) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could find no executable - please rebuild"));
        }
        
        ArrayList<String> args = new ArrayList<String>();
        
        args.addAll(Arrays.asList(new String[] { getMoREExe(), "-program", program.toOSString(), "-resource", resources.toOSString(),
                "-resolution", "" + width, "" + height, "-fd", Integer.toString(fd), "-id", Integer.toString(id)
                /*,"-icon", outputPath.append("more.png").toOSString()*/
        }));
        
        if (debug) {
        	args.add("-gdb");
        }

        return args.toArray(new String[args.size()]);
    }
    
    private String getMoREExe() {
    	return MoSyncTool.getDefault().getBinary("MoRE").toOSString();
    }

	@Override
	public String getName() {
		return "MoRe Emulator";
	}

	@Override
	public boolean isAvailable(MoSyncProject project, String mode) {
		return true;
	}



}
