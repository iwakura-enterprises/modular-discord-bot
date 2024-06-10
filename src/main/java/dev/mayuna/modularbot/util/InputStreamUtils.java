package dev.mayuna.modularbot.util;

import lombok.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class InputStreamUtils {

    private InputStreamUtils() {
    }

    /**
     * Tries to find specified name in specified {@link ZipFile}
     *
     * @param zipFile  Non-null {@link ZipFile}
     * @param fileName Non-null file name
     *
     * @return Nullable {@link InputStream}
     */
    public static InputStream openFileAsInputStream(@NonNull ZipFile zipFile, @NonNull String fileName) {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();

            if (Objects.equals(zipEntry.getName(), fileName)) {
                try {
                    return zipFile.getInputStream(zipEntry);
                } catch (Exception exception) {
                    throw new RuntimeException("Could not open file " + fileName + " as input stream in zip file " + zipFile.getName() + "!");
                }
            }
        }

        return null;
    }

    /**
     * Reads {@link InputStream} as a string
     *
     * @param inputStream Non-null {@link InputStream}
     *
     * @return String
     */
    public static String readStreamAsString(@NonNull InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining());
    }
}
