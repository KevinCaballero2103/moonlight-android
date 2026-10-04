package com.limelight.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Copies the saved layout, or the bundled default if it has never been saved. */
public final class ControlLayoutExport {
    public interface InputOpener {
        InputStream open() throws IOException;
    }

    public interface OutputOpener {
        OutputStream open() throws IOException;
    }

    private ControlLayoutExport() {}

    public static void write(File savedLayout, InputOpener bundledLayout,
                             OutputOpener destination) throws IOException {
        // An existing but unreadable/empty custom layout is an error, not a reason
        // to silently export a different layout. Keep all JSON fields byte-for-byte.
        try (InputStream source = savedLayout.exists()
                ? new FileInputStream(savedLayout) : bundledLayout.open()) {
            if (source == null) {
                throw new IOException("No control layout source");
            }
            int firstByte = source.read();
            if (firstByte == -1) {
                throw new IOException("Control layout is empty");
            }
            // Validate the source before opening/truncating the destination.
            try (OutputStream output = destination.open()) {
                if (output == null) {
                    throw new IOException("Cannot open layout destination");
                }
                output.write(firstByte);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.flush();
            }
        }
    }
}
