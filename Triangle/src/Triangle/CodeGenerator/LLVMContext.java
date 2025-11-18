/*
 * LLVM IR Context Management for Triangle Compiler
 * Handles LLVM module structure, function management, and variable scoping
 */

package Triangle.CodeGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class LLVMContext {
    
    private StringBuilder moduleHeader;
    private StringBuilder globalDeclarations;
    private StringBuilder functionDeclarations;
    private StringBuilder mainFunction;
    

    private Stack<Map<String, LLVMValue>> scopeStack;
    private Map<String, String> functionSignatures;
    private int temporaryCounter;
    private int labelCounter;
    private int blockCounter;
    

    private String currentFunction;
    private StringBuilder currentFunctionBody;

    public LLVMContext() {
        this.moduleHeader = new StringBuilder();
        this.globalDeclarations = new StringBuilder();
        this.functionDeclarations = new StringBuilder();
        this.mainFunction = new StringBuilder();
        this.scopeStack = new Stack<>();
        this.functionSignatures = new HashMap<>();
        this.temporaryCounter = 0;
        this.labelCounter = 0;
        this.blockCounter = 0;

        initializeModule();
        pushScope();
    }
    
    private void initializeModule() {
        addStandardLibraryDeclarations();
        generateEolFunction();
        generateGetEolFunction();
        generatePutEolFunction();
        generateGetIntFunction();
    }
    
    private void addStandardLibraryDeclarations() {
        
        
        globalDeclarations.append("@last_char = global i32 0\n\n");
        globalDeclarations.append("; Format strings\n");
        globalDeclarations.append("@.str.int = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\", align 1\n");
        globalDeclarations.append("@.str.int.scanf = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1\n");
        globalDeclarations.append("@.str.char = private unnamed_addr constant [3 x i8] c\"%c\\00\", align 1\n");
        globalDeclarations.append("@.str.bool = private unnamed_addr constant [4 x i8] c\"%s\\0A\\00\", align 1\n");
        globalDeclarations.append("@.str.true = private unnamed_addr constant [5 x i8] c\"true\\00\", align 1\n");
        globalDeclarations.append("@.str.false = private unnamed_addr constant [6 x i8] c\"false\\00\", align 1\n\n"); 
        
        functionDeclarations.append("; Standard Library Functions\n");
        functionDeclarations.append("declare i32 @printf(i8*, ...)\n");
        functionDeclarations.append("declare i32 @scanf(i8*, ...)\n");
        functionDeclarations.append("declare i8* @malloc(i64)\n");
        functionDeclarations.append("declare void @free(i8*)\n");
        functionDeclarations.append("declare i32 @putchar(i32)\n");
        functionDeclarations.append("declare i32 @getchar()\n\n");
        
        
    }
    
    private void generateEolFunction() {
        startFunction("eol", "i1", "");

        // Function body
        addInstruction("%last = load i32, i32* @last_char");
        addInstruction("%cmp = icmp eq i32 %last, 10  ; '\\n'");
        addInstruction("br i1 %cmp, label %true, label %check_eof");

        addInstruction("check_eof:");
        addInstruction("%cmp2 = icmp eq i32 %last, -1  ; EOF");
        addInstruction("br i1 %cmp2, label %true, label %false");

        addInstruction("true:");
        addInstruction("ret i1 true");

        addInstruction("false:");
        addInstruction("ret i1 false");

        endFunction();
    }

    private void generateGetEolFunction() {
        startFunction("geteol", "void", "");

        // Function body
        addInstruction("%ch = call i32 @getchar()");
        addInstruction("store i32 %ch, i32* @last_char");
        addInstruction("ret void");

        endFunction();
    }

    private void generatePutEolFunction() {
        startFunction("puteol", "void", "");

        // Function body
        addInstruction("%result = call i32 @putchar(i32 10)  ; '\\n'");
        addInstruction("ret void");

        endFunction();
    }
    
    private void generateGetIntFunction() {
    startFunction("getint", "void", "i32* %ptr");
    
    // Usar el formato correcto para scanf
    addInstruction("%result = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str.int.scanf, i32 0, i32 0), i32* %ptr)");
    addInstruction("ret void");
    
    endFunction();
    }
    
    public void pushScope() {
        scopeStack.push(new HashMap<>());
    }
    
    public void popScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }
    
    public void addVariable(String name, LLVMValue value) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().put(name, value);
        }
    }
    
    public LLVMValue lookupVariable(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, LLVMValue> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }
    
    public String generateTemporary() {
        return "%temp" + (temporaryCounter++);
    }
    
    public String generateLabel() {
        return "label" + (labelCounter++);
    }
    
    public String generateBlock() {
        return "block" + (blockCounter++);
    }
    
    public void startFunction(String functionName, String returnType, String parameters) {
        currentFunction = functionName;
        currentFunctionBody = new StringBuilder();
        functionDeclarations.append("define ").append(returnType).append(" @")
                           .append(functionName).append("(").append(parameters).append(") {\n");
        functionDeclarations.append("entry:\n");
        
        functionSignatures.put(functionName, returnType + "(" + parameters + ")");
    }
    
    public void endFunction() {
        if (currentFunctionBody != null) {
            functionDeclarations.append(currentFunctionBody.toString());
            functionDeclarations.append("}\n\n");
            currentFunction = null;
            currentFunctionBody = null;
        }
    }
    
    public void addInstruction(String instruction) {
        if (currentFunctionBody != null) {
            currentFunctionBody.append("  ").append(instruction).append("\n");
        } else {
            mainFunction.append("  ").append(instruction).append("\n");
        }
    }
    
    public void startMainFunction() {
        mainFunction.append("define i32 @main() {\n");
        mainFunction.append("entry:\n");
    }
    
    public void endMainFunction() {
        mainFunction.append("  ret i32 0\n");
        mainFunction.append("}\n");
    }
    
    public String generateCompleteModule() {
        StringBuilder complete = new StringBuilder();
        complete.append(moduleHeader.toString());
        complete.append(globalDeclarations.toString());
        complete.append(functionDeclarations.toString());
        complete.append(mainFunction.toString());
        return complete.toString();
    }
    
    // Helper class for LLVM values
    public static class LLVMValue {
        public String name;
        public String type;
        public boolean isTemporary;
        public boolean isGlobal;
        
        public LLVMValue(String name, String type, boolean isTemporary, boolean isGlobal) {
            this.name = name;
            this.type = type;
            this.isTemporary = isTemporary;
            this.isGlobal = isGlobal;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}