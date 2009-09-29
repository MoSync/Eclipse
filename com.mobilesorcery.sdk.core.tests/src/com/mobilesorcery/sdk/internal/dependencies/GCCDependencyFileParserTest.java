package com.mobilesorcery.sdk.internal.dependencies;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.Test;


public class GCCDependencyFileParserTest {

	@Test
	public void simpleDeps() throws IOException {
		try {
			ResourcesPlugin.getWorkspace().getRoot().getProject("project").create(null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		IFile f1 = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("project/b.s"));
		IFile f2 = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("project/b.c"));
		IFile f3withEscapedSpace = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("project/d c.c"));
		GCCDependencyFileParser parser = createParser();
		parser.parse("", createTestLine(f1, f2, f3withEscapedSpace));
		Map<IResource, Collection<IResource>> deps = parser.getDependencies();
		System.err.println(deps);
		assertEquals(2, deps.entrySet().iterator().next().getValue().size());
	}

	private InputStream createTestLine(IFile depFrom, IFile... depsTo) {
		StringBuffer str = new StringBuffer(depFrom.getLocation().toOSString());
		str.append(": ");
		for (int i = 0; i < depsTo.length; i++) {
			str.append(" ");
			String unescaped = depsTo[i].getLocation().toOSString(); 
			String escaped = unescaped.replace(" ", "\\ ");
			str.append(escaped);
			str.append("\\\n");
		}
		
		return createTestFile(str.toString());
	}

	private GCCDependencyFileParser createParser() {
		GCCDependencyFileParser parser = new GCCDependencyFileParser();
		return parser;
	}

	private InputStream createTestFile(String str) {
		ByteArrayInputStream result = new ByteArrayInputStream(str.getBytes());
		return result;
	}
}
