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
package com.mobilesorcery.sdk.core.templates;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section.Entry;

public class ProjectTemplateDescriptionParserTest {

	@Test
	public void testEntryParser() {
		testEntry("a", "a", "a");
		testEntry("a=b", "a", "b");
		testEntry("a\\=b", "a=b", "a=b");
		testEntry("a\\\\=b # comment", "a\\", "b");
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
