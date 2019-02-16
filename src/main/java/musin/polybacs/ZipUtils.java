package musin.polybacs;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class ZipUtils {

    public void unzip(Path zipFile, Path destinationFolder) {
        try (ZipFile zf = new ZipFile(zipFile.toFile())) {
            Files.createDirectories(destinationFolder);
            Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry e = entries.nextElement();
                Path resolved = destinationFolder.resolve(e.getName());
                Path parentFolder = e.isDirectory() ? resolved : resolved.getParent();
                Files.createDirectories(parentFolder);
                if (!e.isDirectory()) {
                    IOUtils.copy(zf.getInputStream(e), Files.newOutputStream(resolved));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    public void zip(Path sourceFolder, Path zipFile) {
        Files.createDirectories(zipFile.getParent());
        if (Files.notExists(zipFile)) Files.createFile(zipFile);
        try (ArchiveOutputStream os = new ZipArchiveOutputStream(zipFile.toFile());
             Stream<Path> files = Files.walk(sourceFolder)) {
            for (Path path : files.collect(Collectors.toList())) {
                if (path.equals(sourceFolder)) continue;
                ArchiveEntry ae = os.createArchiveEntry(path.toFile(), sourceFolder.relativize(path).toFile().toString());
                os.putArchiveEntry(ae);
                if (Files.isRegularFile(path))
                    Files.copy(path, os);
                os.closeArchiveEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}