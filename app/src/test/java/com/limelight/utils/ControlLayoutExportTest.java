package com.limelight.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ControlLayoutExportTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test public void savedLayoutPreservesOpacityUnicodeAndUnknownFieldsExactly() throws Exception {
        byte[] json = "[{\"name\":\"攻击 / Ataque\",\"opacity\":0,\"futureField\":true}]\r\n"
                .getBytes(StandardCharsets.UTF_8);
        File saved = files.newFile("axi_custom.txt");
        Files.write(saved.toPath(), json);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ControlLayoutExport.write(saved, () -> { throw new AssertionError("Used default"); }, () -> output);
        assertArrayEquals(json, output.toByteArray());
        assertArrayEquals(json, Files.readAllBytes(saved.toPath()));
    }

    @Test public void unsavedLayoutExportsBundledDefaultWithoutCreatingInternalFile() throws Exception {
        File saved = new File(files.getRoot(), "axi_unsaved.txt");
        byte[] json = "[{\"name\":\"Default\"}]".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ControlLayoutExport.write(saved, () -> new ByteArrayInputStream(json), () -> output);
        assertArrayEquals(json, output.toByteArray());
        assertFalse(saved.exists());
    }

    @Test public void intentionallyEmptyLayoutDoesNotFallBackToDefault() throws Exception {
        File saved = files.newFile();
        Files.write(saved.toPath(), "[]".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ControlLayoutExport.write(saved, () -> { throw new AssertionError("Used default"); }, () -> output);
        assertEquals("[]", output.toString("UTF-8"));
    }

    @Test public void zeroByteCustomFileFailsBeforeOpeningDestination() throws Exception {
        File saved = files.newFile();
        assertThrows(IOException.class, () -> ControlLayoutExport.write(saved,
                () -> { throw new AssertionError("Used default"); },
                () -> { throw new AssertionError("Opened destination"); }));
    }

    @Test public void missingDefaultFailsBeforeOpeningDestination() throws Exception {
        assertThrows(IOException.class, () -> ControlLayoutExport.write(new File(files.getRoot(), "missing"),
                () -> { throw new IOException("Missing asset"); },
                () -> { throw new AssertionError("Opened destination"); }));
    }

    @Test public void nullDestinationIsReportedAsFailureAndSourceIsClosed() throws Exception {
        boolean[] closed = {false};
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[] {1}) {
            @Override public void close() throws IOException { closed[0] = true; super.close(); }
        };
        assertThrows(IOException.class, () -> ControlLayoutExport.write(new File(files.getRoot(), "missing"),
                () -> source, () -> null));
        assertTrue(closed[0]);
    }

    @Test public void writeFailureIsReportedAndBothStreamsAreClosed() throws Exception {
        boolean[] closed = {false, false};
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[] {1, 2, 3}) {
            @Override public void close() throws IOException { closed[0] = true; super.close(); }
        };
        OutputStream output = new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("No space"); }
            @Override public void close() { closed[1] = true; }
        };
        assertThrows(IOException.class, () -> ControlLayoutExport.write(new File(files.getRoot(), "missing"),
                () -> source, () -> output));
        assertTrue(closed[0]);
        assertTrue(closed[1]);
    }
}
