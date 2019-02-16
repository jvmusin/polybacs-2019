package musin.polybacs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

@Data @AllArgsConstructor
public class Test {
    private int number;
    private Path input;
    private Path output;
}