package musin.polybacs;

import lombok.SneakyThrows;
import musin.polybacs.model.Problem;
import musin.polybacs.model.Test;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class Converter {

    private final Path unzippedRootFolder = Paths.get("unzipped");
    private final Path preparedRootFolder = Paths.get("prepared");
    private final Path readyRootFolder = Paths.get("ready");

    public void convert(Path problemZip, String problemPrefix) {
        System.out.println("Converting " + problemZip);
        Problem problem = unzipProblem(problemZip);
        attachStatement(problemZip, problem);
        readProblem(problem);
        prepareProblem(problem);
        zipProblem(problem, problemPrefix);
        System.out.println("Converted " + problemZip);
    }

    @SneakyThrows
    private void zipProblem(Problem problem, String problemPrefix) {
        Path zipFile = readyRootFolder.resolve(problem.getShortName() + ".zip");
        Files.deleteIfExists(zipFile);
        ZipUtils.zip(problem.getPreparationFolder(), zipFile, problemPrefix + problem.getShortName());
    }

    private void attachStatement(Path problemZip, Problem problem) {
        String zipName = problemZip.getFileName().toString();
        String pdfName = zipName.replace(".zip", ".pdf");
        Path pdf = problemZip.resolveSibling(pdfName);
        if (Files.exists(pdf))
            problem.setStatement(pdf);
    }

    private Problem unzipProblem(Path problemZip) {
        String shortName = problemName(problemZip);

        Path unzipTo = unzippedRootFolder.resolve(shortName);
        FileUtils.deleteQuietly(unzipTo.toFile());
        ZipUtils.unzip(problemZip, unzipTo);

        Problem problem = new Problem();
        problem.setMaterialsFolder(unzipTo);

        return problem;
    }

    @SneakyThrows
    private void prepareProblem(Problem problem) {
        Path preparationFolder = preparedRootFolder.resolve(problem.getShortName());
        FileUtils.deleteQuietly(preparationFolder.toFile());
        problem.setPreparationFolder(preparationFolder);
        prepareTemplate(preparationFolder);
        prepareCheckerAndStatement(problem);
        prepareConfig(problem);
        prepareSolutions(problem);
        prepareTests(problem);
    }

    @SneakyThrows
    private void prepareSolutions(Problem problem) {
        Path solutions = problem.getPreparationFolder().resolve("misc").resolve("solution");
        for (Path solution : problem.getSolutions()) {
            Files.copy(solution, solutions.resolve(solution.getFileName()));
        }
    }

    @SneakyThrows
    private void prepareTests(Problem problem) {
        Path tests = problem.getPreparationFolder().resolve("tests");
        for (Test test : problem.getTests()) {
            Files.copy(test.getInput(), tests.resolve(String.format("%02d.in", test.getNumber())));
            Files.copy(test.getOutput(), tests.resolve(String.format("%02d.out", test.getNumber())));
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
        Path configFile = problem.getPreparationFolder().resolve("config.ini");
        try (BufferedWriter bw = Files.newBufferedWriter(configFile, Charset.forName("UTF-8"))) {
            bw.write(config);
        }
    }

    @SneakyThrows
    private void prepareCheckerAndStatement(Problem problem) {
        Path preparationFolder = problem.getPreparationFolder();
        Files.copy(problem.getChecker(), preparationFolder.resolve("checker/check.cpp"));
        if (problem.getStatement() != null)
            Files.copy(problem.getStatement(), preparationFolder.resolve("statement/problem.pdf"));
        else
            System.out.println("Problem has no statement!");
    }

    @SneakyThrows
    private void prepareTemplate(Path preparationFolder) {
        Files.createDirectories(preparationFolder.resolve("checker"));
        Files.createDirectories(preparationFolder.resolve("statement"));
        Files.createDirectories(preparationFolder.resolve("tests"));
        Files.createDirectories(preparationFolder.resolve("misc").resolve("solution"));

        Files.copy(loadFile("checker/config.ini"), preparationFolder.resolve("checker/config.ini"));
        Files.copy(loadFile("statement/pdf.ini"), preparationFolder.resolve("statement/pdf.ini"));
        Files.copy(loadFile("format"), preparationFolder.resolve("format"));
    }

    private void readProblem(Problem problem) {
        readProblemMeta(problem);
        readChecker(problem);
        readSolutions(problem);
        readTests(problem);
    }

    @SneakyThrows
    private void readSolutions(Problem problem) {
        List<Path> solutions = Files.list(problem.getMaterialsFolder().resolve("solutions"))
                .filter(f -> !f.toString().endsWith(".desc"))
                .collect(toList());
        problem.setSolutions(solutions);
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

    private void readChecker(Problem problem) {
        problem.setChecker(problem.getMaterialsFolder().resolve("check.cpp"));
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