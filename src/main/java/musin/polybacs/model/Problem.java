package musin.polybacs.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.List;

@Data
public class Problem {
    private String shortName;
    private Path materialsFolder;
    private Path preparationFolder;

    //config.ini
    private String fullName;
    private String maintainers = "Musin";
    private int timeLimitMillis;
    private int memoryLimitBytes;

    private Path checker;
    private Path statement;
    private List<Path> solutions;
    private List<Test> tests;
}