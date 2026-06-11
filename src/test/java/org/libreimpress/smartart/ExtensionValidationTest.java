package org.libreimpress.smartart;

import org.junit.Test;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

/**
 * ExtensionValidationTest - Validates the .oxt extension package structure.
 * 
 * These tests verify that the LibreOffice extension package is correctly
 * assembled without requiring a running LibreOffice instance.
 */
public class ExtensionValidationTest {

    private static final String OXT_FILE = "target/SmartArt.oxt";

    @Test
    public void testOxtFileExists() {
        File oxt = new File(OXT_FILE);
        assertTrue("SmartArt.oxt should exist at " + OXT_FILE, oxt.exists());
    }

    @Test
    public void testOxtFileIsNotEmpty() {
        File oxt = new File(OXT_FILE);
        assertTrue("SmartArt.oxt should not be empty", oxt.length() > 0);
    }

    @Test
    public void testOxtIsValidZipArchive() {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            assertNotNull("Should be able to open as ZIP", zf);
        } catch (Exception e) {
            fail("SmartArt.oxt is not a valid ZIP archive: " + e.getMessage());
        }
    }

    @Test
    public void testOxtContainsManifest() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry manifest = zf.getEntry("META-INF/MANIFEST.MF");
            assertNotNull("MANIFEST.MF should exist in META-INF/", manifest);
            assertTrue("MANIFEST.MF should not be a directory", !manifest.isDirectory());
        }
    }

    @Test
    public void testOxtContainsComponentDescriptor() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry descriptor = zf.getEntry("SmartArtImpl.xml");
            assertNotNull("SmartArtImpl.xml should exist in root", descriptor);
            assertTrue("SmartArtImpl.xml should not be a directory", !descriptor.isDirectory());
        }
    }

    @Test
    public void testOxtContainsExtensionDescription() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry description = zf.getEntry("description.xml");
            assertNotNull("description.xml should exist in root", description);
            assertTrue("description.xml should not be a directory", !description.isDirectory());
        }
    }

    @Test
    public void testOxtContainsCompiledJar() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            // Should contain smartart JAR (any version)
            boolean foundJar = zf.stream()
                .anyMatch(entry -> entry.getName().contains("smartart") && 
                         entry.getName().endsWith(".jar"));
            assertTrue("Should contain compiled smartart JAR", foundJar);
        }
    }

    @Test
    public void testOxtContainsDialogsDirectory() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry dialogs = zf.getEntry("dialogs/");
            assertNotNull("dialogs/ directory should exist", dialogs);
            assertTrue("dialogs/ should be a directory", dialogs.isDirectory());
        }
    }

    @Test
    public void testManifestValidFormat() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry manifest = zf.getEntry("META-INF/MANIFEST.MF");
            InputStream is = zf.getInputStream(manifest);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            assertTrue("Manifest should contain 'Manifest-Version'", 
                content.contains("Manifest-Version:"));
            assertTrue("Manifest should contain 'Created-By'", 
                content.contains("Created-By:"));
        }
    }

    @Test
    public void testComponentDescriptorValidXml() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry descriptor = zf.getEntry("SmartArtImpl.xml");
            InputStream is = zf.getInputStream(descriptor);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            assertTrue("Component descriptor should contain XML declaration", 
                content.contains("<?xml"));
            assertTrue("Should declare 'components' element", 
                content.contains("components"));
            assertTrue("Should contain SmartArtCommand implementation", 
                content.contains("SmartArtCommand"));
        }
    }

    @Test
    public void testDescriptionXmlValidFormat() throws Exception {
        try (ZipFile zf = new ZipFile(OXT_FILE)) {
            ZipEntry description = zf.getEntry("description.xml");
            InputStream is = zf.getInputStream(description);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            assertTrue("Description should contain XML declaration", 
                content.contains("<?xml"));
            assertTrue("Should contain 'description' element", 
                content.contains("description"));
            assertTrue("Should contain extension identifier", 
                content.contains("org.libreimpress.smartart"));
        }
    }

    @Test
    public void testOxtSizeIsReasonable() {
        File oxt = new File(OXT_FILE);
        long size = oxt.length();
        
        // Should be > 100 KB (with JAR) but < 10 MB (sanity check)
        assertTrue("OXT should be at least 100 KB", size > 100_000);
        assertTrue("OXT should not exceed 10 MB", size < 10_000_000);
    }
}
