/*
 * IDE-Triangle v1.1 - Enhanced Multi-Backend Compiler
 * IDEMultiBackendCompiler.java 
 *
 * Version para curso Compiladores 2025
 */

package Triangle;

import Triangle.CodeGenerator.AbstractCodeGenerator;
import Triangle.CodeGenerator.Encoder;
import Triangle.CodeGenerator.LLVMEncoder;
import Triangle.SyntacticAnalyzer.SourceFile;
import Triangle.SyntacticAnalyzer.Scanner;
import Triangle.AbstractSyntaxTrees.Program;
import Triangle.SyntacticAnalyzer.Parser;
import Triangle.ContextualAnalyzer.Checker;

/** 
 * Enhanced IDE Triangle compiler with multi-backend support.
 * Supports both TAM and LLVM IR code generation.
 *
 * @author Enhanced for LLVM IR support
 */
public class IDEMultiBackendCompiler {

    public enum BackendType {
        TAM,
        LLVM_IR,
        BOTH
    }

    // <editor-fold defaultstate="collapsed" desc=" Methods ">
    /**
     * Creates a new instance of IDEMultiBackendCompiler.
     */
    public IDEMultiBackendCompiler() {
    }
    
    /**
     * Compile program with specified backend.
     * 
     * @param sourceName Path to the source file.
     * @param backendType Type of backend to use
     * @return True if compilation was successful.
     */
    public boolean compileProgram(String sourceName, BackendType backendType) {
        System.out.println("********** " +
                           "Triangle Multi-Backend Compiler (IDE-Triangle 1.1)" +
                           " **********");
        
        System.out.println("Syntactic Analysis ...");
        SourceFile source = new SourceFile(sourceName);
        Scanner scanner = new Scanner(source);
        report = new IDEReporter();
        Parser parser = new Parser(scanner, report);
        boolean success = false;
        
        rootAST = parser.parseProgram();
        if (report.numErrors == 0) {
            System.out.println("Contextual Analysis ...");
            Checker checker = new Checker(report);
            checker.check(rootAST);
            if (report.numErrors == 0) {
                System.out.println("Code Generation ...");
                
                success = generateCode(sourceName, backendType);
            }
        }

        if (success)
            System.out.println("Compilation was successful.");
        else
            System.out.println("Compilation was unsuccessful.");
        
        return success;
    }
    
    /**
     * Compile program with default backend (both TAM and LLVM IR).
     * 
     * @param sourceName Path to the source file.
     * @return True if compilation was successful.
     */
    public boolean compileProgram(String sourceName) {
        return compileProgram(sourceName, BackendType.BOTH);
    }
    
    /**
     * Generate code using the specified backend(s)
     */
    private boolean generateCode(String sourceName, BackendType backendType) {
        boolean success = true;
        
        try {
            switch (backendType) {
                case TAM:
                    success = generateTAMCode(sourceName);
                    break;
                    
                case LLVM_IR:
                    success = generateLLVMCode(sourceName);
                    break;
                    
                case BOTH:
                    success = generateTAMCode(sourceName) && generateLLVMCode(sourceName);
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
    private boolean generateTAMCode(String sourceName) {
        try {
            System.out.println("Generating TAM code...");
            AbstractCodeGenerator tamEncoder = new Encoder(report);
            tamEncoder.encodeRun(rootAST, false);
            
            if (report.numErrors == 0) {
                String tamFileName = sourceName.replace(".tri", tamEncoder.getFileExtension());
                tamEncoder.saveObjectProgram(tamFileName);
                System.out.println("TAM code saved to: " + tamFileName);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error generating TAM code: " + e.getMessage());
        }
        return false;
    }

    /**
     * Generate LLVM IR code
     */
    private boolean generateLLVMCode(String sourceName) {
        try {
            System.out.println("Generating LLVM IR code...");
            AbstractCodeGenerator llvmEncoder = new LLVMEncoder(report);
            llvmEncoder.encodeRun(rootAST, false);
            
            if (report.numErrors == 0) {
                String llvmFileName = sourceName.replace(".tri", llvmEncoder.getFileExtension());
                llvmEncoder.saveObjectProgram(llvmFileName);
                System.out.println("LLVM IR code saved to: " + llvmFileName);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error generating LLVM IR code: " + e.getMessage());
        }
        return false;
    }
      
    /**
     * Returns the line number where the first error is.
     * @return Line number.
     */
    public int getErrorPosition() {
        return(report.getFirstErrorPosition());
    }
        
    /**
     * Returns the root Abstract Syntax Tree.
     * @return Program AST (root).
     */
    public Program getAST() {
        return(rootAST);
    }
    
    /**
     * Get available backend types
     * @return Array of available backend types
     */
    public BackendType[] getAvailableBackends() {
        return BackendType.values();
    }
    
    /**
     * Get file extension for backend type
     * @param backendType The backend type
     * @return File extension including the dot
     */
    public String getBackendFileExtension(BackendType backendType) {
        switch (backendType) {
            case TAM:
                return ".tam";
            case LLVM_IR:
                return ".ll";
            case BOTH:
                return ".tam/.ll";
            default:
                return ".tam";
        }
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Attributes ">
    private Program rootAST;        // The Root Abstract Syntax Tree.    
    private IDEReporter report;     // Our ErrorReporter class.
    // </editor-fold>
}