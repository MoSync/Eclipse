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

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.sourcelookup.ICSourceLocator;
import org.eclipse.cdt.debug.core.sourcelookup.SourceLookupFactory;
import org.eclipse.cdt.internal.core.model.CModelManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
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
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.ui.statushandlers.StatusManager;

import com.mobilesorcery.sdk.core.BuildConfiguration;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.ILaunchConstants;
import com.mobilesorcery.sdk.core.IProcessUtil;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.SpawnedProcess;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.internal.EmulatorOutputParser;
import com.mobilesorcery.sdk.internal.OSPipeInputStream;
import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import com.mobilesorcery.sdk.internal.debug.MoSyncCDebugTarget;
import com.mobilesorcery.sdk.internal.debug.MoSyncDebugger;
import com.mobilesorcery.sdk.profiles.IProfile;

public class EmulatorLaunchConfigurationDelegate extends LaunchConfigurationDelegate implements ILaunchConfigurationDelegate2 {

    private static int GLOBAL_ID = 1;

    public void launch(final ILaunchConfiguration launchConfig, final String mode, final ILaunch launch,
            final IProgressMonitor monitor) throws CoreException {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    final int emulatorId = getNextId();
                    launchSync(launchConfig, mode, launch, emulatorId, monitor);
                } catch (CoreException e) {
                    StatusManager.getManager().handle(e.getStatus(), StatusManager.SHOW);
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
    	IProject project = getProject(configuration);
    	MoSyncProject mosyncProject = MoSyncProject.create(project);
    	// We'll let non-mosync projects slip through; they'll be handled in launchSync
    	if (mosyncProject != null && mosyncProject.isBuildConfigurationsSupported()) {
    		if (configuration.getAttribute(ILaunchConstants.AUTO_CHANGE_CONFIG, true)) {
    			String buildConfig = configuration.getAttribute(ILaunchConstants.BUILD_CONFIG, "Debug");
    			IBuildConfiguration activeBuildConfig = mosyncProject.getActiveBuildConfiguration();
    			String activeBuildConfigId = activeBuildConfig == null ? null : activeBuildConfig.getId();
    			if (buildConfig != null && !buildConfig.equals(activeBuildConfigId)) {
    				mosyncProject.setActiveBuildConfiguration(buildConfig);
    			}
    		}
    	}
    	
    	return super.preLaunchCheck(configuration, mode, monitor);
    }
    
    public void launchSync(ILaunchConfiguration launchConfig, String mode, ILaunch launch, int emulatorId, IProgressMonitor monitor)
            throws CoreException {
    	boolean debug = "debug".equals(mode);
    	
    	String width = launchConfig.getAttribute(ILaunchConstants.SCREEN_SIZE_WIDTH, "176");
        String height = launchConfig.getAttribute(ILaunchConstants.SCREEN_SIZE_HEIGHT, "220");
        
        IProject project = getProject(launchConfig);
        
        if (!MoSyncNature.hasNature(project) && MoSyncNature.isCompatible(project)) {
        	throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format(
        			"Could not launch ''{0}'' - please upgrade this project to the new MoSync project type (available in the context menu)", project.getName())));
        }

        if (project.findMaxProblemSeverity(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE) == IMarker.SEVERITY_ERROR) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format("Could not launch; build errors in project {0}", project.getName())));
        }
        
        MoSyncProject mosyncProject = MoSyncProject.create(project);
        if (MoSyncBuilder.PROJECT_TYPE_LIBRARY.equals(mosyncProject.getProperty(MoSyncBuilder.PROJECT_TYPE))) {
            throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
                    "Cannot execute a library; please compile as application"));
        }
        

    	IBuildConfiguration buildConfiguration = mosyncProject.getActiveBuildConfiguration();
    	
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

        String[] cmdline = getCommandLine(project, width, height, dupWriteFd, emulatorId, debug);

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

        File dir = MoSyncBuilder.getOutputPath(mosyncProject.getWrappedProject()).toFile();
        
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
     
            IPath outputPath = MoSyncBuilder.getOutputPath(project);
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
        } catch (IOException e) {
            e.printStackTrace();
            CoreMoSyncPlugin.getDefault().log(e);
        }

        pu.pipe_close(dupWriteFd);
    }

    private void attachDebugger(ILaunch launch, IProcess process, IPath program) throws CoreException {
    	IFile[] programFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(program);
    	IProject project = getProject(launch.getLaunchConfiguration());
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

	public static IProject getProject(ILaunchConfiguration launchConfig) throws CoreException {
		return MoSyncBuilder.getProject(launchConfig);
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

    private String[] getCommandLine(IProject project, String width, String height, int fd, int id, boolean debug) {
        IPath outputPath = MoSyncBuilder.getOutputPath(project);
        IPath program = outputPath.append("program");
        IPath resources = outputPath.append("resources");
         
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
        return MoSyncTool.getDefault().getMoSyncBin().append("MoRE.exe").toOSString();
    }

    private synchronized int getNextId() {
        GLOBAL_ID %= 256;
        return GLOBAL_ID++;
    }

    public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
    	// We implement this ourselves so we can add source lookup (and hence niceties like 
    	// clicking on stack trace -> open editor
    	ICSourceLocator sourceLocator = SourceLookupFactory.createSourceLocator(getProject(configuration));    	
    	Launch launch = new Launch(configuration, mode, sourceLocator);
    	return launch;
    }
    
    public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        //MoSyncBuilder builder = new MoSyncBuilder();
        IProject project = getProject(configuration);
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
        //builder.fullBuild(project, MoSyncProject.create(project).getTargetProfile(), false, true, monitor);
        return false;
    }
}
