package org.dbtools.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * User: jcampbell
 * Date: 3/22/14
 */
public class JavaUtilTest {
    @Test
    public void testNameToJavaConst() throws Exception {
        assertEquals("TEST", JavaUtil.nameToJavaConst("test"));
        assertEquals("TEST123", JavaUtil.nameToJavaConst("test123"));
        assertEquals("MAIN_DATABASE", JavaUtil.nameToJavaConst("mainDatabase"));
        assertEquals("MAIN_DATABASE", JavaUtil.nameToJavaConst("MainDatabase"));
        assertEquals("MAIN_DATABASE", JavaUtil.nameToJavaConst("main_database"));
    }
}
