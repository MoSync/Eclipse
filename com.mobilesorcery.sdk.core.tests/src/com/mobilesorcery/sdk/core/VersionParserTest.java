package com.mobilesorcery.sdk.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionParserTest {

    @Test
    public void testCombinations() {
        testParser("1", 1, Version.UNDEFINED, Version.UNDEFINED, "", "1");
        testParser("1.0", 1, 0, Version.UNDEFINED, "", "1.0");
        testParser("1.0.2", 1, 0, 2, "", "1.0.2"); 
        testParser("1.2a", 1, 2, Version.UNDEFINED, "a", "1.2.a"); 
        testParser("1.2.08.alpha", 1, 2, 8, "alpha", "1.2.8.alpha");         
    }

    private void testParser(String value, int major, int minor, int micro, String qualifier, String canonical) {
        Version v = new Version(value);
        assertEquals(value, v.toString());
        assertEquals(canonical, v.asCanonicalString());
        assertEquals(major, v.getMajor());
        assertEquals(minor, v.getMinor());
        assertEquals(micro, v.getMicro());
        assertTrue(Util.equals(qualifier, v.getQualifier()));
    }
}
