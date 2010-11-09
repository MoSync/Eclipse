package com.mobilesorcery.sdk.importproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.ImportProjectsRunnable;


public class ImportTest {
	
	public class FileVisitor implements IResourceVisitor {

		private HashSet<IPath> resources = new HashSet<IPath>();
		HashSet<String> extensions;
		private int type;
		
		public FileVisitor(int type, String... extensions) {
			this.type = type;
			this.extensions = new HashSet<String>(Arrays.asList(extensions));
		}
		
		public boolean visit(IResource resource) throws CoreException {
			if (type == resource.getType()) {
				if (extensions.contains(resource.getFileExtension())) {
					resources.add(resource.getProjectRelativePath());
				}
			}
			
			return !new Path("Output").equals(resource.getProjectRelativePath());
		}
		
		public Set<IPath> getProjectRelativePaths() {
			return resources;
		}

	}

	@BeforeClass
	public static void setToHeadless() {
		CoreMoSyncPlugin.setHeadless(true);
	}

	@Test
	public void testLegacyImports() throws Exception {
		testLegacyImport("resources/testproject-legacy-1", ImportProjectsRunnable.COPY_ALL_FILES | ImportProjectsRunnable.USE_NEW_PROJECT_IF_AVAILABLE, null);
		testLegacyImport("resources/testproject-legacy-local", ImportProjectsRunnable.COPY_ALL_FILES | ImportProjectsRunnable.USE_NEW_PROJECT_IF_AVAILABLE, null);
		testLegacyImport("resources/testproject-legacy-mopro-copy-some", ImportProjectsRunnable.COPY_ONLY_FILES_IN_PROJECT_DESC, null);
		testLegacyImport("resources/testproject-legacy-mopro-copy-all", ImportProjectsRunnable.COPY_ALL_FILES, null);
	}
		
	private void testLegacyImport(String pathStr, int strategy, Map<String, String> expectedProperties) throws Exception {
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

		assertProperlyImported(result.get(0), importer, expectedProperties);
	}

	private void assertProperlyImported(IProject project, ImportProjectsRunnable importer,
			Map<String, String> expectedProperties) throws CoreException {
		// We all share the same files, etc - in the future we may want to
		// allow each test project to have different set of files, properties, etc.
		MoSyncProject mosyncProject = MoSyncProject.create(project);
		
		assertTrue(mosyncProject.areBuildConfigurationsSupported());
		assertEquals("Release", mosyncProject.getActiveBuildConfiguration().getId());
		
		FileVisitor fileVisitor = new FileVisitor(IResource.FILE, "c", "cpp", "h", "hpp");
		mosyncProject.getWrappedProject().accept(fileVisitor);
		
		Set<IPath> expectedFiles = getExpectedFiles();
		assertEquals(expectedFiles, fileVisitor.getProjectRelativePaths());
		
		HashMap<String, String> projectProperties = new HashMap<String, String>(mosyncProject.getProperties());
		
		if (expectedProperties == null) {
			expectedProperties = new HashMap<String, String>();
		}
		
		for (String expectedProperty : expectedProperties.keySet()) {
			String expectedValue = expectedProperties.get(expectedProperty);
			String actualValue = projectProperties.get(expectedProperty);
			assertEquals(expectedValue, actualValue);
		}
	}

	private Set<IPath> getExpectedFiles() {
		HashSet<IPath> result = new HashSet<IPath>();
		result.add(new Path("a.h"));
		result.add(new Path("a.c"));
		result.add(new Path("b.c"));
		return result;
	}
	
}
