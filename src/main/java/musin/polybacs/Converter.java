package musin.polybacs;

import lombok.SneakyThrows;
import musin.polybacs.model.Problem;
import musin.polybacs.model.Test;
import org.json.JSONObject;
import org.json.XML;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class Converter {

    private final Path unzippedRootFolder = Paths.get("unzipped");
    private final Path preparedRootFolder = Paths.get("prepared");
    private final Path readyRootFolder = Paths.get("ready");

    public void convert(Path problemZip) {
        Problem problem = unzipProblem(problemZip);
        readProblem(problem);
        prepareProblem(problem);
        zipProblem(problem);
    }

    private void zipProblem(Problem problem) {
        Path zipFile = readyRootFolder.resolve(problem.getShortName() + ".zip");
        ZipUtils.zip(problem.getPreparationFolder(), zipFile);
    }

    private Problem unzipProblem(Path problemZip) {
        String shortName = problemName(problemZip);

        Path unzipTo = unzippedRootFolder.resolve(shortName);
        ZipUtils.unzip(problemZip, unzipTo);

        Problem problem = new Problem();
        problem.setMaterialsFolder(unzipTo);

        return problem;
    }

    @SneakyThrows
    private void prepareProblem(Problem problem) {
        Path preparationFolder = preparedRootFolder.resolve(problem.getShortName());
        problem.setPreparationFolder(preparationFolder);

        prepareTemplate(preparationFolder);
        prepareCheckerAndStatement(problem);
        prepareConfig(problem);
        prepareTests(problem);
    }

    @SneakyThrows
    private void prepareTests(Problem problem) {
        Path tests = problem.getPreparationFolder().resolve("tests");
        for (Test test : problem.getTests()) {
            Files.copy(test.getInput(), tests.resolve(String.format("%02d.in", test.getNumber())), REPLACE_EXISTING);
            Files.copy(test.getOutput(), tests.resolve(String.format("%02d.out", test.getNumber())), REPLACE_EXISTING);
        }
    }

    @SneakyThrows
    private void prepareConfig(Problem problem) {
        String config = String.format("" +
                        "[info]\n" +
                        "name = %s\n" +
                        "maintainers = %s\n" +
                        "\n" +
                        "[resource_limits]\n" +
                        "time = %ds\n" +
                        "memory = %dMiB\n",
                problem.getFullName(),
                problem.getMaintainers(),
                problem.getTimeLimitMillis() / 1000,
                problem.getMemoryLimitBytes() / 1024 / 1024);
        Files.write(problem.getPreparationFolder().resolve("config.ini"), config.getBytes());
    }

    @SneakyThrows
    private void prepareCheckerAndStatement(Problem problem) {
        Path preparationFolder = problem.getPreparationFolder();
        Files.copy(problem.getChecker(), preparationFolder.resolve("checker/check.cpp"), REPLACE_EXISTING);
        Files.copy(problem.getStatement(), preparationFolder.resolve("statement/problem.pdf"), REPLACE_EXISTING);
    }

    @SneakyThrows
    private void prepareTemplate(Path preparationFolder) {
        Files.createDirectories(preparationFolder.resolve("checker"));
        Files.createDirectories(preparationFolder.resolve("statement"));
        Files.createDirectories(preparationFolder.resolve("tests"));

        Files.copy(loadFile("checker/config.ini"), preparationFolder.resolve("checker/config.ini"), REPLACE_EXISTING);
        Files.copy(loadFile("statement/pdf.ini"), preparationFolder.resolve("statement/pdf.ini"), REPLACE_EXISTING);
        Files.copy(loadFile("format"), preparationFolder.resolve("format"), REPLACE_EXISTING);
    }

    private void readProblem(Problem problem) {
        readProblemMeta(problem);
        readCheckerAndStatement(problem);
        readTests(problem);
    }

    @SneakyThrows
    private void readTests(Problem problem) {
        Stream<Path> testFiles = Files.list(problem.getMaterialsFolder().resolve("tests"));
        Map<String, List<Path>> testsByName = testFiles.collect(groupingBy(
                f -> f.getFileName().toString().split("\\.")[0],
                toList()
        ));
        List<Test> tests = testsByName.entrySet().stream()
                .map(f -> {
                    int number = Integer.parseInt(f.getKey());
                    Path first = f.getValue().get(0);
                    Path second = f.getValue().get(1);
                    Path in = first.getFileName().toString().endsWith(".a") ? second : first;
                    Path out = in == first ? second : first;
                    return new Test(number, in, out);
                })
                .sorted(Comparator.comparingInt(Test::getNumber))
                .collect(toList());
        problem.setTests(tests);
    }

    private void readCheckerAndStatement(Problem problem) {
        Path materialsFolder = problem.getMaterialsFolder();
        problem.setChecker(materialsFolder.resolve("files").resolve("check.cpp"));
        problem.setStatement(materialsFolder
                .resolve("statements")
                .resolve(".pdf")
                .resolve("russian")
                .resolve("problem.pdf")
        );
    }

    @SneakyThrows
    private void readProblemMeta(Problem problem) {
        Path configXml = problem.getMaterialsFolder().resolve("problem.xml");
        JSONObject jsonObject = XML.toJSONObject(Files.newBufferedReader(configXml));
        JSONObject problemObj = jsonObject.getJSONObject("problem");
        JSONObject testset = problemObj
                .getJSONObject("judging")
                .getJSONObject("testset");

        String shortName = problemObj.optString("short-name");
        String problemName = problemObj.getJSONObject("names").getJSONObject("name").optString("value");
        int timeLimitMillis = testset.getInt("time-limit");
        int memoryLimitBytes = testset.getInt("memory-limit");

        problem.setShortName(shortName);
        problem.setFullName(problemName);
        problem.setTimeLimitMillis(timeLimitMillis);
        problem.setMemoryLimitBytes(memoryLimitBytes);
    }

    private String problemName(Path problemZip) {
        String zipName = problemZip.getFileName().toString();
        return zipName.split("\\.")[0];
    }

    @SneakyThrows
    private InputStream loadFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream("template-archive/" + path);
    }
}