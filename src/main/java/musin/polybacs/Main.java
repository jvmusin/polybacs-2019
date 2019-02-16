package musin.polybacs;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Converter converter = new Converter();
        Files.list(Paths.get(""))
                .filter(f -> f.toString().endsWith(".zip"))
                .forEach(converter::convert);
    }
}