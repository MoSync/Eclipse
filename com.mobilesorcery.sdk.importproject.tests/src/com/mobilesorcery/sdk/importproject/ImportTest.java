package com.mobilesorcery.sdk.importproject;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;


public class ImportTest {
	
	private static final String ACTIVE_BUILD_CFG = "test.cfg";
	private static final String BUILD_CFG_SUPPORTED = "test.cfg.sup";

	@BeforeClass
	public static void setToHeadless() {
		CoreMoSyncPlugin.setHeadless(true);
	}

	@Test
	public void testLegacyImports() throws Exception {
		testLegacyImport("resources/testproject-legacy-1", defaultExpectedValues());
	}

	private Map<String, String> defaultExpectedValues() {
		Map<String, String> result = new HashMap<String, String>();
		result.put(ACTIVE_BUILD_CFG, "Debug");
		result.put(BUILD_CFG_SUPPORTED, Boolean.TRUE.toString());
		return result;
	}

	private void testLegacyImport(String path, Map<String, String> expected) throws Exception {
		testLegacyImport(path, ImportProjectsRunnable.DO_NOT_COPY | ImportProjectsRunnable.USE_NEW_PROJECT_IF_AVAILABLE, expected);
	}
		
	private void testLegacyImport(String pathStr, int strategy, Map<String, String> expected) throws Exception {
		Bundle bundle = Platform.getBundle("com.mobilesorcery.sdk.importproject.tests");
		Path path = new Path(pathStr);
        URL pathURL = FileLocator.find(bundle, path.append(path.lastSegment() + ".zip"), null);
        URL pathFileURL = FileLocator.toFileURL(pathURL);
		File file = new File(pathFileURL.getPath());
		File unzipped = new File(file.getParentFile(), "unzipped");
		Util.unzip(file, unzipped);
		ArrayList<IProject> result = new ArrayList<IProject>();
		ImportProjectsRunnable importer = new ImportProjectsRunnable(new File[] { new File(unzipped, path.lastSegment() + ".mopro") }, strategy, result);
		importer.run(null);
		
		assertProperlyImported(result.get(0), expected);
	}

	private void assertProperlyImported(IProject project,
			Map<String, String> expected) {
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		HashMap<String, String> projectProperties = new HashMap<String, String>(mosyncProject.getProperties());
		projectProperties.put(ACTIVE_BUILD_CFG, mosyncProject.getActiveBuildConfiguration().getId());
		projectProperties.put(BUILD_CFG_SUPPORTED, Boolean.toString(mosyncProject.areBuildConfigurationsSupported()));
		
		for (String expectedProperty : expected.keySet()) {
			String expectedValue = expected.get(expectedProperty);
			String actualValue = projectProperties.get(expectedProperty);
			assertEquals(expectedValue, actualValue);
		}
	}
	
}
