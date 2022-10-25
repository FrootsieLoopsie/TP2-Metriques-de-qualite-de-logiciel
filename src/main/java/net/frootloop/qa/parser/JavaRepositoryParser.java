package net.frootloop.qa.parser;

import net.frootloop.qa.inputhandling.GitGudder;
import net.frootloop.qa.parser.result.ParsedRepository;
import net.frootloop.qa.parser.result.ParsedSourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaRepositoryParser {

    public static void analyseRepositoryAt(Path directory) {

        System.out.println("\n[ PARSING LOCAL REPOSITORY ]\nParsing the source files of repository at: \'" + directory + "\'...");
        ParsedRepository repo = JavaRepositoryParser.parse(directory);
        System.out.println("...Done!");

        System.out.println("\n[ LINKING ]\nBuilding the reference maps between methods, classes, functions...");
        repo.buildReferences();
        System.out.println("...Done!");

        System.out.println("\n============================\n          ANALYSIS          \n============================");

        System.out.println("\n[ STATISTICS OF REPOSITORY ]");
        System.out.println("Consists of " + repo.getNumSourceFiles() + " Source Files, " + repo.getNumClasses() + " Classes, " + repo.getNumMethods() + " Methods, " + repo.getNumAssertStatements() + " Unit Tests.");
        System.out.println("Number of lines: " + repo.getTotalLinesCode() + " are code, " + repo.getTotalLinesComments() + " are comments, " + repo.getTotalLinesEmpty() + " are empty.\nIn total: " + repo.getTotalLines() + " lines.");

        System.out.println("\n[ COMPLEXITY ]");
        System.out.println("Percentage of code dedicated to documentation (CD): " + String.format("%.1f", 100 * repo.getCommentDensity()) + "%" );
        System.out.println("Weighted Methods per Class (WMC): " + String.format("%.1f", repo.getAverageWeightedMethods()));
        System.out.println("The most complex class is \'" + repo.getMostComplexClass().getSignature() + "\', with a WMC of " + repo.getMostComplexClass().getWeightedMethods() + " and a total cyclomatic complexity of " + repo.getMostComplexClass().getCyclomatcComplexity() + ".");

        System.out.println("\n[ MODULARITY ]");
        System.out.println("Average Lack Of Cohesion in Methods (LCOM): " + String.format("%.1f", repo.getAverageLackOfCohesionInMethods()));
        System.out.println("The least cohesive class is \'" + repo.getLeastCohesiveClass().getSignature() + "\', with an LCOM of " + repo.getLeastCohesiveClass().getLackOfCohesionInMethods() + ".");

        System.out.println("\n[ MATURITY ]");
        System.out.println("Commits made to the project: " + GitGudder.getCommitCountTo(repo.getFilePath()));

        System.out.println("\n[ RELIABILITY ]");
        System.out.println("Average Unit Tests per method : " + repo.getAverageUnitTestsPerMethod());
        System.out.println("Percentage of non-abstract methods not tested (PMNT) : " + String.format("%.1f", repo.getPercentageMethodsUntested()) + "%");
        System.out.println("Percentage of code statements dedicated to tests : " + String.format("%.1f", repo.getPercentageCodeDedicatedForTests()) + "%  (Including \'@Test\' Functions)");

    }

    /***
     * Builds and returns a data container for information relating to the given repository, including
     * class references, number of lines, etc.
     *
     * @param directory : (Path) directory from which .java files will be analyzed and parsed.
     * @return ParsedRepository instance
     */
    private static ParsedRepository parse(Path directory){
        ParsedRepository repo = new ParsedRepository(directory);
        JavaRepositoryParser.walk(directory, repo);
        repo.buildReferences();
        return repo;
    }

    /***
     * Called by the parse() function. Checks what's inside a given folder; for each
     * file with a .java extension it finds, it parses the source file and adds its data to
     * the ParsedRepository instance.
     *
     * In the end, the ParsedRepository will contain all relevant data relating to the Java
     * source files contained in a given folder and in all of its subfolders.
     *
     * @param path
     * @param repo
     */
    private static void walk(Path path, ParsedRepository repo) {
        if(repo == null) return;
        try {
            Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .forEach((filePath) -> {
                        ParsedSourceFile sourceFile = JavaSourceFileParser.parse(filePath);

                        // Add to the repository:
                        if (sourceFile != null) repo.addParsedFile(sourceFile);

                    });
        }
        catch (IOException e) {
            System.out.println("ERROR: Something went horribly wrong in function visitFolder when trying to list the files of directory :\n" + path);
            e.printStackTrace();
        }
    }

    private static void addFileToRepository(Path filePath, ParsedRepository repo) {
        ParsedSourceFile sourceFile = JavaSourceFileParser.parse(filePath);
        if(sourceFile != null) repo.addParsedFile(sourceFile);
    }
}
