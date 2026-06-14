package org.libreimpress.smartart;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

/**
 * Verifies the built SmartArt.oxt contains every file that LibreOffice
 * requires to load the extension. Runs in the integration-test phase
 * (after package) via maven-failsafe-plugin so the OXT already exists.
 *
 * If this test fails after an assembly/pom change, check that
 * src/main/assembly/oxt.xml includes the missing entry and that its
 * source directory actually contains the file.
 */
public class OxtContentsIT {

    private static final List<String> REQUIRED_ENTRIES = Arrays.asList(
            "description.xml",
            "META-INF/manifest.xml",
            "Addons.xcu",
            "ProtocolHandler.xcu",
            "uno/SmartArtImpl.xml",
            "uno/smartart.jar"
    );

    private static File oxtFile;

    @BeforeClass
    public static void locateOxt() {
        String buildDir = System.getProperty("project.build.directory", "target");
        oxtFile = new File(buildDir, "SmartArt.oxt");
        assertTrue("SmartArt.oxt not found at " + oxtFile.getAbsolutePath()
                + " — run mvn verify, not mvn test", oxtFile.exists());
    }

    @Test
    public void oxtContainsAllRequiredEntries() throws Exception {
        try (ZipFile zip = new ZipFile(oxtFile)) {
            for (String entry : REQUIRED_ENTRIES) {
                ZipEntry ze = zip.getEntry(entry);
                assertNotNull("Missing required OXT entry: " + entry
                        + " — check src/main/assembly/oxt.xml", ze);
                assertTrue("OXT entry is empty: " + entry, ze.getSize() != 0);
            }
        }
    }

    @Test
    public void descriptionXmlVersionMatchesBuildVersion() throws Exception {
        String buildVersion = readBuildVersion();
        assertFalse("smartart-version.properties must not be unfiltered",
                buildVersion.startsWith("${"));

        try (ZipFile zip = new ZipFile(oxtFile)) {
            ZipEntry entry = zip.getEntry("description.xml");
            assertNotNull("description.xml missing from OXT", entry);
            String content;
            try (InputStream in = zip.getInputStream(entry)) {
                content = new String(in.readAllBytes(), "UTF-8");
            }
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("<version\\s+value=\"([^\"]+)\"")
                            .matcher(content);
            assertTrue("No <version value=...> in OXT description.xml", m.find());
            String oxtVersion = m.group(1).trim();
            assertFalse("OXT description.xml version must not be unfiltered (${project.version} literal found)",
                    oxtVersion.startsWith("${"));
            assertEquals(
                    "OXT description.xml version must match pom.xml version",
                    buildVersion, oxtVersion);
        }
    }

    private static String readBuildVersion() throws Exception {
        try (InputStream in =
                OxtContentsIT.class.getResourceAsStream("/smartart-version.properties")) {
            assertNotNull("smartart-version.properties not found on classpath", in);
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version");
            assertNotNull("version key missing", v);
            return v.trim();
        }
    }
}
