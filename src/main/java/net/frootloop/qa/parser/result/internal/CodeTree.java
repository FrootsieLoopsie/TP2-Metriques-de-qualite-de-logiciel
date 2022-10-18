package net.frootloop.qa.parser.result.internal;

import net.frootloop.qa.parser.StringParser;
import net.frootloop.qa.parser.result.ParsedClass;

import java.nio.file.Path;
import java.util.ArrayList;

public class CodeTree implements StringParser {

    protected BlockOfCode root;

    /**
     * The source code is organized as a tree, where blocks of code (i.e. curly braces) are the nodes and where each block is represented by
     * its contained statements, and its leading statement. This allows us to attribute proper class/method ownership, and do fancy things like
     * print a .java file's entire cleaned up source code with proper indentation.
     *
     * @param cleanSourceFileTextData Assumes that the input text has already been cleaned up.
     */
    public CodeTree(String cleanSourceFileTextData) {

        String[] codeStatements = cleanSourceFileTextData.split("(;|\\{|\\})");
        String statement;
        int statementIndex = 0;

        BlockOfCode currentCodeBlock = new BlockOfCode();
        this.root = currentCodeBlock;
        this.root.leadingStatement = codeStatements[0];

        for(int i = 0; i < cleanSourceFileTextData.length(); i++) {
            char c = cleanSourceFileTextData.charAt(i);

            // Ending a statement:
            if(c == ';' || c == '{' || c == '}') {
                if(statementIndex < codeStatements.length) statement = codeStatements[statementIndex++];
                else statement = "";

                if(c == '{') {
                    // Start a new code block, imbedded in the previous one:
                    BlockOfCode newBlock = new BlockOfCode();
                    currentCodeBlock.children.add(newBlock);
                    newBlock.parent = currentCodeBlock;
                    currentCodeBlock = newBlock;

                    // Add the current statement to the current code block as its leading statement;
                    currentCodeBlock.leadingStatement = statement;
                }
                else {
                    // Add the statement to the current block of code:
                    if(!statement.matches("\\s*")) currentCodeBlock.codeStatements.add(statement);

                    // End the current code block:
                    if(c == '}') currentCodeBlock = currentCodeBlock.parent;
                }
            }
        }
    }

    protected CodeTree(BlockOfCode root) {
        this.root = root;
    }

    public void print() {
        System.out.println(root.toString());
    }

    public int getCyclomaticComplexity() {
        return 1 + root.getCyclomaticComplexity();
    }

    public ArrayList<ParsedClass> getListOfClasses(String packageName, Path filePath, String[] importStatements) {
        return this.root.generateParsedClasses(packageName, filePath, importStatements);
    }

    public class BlockOfCode {

        public BlockOfCode parent = null;
        public ArrayList<BlockOfCode> children = new ArrayList<>();
        public String leadingStatement;
        public ArrayList<String> codeStatements = new ArrayList<>();

        public int getNumChildren(){
            int numChildren = children.size(); // i.e. degree
            for (BlockOfCode child : children) numChildren += child.getNumChildren();
            return numChildren;
        }

        public int getCyclomaticComplexity() {
            int complexity = leadingStatement.matches("(if|else if|while|for).*") ? 1 : 0;
            boolean isBranchingStatement;

            for(String codeLine : codeStatements) {
                isBranchingStatement = codeLine.matches(".*(if|else|while|for).*"); // Check for conditional statements
                isBranchingStatement |= codeLine.matches("\\w+ +\\w+ +=.+\\?.+:.+"); // Check for ternary operators
                isBranchingStatement |= codeLine.matches(".*(.*(==|!=|>=|<=).*).*"); // Check for simple predicates
                isBranchingStatement |= (leadingStatement.matches("switch.*") && codeLine.matches("(default|case\\s*\\w+\\s*):.*")); // Switch cases
                if(isBranchingStatement) complexity += 1;
            }

            for (BlockOfCode child : children) complexity += child.getCyclomaticComplexity();
            return complexity;
        }

        public ArrayList<ParsedClass> generateParsedClasses(String packageName, Path filePath, String[] importStatements) {
            ArrayList<ParsedClass> listOfClasses = new ArrayList<>();
            root.generateParsedClasses(listOfClasses, packageName, filePath, importStatements);
            return listOfClasses;
        }

        private void generateParsedClasses(ArrayList<ParsedClass> listOfClasses, String packageName, Path filePath, String[] importStatements) {
            if(StringParser.isClassDeclaration(this.leadingStatement)) {
                listOfClasses.add(new ParsedClass(this, packageName, filePath, importStatements));
                packageName = packageName + "." + StringParser.getDeclaredClassName(this.leadingStatement);
            }
            for(BlockOfCode child: this.children)
                child.generateParsedClasses(listOfClasses, packageName, filePath, importStatements);
        }

        public String toString() {
            return this.toString("");
        }

        private String toString(String indentation) {
            String str = "";
            if(StringParser.isClassDeclaration(this.leadingStatement)){
                str += "\n\n" + indentation + "(CLASS: " + StringParser.getDeclaredClassName(leadingStatement);
                ArrayList<String> inheritance = StringParser.getDeclaredClassInheritance(leadingStatement);
                if(inheritance.size() > 0) str += ", with parents: " + String.join(",", inheritance);
                str += ")";
            }

            str += "\n" + indentation + this.leadingStatement + " {";
            for(String s : this.codeStatements)
                str += "\n" + indentation + "    "  + s + ";";
            for(BlockOfCode c : this.children)
                str += c.toString(indentation + "    ");
            str += "\n" + indentation + "}";
            return str;
        }
    }
}