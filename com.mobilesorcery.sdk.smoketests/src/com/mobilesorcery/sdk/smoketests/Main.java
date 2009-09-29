package com.mobilesorcery.sdk.smoketests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IEmulatorProcessListener;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncNature;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.swtbot.SWTBotProjectExplorer;

/**
 * <p>The main entry point for automated functional testing, with focus on
 * a few major use cases being 'smoke tested' (although we do some asserts
 * while we're at it, so it's a tad more than just 'does it start at all?')
 * <p>
 * <b>Tested use cases (or planned to be):</b>
 * <ul>
 * <li>Build and run on emulator</li>
 * <li>Start a debugging session, set a few breakpoints, make sure they are hit</li>
 * <li>Simple incremental build and dependencies between projects</li>
 * </ul>
 * </p>
 * @author Mattias Bybro, mattias.bybro@purplescout.com/mattias@bybro.com
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class Main {

	private static final int RUN = 0;
	private static final int DEBUG = 1;
	
	private static SWTWorkbenchBot ui;

	@BeforeClass
	public static void beforeClass() throws Exception {
		ui = new SWTWorkbenchBot();
		// Close welcome view.
		ui.viewByTitle("Welcome").close();
	}

	@Test
	public void createAndRunProject() throws Exception {
		MoSyncProject projectA = createProjectWithFiles("a", new String[] { "/files/test.c", "/files/dummy.c" }, new String[] { "test.c", "dummy.c" });
		defaultInit(projectA);

		MockEmulatorProcessListener listener = new MockEmulatorProcessListener();
		listener.connect();
		try {
			botRunProject(projectA, listener, RUN);
			listener.awaitStopped(30, TimeUnit.SECONDS);
			assertTrue(listener.getStreamedData().contains("It worked!"));
		} finally {
			listener.disconnect();
		}
	}
	
	@Test
	public void createAndDebugProject() throws Exception {
		MoSyncProject project = createProjectWithFiles("debug", new String[] { "/files/bp_test.c" }, new String[] { "bp_test.c" });
		project.setProperty(MoSyncBuilder.DEAD_CODE_ELIMINATION, Boolean.TRUE.toString());
		defaultInit(project);
		
		// Add breakpoints
		BreakpointTestParser parser = new BreakpointTestParser();
		parser.parse(getClass().getResourceAsStream("/files/bp_test.c"), project.getWrappedProject().getFile(new Path("bp_test.c")));
		
		MockEmulatorProcessListener listener = new MockEmulatorProcessListener();
		listener.connect();
		
		try {
			botRunProject(project, listener, DEBUG);
			okPerspectiveSwitch();
			listener.awaitBreakpointHit(30, TimeUnit.SECONDS);
			assertTrue("Breakpoint SHOULD contain this message at this point!", listener.getStreamedData().contains("Before breakpoint"));
			// TODO; activate this.
			//assertFalse("Breakpoint should NOT contain this message yet!", listener.getStreamedData().contains("After breakpoint"));
			debugContinue();
			listener.awaitStopped(30, TimeUnit.SECONDS);
			assertTrue(listener.getStreamedData().contains("It still works!"));
		} finally {
			listener.disconnect();
		}
	}

	private void debugContinue() {
		ui.menu("Run").menu("Resume").click();
	}

	private void okPerspectiveSwitch() {
		SWTBotShell confirmDialog = ui.shell("Confirm Perspective Switch");
		confirmDialog.activate();
		ui.button("Yes").click();
	}

	private void defaultInit(MoSyncProject project) {
		project.setTargetProfile(MoSyncTool.getDefault().getDefaultEmulatorProfile());
		setPerspective("MoSync");
	}

	private MoSyncProject createProjectWithFiles(String projectName, String[] src, String[] dest) throws Exception {
		MoSyncProject project = botCreateProject(projectName);
		for (int i = 0; i < src.length; i++) {
			addFile(project, src[i], dest[i]);
		}
		return project;		
	}
	
	private void addFile(MoSyncProject project, String src, String dest)
			throws CoreException {
		IFile destFile = project.getWrappedProject().getFile(new Path(dest));		
		
		destFile.create(getClass().getResourceAsStream(src), true,
				new NullProgressMonitor());
		assertTrue(destFile.exists());
	}

	private void setPerspective(String perspectiveName) {
		ui.perspectiveByLabel(perspectiveName).activate();
	}

	void botRunProject(MoSyncProject project, IEmulatorProcessListener listener, int mode) {
		CoreMoSyncPlugin.getDefault().getEmulatorProcessManager()
				.addEmulatorProcessListener(listener);

		SWTBotProjectExplorer explorer = new SWTBotProjectExplorer(ui);
		explorer.select(project.getWrappedProject());
		System.err.println(ui.activeShell().getText());
		SWTBotMenu runMenu = ui.menu("Run");
		SWTBotMenu runRunMenu = runMenu.menu(mode == RUN ? "Run" : "Debug");
		runRunMenu.click();
		String shellTitle = mode == RUN ? "Run As" : "Debug As";
		if (ui.shells(shellTitle).size() > 0) {
			ui.shell(shellTitle).activate();
			ui.table().select("Run MoRE Emulator");
			ui.button("OK").click();
		}
	}

	MoSyncProject botCreateProject(String name) throws Exception {
		ui.menu("File").menu("New").menu("Project...").click();

		SWTBotShell shell = ui.activeShell();
		shell.activate();
		ui.tree().select("MoSync Project");
		clickNext();

		ui.textWithLabel("Project name:").setText(name);
		clickNext();

		ui.checkBox(0).click();

		clickFinish();

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
				name);
		assertTrue(project.exists());
		assertTrue(MoSyncNature.hasNature(project));

		return MoSyncProject.create(project);
	}

	private void clickFinish() {
		ui.button("Finish").click();
	}

	private void clickNext() {
		ui.button("Next >").click();
	}

	@AfterClass
	public static void sleep() {
		ui.sleep(2000);
	}

}
