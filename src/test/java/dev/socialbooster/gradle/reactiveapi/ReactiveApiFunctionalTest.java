package dev.socialbooster.gradle.reactiveapi;

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ReactiveApiFunctionalTest {
    // https://www.baeldung.com/java-compress-and-uncompress
    private static void unzip(File archive, File destination) throws IOException {
        Preconditions.checkArgument(destination.isDirectory() && archive.isFile());

        byte[] buff = new byte[2048];
        ZipInputStream zip = new ZipInputStream(new FileInputStream(archive));

        ZipEntry zipEntry = zip.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(zipEntry, destination);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zip.read(buff)) > 0) {
                    fos.write(buff, 0, len);
                }
                fos.close();
            }
            zipEntry = zip.getNextEntry();
        }

        zip.closeEntry();
        zip.close();
    }

    // https://www.baeldung.com/java-compress-and-uncompress
    private static File newFile(ZipEntry zipEntry, File destination) throws IOException {
        File destFile = new File(destination, zipEntry.getName());

        String destDirPath = destination.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

//    @Test
    public void buildTestProject() throws IOException {
        File projectDir = new File("build/functionalTest");
        Files.createDirectories(projectDir.toPath());

        File projectZip = new File(Resources.getResource("testProject.zip").getFile());
        unzip(projectZip, projectDir);

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("generateReactiveAPI")
                .withProjectDir(projectDir)
                .build();
    }
}
