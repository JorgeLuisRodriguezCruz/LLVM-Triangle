/*
 * LLVM IR Code Generator for Triangle Compiler
 * Generates LLVM IR text (.ll files) from Triangle AST
 */

package Triangle.CodeGenerator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import Triangle.ErrorReporter;
import Triangle.AbstractSyntaxTrees.*;

public class LLVMEncoder extends AbstractCodeGenerator implements Visitor {
    
    private LLVMContext llvmContext;
    private boolean tableDetailsReqd;
    
    public LLVMEncoder(ErrorReporter reporter) {
        super(reporter);
        this.llvmContext = new LLVMContext();
    }
    
    @Override
    public void encodeRun(Program theAST, boolean showingTable) {
        tableDetailsReqd = showingTable;
        
        // Start main function
        llvmContext.startMainFunction();
        
        // Generate code for the program
        theAST.visit(this, null);
        
        // End main function
        llvmContext.endMainFunction();
    }
    
    @Override
    public void saveObjectProgram(String fileName) {
        try {
            FileOutputStream fileStream = new FileOutputStream(fileName);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileStream, "UTF-8");
            PrintWriter writer = new PrintWriter(outputWriter);
            
            writer.print(llvmContext.generateCompleteModule());
            writer.close();
            outputWriter.close();
            fileStream.close();
            
        } catch (IOException e) {
            System.err.println("Error writing LLVM IR file: " + e.getMessage());
        }
    }
    
    @Override
    public String getFileExtension() {
        return ".ll";
    }
    
    // Helper method to map Triangle types to LLVM types
    private String mapTypeToLLVM(String triangleType) {
        switch (triangleType.toLowerCase()) {
            case "integer":
            case "int":
                return "i32";
            case "boolean":
            case "bool":
                return "i1";
            case "character":
            case "char":
                return "i8";
            default:
                return "i32"; // Default fallback
        }
    }
    
    // Visitor methods for AST nodes
    
    @Override
    public Object visitProgram(Program ast, Object o) {
        return ast.C.visit(this, o);
    }
    
    // Commands
    @Override
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        // Generate code for the expression
        String exprResult = (String) ast.E.visit(this, o);
        
        // Generate code for the variable assignment
        //String varName = ast.V.toString(); // Simplified
        SimpleVname v = (SimpleVname) ast.V;
        llvmContext.addInstruction("store i32 " + exprResult + ", i32* %" + v.I.spelling);
        
        return null;
    }
    
    @Override
    public Object visitCallCommand(CallCommand ast, Object o) {
        // Handle procedure calls
        String procName = ast.I.spelling;
        
        // Generate arguments
        //String args = (String) ast.APS.visit(this, o);
        
        // Generate call instruction
        if (procName.equals("putint")) {
            String temp =(String) ast.APS.visit(this, o); // para el load de la variable.
            // Special case for output
            llvmContext.addInstruction("call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.int, i32 0, i32 0), i32 " + temp + ")");
        } else {
            //llvmContext.addInstruction("call void @" + procName + "(" + (args != null ? args : "") + ")");
        }
        
        return null;
    }
    
    @Override
    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        return null;
    }
    
    @Override
    public Object visitIfCommand(IfCommand ast, Object o) {
        // Generate condition
        String condition = (String) ast.E.visit(this, o);
        
        // Generate labels
        String thenLabel = llvmContext.generateLabel();
        String elseLabel = llvmContext.generateLabel();
        String endLabel = llvmContext.generateLabel();
        
        // Branch instruction
        llvmContext.addInstruction("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        // Then block
        llvmContext.addInstruction(thenLabel + ":");
        ast.C1.visit(this, o);
        llvmContext.addInstruction("br label %" + endLabel);
        
        // Else block
        llvmContext.addInstruction(elseLabel + ":");
        ast.C2.visit(this, o);
        llvmContext.addInstruction("br label %" + endLabel);
        
        // End block
        llvmContext.addInstruction(endLabel + ":");
        
        return null;
    }
    
    @Override
    public Object visitLetCommand(LetCommand ast, Object o) {
        llvmContext.pushScope();
        
        // Process declarations
        ast.D.visit(this, o);
        
        // Process command
        ast.C.visit(this, o);
        
        llvmContext.popScope();
        return null;
    }
    
    @Override
    public Object visitSequentialCommand(SequentialCommand ast, Object o) {
        ast.C1.visit(this, o);
        ast.C2.visit(this, o);
        return null;
    }
    
    @Override
    public Object visitWhileCommand(WhileCommand ast, Object o) {
        String loopLabel = llvmContext.generateLabel();
        String bodyLabel = llvmContext.generateLabel();
        String endLabel = llvmContext.generateLabel();
        
        // Jump to loop condition
        llvmContext.addInstruction("br label %" + loopLabel);
        
        // Loop condition
        llvmContext.addInstruction(loopLabel + ":");
        String condition = (String) ast.E.visit(this, o);
        llvmContext.addInstruction("br i1 " + condition + ", label %" + bodyLabel + ", label %" + endLabel);
        
        // Loop body
        llvmContext.addInstruction(bodyLabel + ":");
        ast.C.visit(this, o);
        llvmContext.addInstruction("br label %" + loopLabel);
        
        // End
        llvmContext.addInstruction(endLabel + ":");
        
        return null;
    }
    
    // Expressions
    @Override
    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        return ast.IL.spelling; // Return the literal value
    }
    
    @Override
    public Object visitBinaryExpression(BinaryExpression ast, Object o) {
        String left = (String) ast.E1.visit(this, o);
        String right = (String) ast.E2.visit(this, o);
        String result = llvmContext.generateTemporary();
        String operator = ast.O.spelling;
        
        String llvmOp;
        switch (operator) {
            case "+":
                llvmOp = "add";
                break;
            case "-":
                llvmOp = "sub";
                break;
            case "*":
                llvmOp = "mul";
                break;
            case "/":
                llvmOp = "sdiv";
                break;
            case "=":
                llvmOp = "icmp eq";
                break;
            case "<":
                llvmOp = "icmp slt";
                break;
            case ">":
                llvmOp = "icmp sgt";
                break;
            default:
                llvmOp = "add"; // fallback
        }
        
        if (llvmOp.startsWith("icmp")) {
            llvmContext.addInstruction(result + " = " + llvmOp + " i32 " + left + ", " + right);
        } else {
            llvmContext.addInstruction(result + " = " + llvmOp + " i32 " + left + ", " + right);
        }
        
        return result;
    }
    
    @Override
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        String varName = ast.V.toString();
        SimpleVname v =(SimpleVname) ast.V;
        String temp = llvmContext.generateTemporary();
        llvmContext.addInstruction(temp + " = load i32, i32* %" + v.I.spelling);
        return temp;
    }
    
    // Declarations
    @Override
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        String varType = mapTypeToLLVM("integer"); // Simplified type mapping
        
        llvmContext.addInstruction("%" + varName + " = alloca " + varType);
        
        LLVMContext.LLVMValue value = new LLVMContext.LLVMValue("%" + varName, varType, false, false);
        llvmContext.addVariable(varName, value);
        
        return null;
    }
    
    @Override
    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        String constName = ast.I.spelling;
        String constValue = (String) ast.E.visit(this, o);
        
        // In LLVM, we can handle constants as regular variables initialized with the constant value
        String varType = mapTypeToLLVM("integer");
        llvmContext.addInstruction("%" + constName + " = alloca " + varType);
        llvmContext.addInstruction("store " + varType + " " + constValue + ", " + varType + "* %" + constName);
        
        LLVMContext.LLVMValue value = new LLVMContext.LLVMValue("%" + constName, varType, false, false);
        llvmContext.addVariable(constName, value);
        
        return null;
    }
    
    // Stub implementations for remaining visitor methods
    // (These would need to be fully implemented for a complete compiler)
    
    @Override
    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        ast.D1.visit(this, o);
        ast.D2.visit(this, o);
        return null;
    }
    
    // Add minimal implementations for other required visitor methods
    public Object visitArrayExpression(ArrayExpression ast, Object o) { return null; }
    public Object visitCallExpression(CallExpression ast, Object o) { return null; }
    public Object visitCharacterExpression(CharacterExpression ast, Object o) { return "nna"; }
    public Object visitEmptyExpression(EmptyExpression ast, Object o) { return null; }
    public Object visitIfExpression(IfExpression ast, Object o) { return null; }
    public Object visitLetExpression(LetExpression ast, Object o) { return null; }
    public Object visitRecordExpression(RecordExpression ast, Object o) { return null; }
    public Object visitUnaryExpression(UnaryExpression ast, Object o) { return null; }
    
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) { return null; }
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) { return null; }
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) { return null; }
    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) { return null; }
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) { return null; }
    
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) { return null; }
    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) { return null; }
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) { return null; }
    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) { return null; }
    
    public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) { return "wo"; }
    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) { return null; }
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) { return null; }
    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) { return "wi"; }
    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) { return null; }
    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) { return null; }
    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) { return "wowu"; }
    
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        return ast.E.visit(this, o);
        //return "de";
            }
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) { return "b"; }
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) { return "c"; }
    public Object visitVarActualParameter(VarActualParameter ast, Object o) { return "d"; }
    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) { return "e"; }
    public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) { return "f"; }
    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o)
    {
        return ast.AP.visit(this, o);
        
        //return "g"; 
    }
    
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) { return null; }
    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) { return null; }
    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) { return null; }
    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) { return null; }
    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) { return null; }
    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) { return null; }
    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) { return null; }
    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) { return null; }
    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) { return null; }
    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) { return null; }
    
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) { return "we"; }
    public Object visitIdentifier(Identifier ast, Object o) { 
        //return ast.spelling;
        return "wa"; 
    }
    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) { return null; }
    public Object visitOperator(Operator ast, Object o) { return null; }
    
    public Object visitDotVname(DotVname ast, Object o) { return null; }
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        return "ab";
        //String varName = ast.I.spelling;
        
        //return varName;
        // Si la variable aún no tiene un nombre LLVM asignado, la registramos
        //String llvmVarName = llvmContext.lookupVariable(varName);
    }
    public Object visitSubscriptVname(SubscriptVname ast, Object o) { return "cd"; }
}