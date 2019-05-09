package musin.polybacs;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Converter converter = new Converter();
        String prefix = args.length == 0 ? "" : finish(args[0], "-");
        Files.list(Paths.get(""))
                .filter(f -> f.toString().endsWith(".zip"))
                .forEach(f -> converter.convert(f, prefix));
    }

    private static String finish(String s, String suf) {
        if (!s.isEmpty() && !s.endsWith(suf)) s += suf;
        return s;
    }
}