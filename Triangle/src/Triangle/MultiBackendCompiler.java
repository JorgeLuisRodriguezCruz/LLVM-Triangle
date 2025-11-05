/*
 * Multi-Backend Triangle Compiler
 * Supports both TAM and LLVM IR code generation
 */

package Triangle;

import Triangle.AbstractSyntaxTrees.Program;
import Triangle.CodeGenerator.AbstractCodeGenerator;
import Triangle.CodeGenerator.Encoder;
import Triangle.CodeGenerator.LLVMEncoder;
import Triangle.ContextualAnalyzer.Checker;
import Triangle.SyntacticAnalyzer.Parser;
import Triangle.SyntacticAnalyzer.Scanner;
import Triangle.SyntacticAnalyzer.SourceFile;
import Triangle.TreeDrawer.Drawer;

/**
 * Enhanced Triangle compiler with support for multiple backends
 * 
 * @version 2.2 2025/11/02
 * @author Enhanced for LLVM IR support
 */
public class MultiBackendCompiler {

    public enum BackendType {
        TAM,
        LLVM_IR,
        BOTH
    }

    private static Scanner scanner;
    private static Parser parser;
    private static Checker checker;
    private static ErrorReporter reporter;
    private static Drawer drawer;

    /** The AST representing the source program. */
    private static Program theAST;

    /**
     * Compile the source program to the specified backend(s).
     *
     * @param sourceName    the name of the file containing the source program.
     * @param backendType   the type of backend to use (TAM, LLVM_IR, or BOTH)
     * @param showingAST    true iff the AST is to be displayed after contextual analysis
     * @param showingTable  true iff the object description details are to be displayed
     * @return true iff the source program is free of compile-time errors, otherwise false.
     */
    public static boolean compileProgram(String sourceName, BackendType backendType,
                                       boolean showingAST, boolean showingTable) {

        System.out.println("********** " +
                           "Triangle Multi-Backend Compiler (Java Version 2.2)" +
                           " **********");

        System.out.println("Syntactic Analysis ...");
        SourceFile source = new SourceFile(sourceName);

        if (source == null) {
            System.out.println("Can't access source file " + sourceName);
            System.exit(1);
        }

        scanner = new Scanner(source);
        reporter = new ErrorReporter();
        parser = new Parser(scanner, reporter);
        checker = new Checker(reporter);
        drawer = new Drawer();

        theAST = parser.parseProgram();                // 1st pass
        if (reporter.numErrors == 0) {
            System.out.println("Contextual Analysis ...");
            checker.check(theAST);                     // 2nd pass
            if (showingAST) {
                drawer.draw(theAST);
            }
            if (reporter.numErrors == 0) {
                System.out.println("Code Generation ...");
                
                // Generate code for the specified backend(s)
                boolean success = generateCode(sourceName, backendType, showingTable);
                if (!success) {
                    return false;
                }
            }
        }

        boolean successful = (reporter.numErrors == 0);
        if (successful) {
            System.out.println("Compilation was successful.");
        } else {
            System.out.println("Compilation was unsuccessful.");
        }
        return successful;
    }

    /**
     * Generate code using the specified backend(s)
     */
    private static boolean generateCode(String sourceName, BackendType backendType, boolean showingTable) {
        boolean success = true;
        
        try {
            switch (backendType) {
                case TAM:
                    success = generateTAMCode(sourceName, showingTable);
                    break;
                    
                case LLVM_IR:
                    success = generateLLVMCode(sourceName, showingTable);
                    break;
                    
                case BOTH:
                    success = generateTAMCode(sourceName, showingTable) && 
                             generateLLVMCode(sourceName, showingTable);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error during code generation: " + e.getMessage());
            e.printStackTrace();
            success = false;
        }
        
        return success;
    }

    /**
     * Generate TAM code
     */
    private static boolean generateTAMCode(String sourceName, boolean showingTable) {
        try {
            System.out.println("Generating TAM code...");
            AbstractCodeGenerator tamEncoder = new Encoder(reporter);
            tamEncoder.encodeRun(theAST, showingTable);
            
            String tamFileName = sourceName.replace(".tri", tamEncoder.getFileExtension());
            tamEncoder.saveObjectProgram(tamFileName);
            
            System.out.println("TAM code saved to: " + tamFileName);
            return true;
        } catch (Exception e) {
            System.err.println("Error generating TAM code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate LLVM IR code
     */
    private static boolean generateLLVMCode(String sourceName, boolean showingTable) {
        try {
            System.out.println("Generating LLVM IR code...");
            AbstractCodeGenerator llvmEncoder = new LLVMEncoder(reporter);
            llvmEncoder.encodeRun(theAST, showingTable);
            
            String llvmFileName = sourceName.replace(".tri", llvmEncoder.getFileExtension());
            llvmEncoder.saveObjectProgram(llvmFileName);
            
            System.out.println("LLVM IR code saved to: " + llvmFileName);
            return true;
        } catch (Exception e) {
            System.err.println("Error generating LLVM IR code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Triangle multi-backend compiler main program.
     *
     * @param args command-line arguments:
     *             args[0] - source filename
     *             args[1] - backend type (optional): "tam", "llvm", or "both" (default: "both")
     */
    public static void main(String[] args) {
        boolean compiledOK;
        String sourceName;
        BackendType backendType = BackendType.BOTH; // Default to both backends

        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java Triangle.MultiBackendCompiler filename [backend]");
            System.out.println("  backend options: tam, llvm, both (default: both)");
            System.exit(1);
        }

        sourceName = args[0];
        
        if (args.length == 2) {
            switch (args[1].toLowerCase()) {
                case "tam":
                    backendType = BackendType.TAM;
                    break;
                case "llvm":
                    backendType = BackendType.LLVM_IR;
                    break;
                case "both":
                    backendType = BackendType.BOTH;
                    break;
                default:
                    System.out.println("Unknown backend type: " + args[1]);
                    System.out.println("Valid options: tam, llvm, both");
                    System.exit(1);
            }
        }

        compiledOK = compileProgram(sourceName, backendType, false, false);
        
        if (!compiledOK) {
            System.exit(1);
        }
    }
}