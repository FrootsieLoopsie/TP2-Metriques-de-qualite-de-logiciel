package net.frootloop.qa.parser.result;

import net.frootloop.qa.parser.StringParser;
import net.frootloop.qa.parser.result.internal.CodeTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ParsedSourceFile {

    /**
     * The source code is organized as a tree, where blocks of code (i.e. curly braces) are the nodes and where each block is represented by
     * its contained statements, and its leading statement. This allows us to attribute proper class/method ownership, and do fancy things like
     * print a .java file's entire cleaned up source code with proper indentation.
     */
    private CodeTree codeTree;
    private Path filePath;
    private String packageName;

    private int numLines = 0, numLinesEmpty = 0, numLinesComments = 0, numLinesCode = 0;

    /***
     * List of classes found within the source file. One file can declare multiple nested classes, enums, etc.
     */
    private ArrayList<ParsedClass> classes = new ArrayList<>();

    private String[] assertStatements;

    private String[] importStatements;


    /***
     * Parses a given source file
     *
     * @param path : file path and extension.
     * @return Data contained in the file, in the form of a String.
     */
    public ParsedSourceFile(Path path) {

        if(!path.toString().endsWith(".java")) return;
        this.filePath = path;

        try {
            // STEP 1: READ AND CLEAN UP THE TEXT DATA
            // Remove unnecessary spaces, null chars, normalize line breaks, and replace string values with "text":
            String sourceFileTextData = StringParser.cleanUpSource(Files.readString(path));

            // STEP 2: COUNT LINES
            // First, get total number of lines:
            this.numLines = StringParser.getLineCountOf(sourceFileTextData);

            // Second, remove empty lines, then count what's left:
            sourceFileTextData = StringParser.getWithoutEmptyLines(sourceFileTextData);
            this.numLinesEmpty = this.numLines - StringParser.getLineCountOf(sourceFileTextData);

            // Third, remove comments, then count what's left:
            sourceFileTextData = StringParser.getWithoutComments(sourceFileTextData);
            this.numLinesComments = this.numLines - this.numLinesEmpty - StringParser.getLineCountOf(sourceFileTextData);
            this.numLinesCode = this.numLines - this.numLinesComments - this.numLinesEmpty;

            // STEP 3: FETCH DATA RELATING TO CODE STATEMENTS, SUCH AS PACKAGE, IMPORTS, ETC.
            sourceFileTextData = StringParser.getWithoutLineBreaks(sourceFileTextData);
            this.importStatements = StringParser.getImportStatementsOf(sourceFileTextData);
            this.assertStatements = StringParser.getAssertStatementsOf(sourceFileTextData);
            this.packageName = StringParser.getPackageNameOf(sourceFileTextData);

            // STEP 4: BUILD CODE TREE AND CLASSES
            this.codeTree = new CodeTree(sourceFileTextData);
            this.classes = this.codeTree.getListOfClasses(this.packageName, this.filePath, this.importStatements);

        } catch (IOException e) {
            System.out.println("ERROR. Unable to read file " + path);
            e.printStackTrace();
        };
    }

    public int getNumLinesEmpty() {
        return numLinesEmpty;
    }

    public int getNumLines() {
        return numLines;
    }

    public int getNumLinesCode() {
        return numLinesCode;
    }

    public int getNumLinesComments(){
        return numLinesComments;
    }

    public String getPackageName() {
        return packageName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getNumAssertStatements() {
        return assertStatements.length;
    }

    public ParsedClass[] getClasses() {
        return classes.toArray(new ParsedClass[classes.size()]);
    }
}