/*
 * Abstract interface for code generators in Triangle compiler
 * Supports multiple backends (TAM, LLVM IR, etc.)
 */

package Triangle.CodeGenerator;

import Triangle.AbstractSyntaxTrees.Program;
import Triangle.ErrorReporter;

public abstract class AbstractCodeGenerator {
    
    protected ErrorReporter reporter;
    
    public AbstractCodeGenerator(ErrorReporter reporter) {
        this.reporter = reporter;
    }
    
    /**
     * Generate code for the given AST
     * @param theAST The program AST to compile
     * @param showingTable Whether to show symbol table details
     */
    public abstract void encodeRun(Program theAST, boolean showingTable);
    
    /**
     * Save the generated code to a file
     * @param fileName The output file name
     */
    public abstract void saveObjectProgram(String fileName);
    
    /**
     * Get the file extension for this code generator
     * @return File extension (e.g., ".tam", ".ll")
     */
    public abstract String getFileExtension();
}