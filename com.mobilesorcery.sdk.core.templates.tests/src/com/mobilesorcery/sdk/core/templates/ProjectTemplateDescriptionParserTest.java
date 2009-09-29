package com.mobilesorcery.sdk.core.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.junit.Test;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.templates.SectionedPropertiesFile.Section.Entry;

public class ProjectTemplateDescriptionParserTest {

	@Test
	public void testEntryParser() {
		testEntry("a", null, "a");
		testEntry("a=b", "a", "b");
		testEntry("a\\=b", null, "a=b");
		testEntry("a\\\\=b", "a\\", "b");
	}
	
	private void testEntry(String line, String key, String value) {
		Entry entry = Entry.parse(line);
		assertEquals(key, entry.getKey());
		assertEquals(value, entry.getValue());
	}

	@Test
	public void testParsing() throws Exception {
		ProjectTemplateDescription desc = ProjectTemplateDescription.parse(new StringReader(
				"[Files]\n" +
				"test.c\n" +
				"hello.c -> hi.c\n" +
				"[Settings]\n" +
				MoSyncBuilder.EXTRA_COMPILER_SWITCHES + "= -O4"
			));
		
		desc.getTemplateFiles().get(0).equals("test.c");
		desc.getTemplateFiles().get(1).equals("hello.c");
		desc.getGeneratedFiles().get(1).equals("hi.c");
		desc.getSettings().get(MoSyncBuilder.EXTRA_COMPILER_SWITCHES).equals("-O4");
	}
}
