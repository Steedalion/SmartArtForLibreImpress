package org.libreimpress.smartart;

import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Verifies that description.xml (the version LibreOffice reads from the OXT)
 * always matches the Maven project version.
 *
 * Both files are read from the classpath after Maven resource filtering, so
 * ${project.version} is already substituted. If either file is hardcoded the
 * test will catch the divergence on the next build.
 */
public class VersionConsistencyTest {

    @Test
    public void descriptionXmlVersionMatchesMavenVersion() throws Exception {
        String buildVersion = readBuildVersion();
        assertFalse("smartart-version.properties must not be unfiltered",
                buildVersion.startsWith("${"));

        String descVersion = readDescriptionVersion();
        assertFalse("description.xml version must not be unfiltered (${project.version} literal found)",
                descVersion.startsWith("${"));

        assertEquals(
                "description.xml <version value=...> must match pom.xml <version>",
                buildVersion, descVersion);
    }

    private static String readBuildVersion() throws Exception {
        try (InputStream in =
                VersionConsistencyTest.class.getResourceAsStream("/smartart-version.properties")) {
            assertNotNull("smartart-version.properties not found on classpath", in);
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version");
            assertNotNull("version key missing in smartart-version.properties", v);
            return v.trim();
        }
    }

    private static String readDescriptionVersion() throws Exception {
        try (InputStream in =
                VersionConsistencyTest.class.getResourceAsStream("/description.xml")) {
            assertNotNull("description.xml not found on classpath", in);
            String content = new String(in.readAllBytes(), "UTF-8");
            Matcher m = Pattern.compile("<version\\s+value=\"([^\"]+)\"").matcher(content);
            assertTrue("No <version value=...> found in description.xml", m.find());
            return m.group(1).trim();
        }
    }
}
