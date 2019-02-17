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
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

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
    public void zip(Path sourceFolder, Path zipFile, String addParent) {
        List<Path> files = Files.walk(sourceFolder).filter(f -> !f.equals(sourceFolder)).collect(Collectors.toList());
        Files.createDirectories(zipFile.getParent());
        if (Files.notExists(zipFile)) Files.createFile(zipFile);
        try (ArchiveOutputStream os = new ZipArchiveOutputStream(zipFile.toFile())) {
            for (Path path : files) {
                Path zipPath = sourceFolder.relativize(path);
                if (addParent != null) zipPath = Paths.get(addParent).resolve(zipPath);
                ArchiveEntry ae = os.createArchiveEntry(path.toFile(), zipPath.toFile().toString().replace('\\', '/'));
                os.putArchiveEntry(ae);
                if (Files.isRegularFile(path))
                    Files.copy(path, os);
                os.closeArchiveEntry();
            }
        }
    }
}