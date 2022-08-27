package dev.mayuna.modularbot.utils;

import lombok.NonNull;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtil {

    /**
     * Tries to find specified name in specified {@link ZipFile}
     *
     * @param zipFile Non-null {@link ZipFile}
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
}
