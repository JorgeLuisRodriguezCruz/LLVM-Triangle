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
import Triangle.StdEnvironment;
import java.awt.SystemColor;

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

    
    @Override
    public Object visitProgram(Program ast, Object o) {
        return ast.C.visit(this, o);
    }
    
    // Commands
    @Override
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        String exprResult = (String) ast.E.visit(this, o);
        String Type = (String) ast.E.type.visit(this, o);
        String ptr = (String) ast.V.visit(this, o); 
        llvmContext.addInstruction("store " + Type + " " + exprResult + ", " + Type + "* " + ptr);
        
        return null;
    }
    
    @Override
    public Object visitCallCommand(CallCommand ast, Object o) {
        String procName = ast.I.spelling;
        String args = (String) ast.APS.visit(this, o);
        if (procName.equals("putint")) {
            llvmContext.addInstruction("call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.int, i32 0, i32 0), i32 " + args + ")");  
        }
        else if(procName.equals("put"))
        {
            if (args.startsWith("%")) {
                String tmpLoad = llvmContext.generateTemporary();
                String tmpExt = llvmContext.generateTemporary();
                //llvmContext.addInstruction(tmpLoad + " = load i8, i8* " + args);
                llvmContext.addInstruction(tmpExt + " = sext i8 " + args + " to i32");
                
                llvmContext.addInstruction("call i32 @putchar(i32 " + tmpExt + ")");
            }
            else if (args.startsWith("'")) {
                int ascii = (int) args.charAt(1);
                llvmContext.addInstruction("call i32 @putchar(i32 " + ascii + ")");
            }
            else {
                llvmContext.addInstruction("call i32 @putchar(i32 " + args + ")");
            }
        }
        else if(procName.equals("get"))
        {
                String tmpCall = llvmContext.generateTemporary();
                String tmpTrunc = llvmContext.generateTemporary();
                llvmContext.addInstruction(tmpCall + " = call i32 @getchar()");
                // truncate to i8
                llvmContext.addInstruction(tmpTrunc + " = trunc i32 " + tmpCall + " to i8");
                // store i8 into pointer
                llvmContext.addInstruction("store i8 " + tmpTrunc + ", i8* " + args);
        }
        else {
            llvmContext.addInstruction("call void @" + procName + "(" + (args != null ? args : "") + ")");
        }
        
        return null;
    }
    
    @Override
    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        return null;
    }
    
    @Override
    public Object visitIfCommand(IfCommand ast, Object o) {
        String condition = (String) ast.E.visit(this, o);
        
        String thenLabel = llvmContext.generateLabel();
        String elseLabel = llvmContext.generateLabel();
        String endLabel = llvmContext.generateLabel();
        
        llvmContext.addInstruction("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        llvmContext.addInstruction(thenLabel + ":");
        ast.C1.visit(this, o);
        llvmContext.addInstruction("br label %" + endLabel);
        
        llvmContext.addInstruction(elseLabel + ":");
        ast.C2.visit(this, o);
        llvmContext.addInstruction("br label %" + endLabel);
        
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
        
        llvmContext.addInstruction("br label %" + loopLabel);

        llvmContext.addInstruction(loopLabel + ":");
        String condition = (String) ast.E.visit(this, o);
        llvmContext.addInstruction("br i1 " + condition + ", label %" + bodyLabel + ", label %" + endLabel);
        
        llvmContext.addInstruction(bodyLabel + ":");
        ast.C.visit(this, o);
        llvmContext.addInstruction("br label %" + loopLabel);
        
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
        String operator =(String) ast.O.visit(this, o);
        
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
            case ">=":
                llvmOp = "icmp sge";
                break;
            case "<=":
                llvmOp = "icmp sle";
                break;
            case "\\/": 
                llvmOp = "or"; 
                break;
            case "/\\": 
                llvmOp = "and"; 
                break;
            default:
                llvmOp = "FALTAAGREGAR"; // fallback
        }
        
        if (llvmOp.startsWith("icmp")) {
            llvmContext.addInstruction(result + " = " + llvmOp + " i32 " + left + ", " + right);
        } else {
            llvmContext.addInstruction(result + " = " + llvmOp + " "+ ast.E1.type.visit(this, o)+ " " + left + ", " + right);
        }
        
        return result;
    }
    
    @Override
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        
        String ptr = (String) ast.V.visit(this, o);

        if (ptr.equals("true") || ptr.equals("false") || ptr.startsWith("'") || ptr.matches("[0-9]+"))   {
            return ptr;
        }
        
        if (ptr.startsWith("%") && !ptr.matches("%[a-zA-Z_][a-zA-Z0-9_]*")) {
            return ptr;
        }

        String type = (String) ast.type.visit(this, o);   // i1, i8, i32
        String temp = llvmContext.generateTemporary();

        llvmContext.addInstruction(temp + " = load " + type + ", " + type + "* " + ptr);
        return temp;

    }
    
    // Declarations
    @Override
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        
        String varType =(String) ast.T.visit(this, o); 
        
        llvmContext.addInstruction("%" + varName + " = alloca " + varType);
        
        LLVMContext.LLVMValue value = new LLVMContext.LLVMValue(varName, varType, false, false);
        llvmContext.addVariable(varName, value);
        
        return null;
    }
    
    @Override
    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        String constName = (String) ast.I.visit(this, o);
        String constValue = (String) ast.E.visit(this, o);
        String varType = (String) ast.E.type.visit(this, o);

        llvmContext.addInstruction( constName + " = alloca " + varType);
        llvmContext.addInstruction("store " + varType + " " + constValue + ", " + varType + "* " + constName);
        
        LLVMContext.LLVMValue value = new LLVMContext.LLVMValue("%" + constName, varType, false, false);
        llvmContext.addVariable(constName, value);
        
        return null;
    }

    @Override
    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        ast.D1.visit(this, o);
        ast.D2.visit(this, o);
        return null;
    }

    public Object visitArrayExpression(ArrayExpression ast, Object o) 
    { 
        
        return  "AE-sin-implementar"; 
    }
    public Object visitCallExpression(CallExpression ast, Object o) 
    {

        String funcName = ast.I.spelling;  // add

        String args =  (String) ast.APS.visit(this, o);  // "i32 3, i32 4"

        String retType = (String) ast.type.visit(this, o);

        String tmp = llvmContext.generateTemporary();

        llvmContext.addInstruction(tmp + " = call " + retType + " @" + funcName + "(" + args + ")");

        return tmp;
    
    }
    
    
    public Object visitCharacterExpression(CharacterExpression ast, Object o) 
    {
        //retornar solo el ascii da error por alguna extraña razon
        return ast.CL.spelling;
    }
    public Object visitEmptyExpression(EmptyExpression ast, Object o) { 
        return null; 
    }
    
    public Object visitIfExpression(IfExpression ast, Object o) { 
        
    String condition = (String) ast.E1.visit(this, o);
    
    String thenLabel = llvmContext.generateLabel();
    String elseLabel = llvmContext.generateLabel();
    String endLabel = llvmContext.generateLabel();
    String resultTemp = llvmContext.generateTemporary();
    
    llvmContext.addInstruction("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
    
    llvmContext.addInstruction(thenLabel + ":");
    String thenResult = (String) ast.E2.visit(this, o);
    llvmContext.addInstruction("br label %" + endLabel);
    
    llvmContext.addInstruction(elseLabel + ":");
    String elseResult = (String) ast.E3.visit(this, o);
    llvmContext.addInstruction("br label %" + endLabel);
    
    llvmContext.addInstruction(endLabel + ":");
    llvmContext.addInstruction(resultTemp + " = phi i32 [ " + thenResult + ", %" + thenLabel + " ], [ " + elseResult + ", %" + elseLabel + " ]");
    
    return resultTemp; 
    
    }
    
    
    public Object visitLetExpression(LetExpression ast, Object o) { 
        llvmContext.pushScope();
    
        // Process declarations
        ast.D.visit(this, o);

        // Process expression
        String result = (String) ast.E.visit(this, o);

        llvmContext.popScope();
        return result; 
    
    }
    
    
    public Object visitRecordExpression(RecordExpression ast, Object o) {     // Para records, necesitamos allocar memoria y almacenar los campos
    String recordType = (String) ast.type.visit(this, o);
    String recordPtr = llvmContext.generateTemporary();

    
    llvmContext.addInstruction(recordPtr + " = alloca " + recordType);
    
    ast.RA.visit(this, recordPtr);
    
    return recordPtr;}
    
    
    public Object visitUnaryExpression(UnaryExpression ast, Object o) { 
        //ast.
        return  "UE-sin-implementar"; }
    
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) { return  "BOD-sin-implementar"; }
    
    
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        
        
       String name = ast.I.spelling;  // "add"
       String retType = (String) ast.T.visit(this, o);  // i32

       String params = (String) ast.FPS.visit(this, o);  // genera: "i32 %a, i32 %b"

       llvmContext.startFunction(name, retType, params);

       String result = (String) ast.E.visit(this, o);  // cuerpo
       
       

       llvmContext.addInstruction("ret " + retType + " " + result);

       llvmContext.endFunction();

       return null;
    }
    
    
    
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) { return  "PD-sin-implementar"; }
    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) { return  "TD-sin-implementar"; }
    
    
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) { return  "UOD-sin-implementar"; }
    
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) { return  "MAA-sin-implementar"; }
    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) { return  "SAA-sin-implementar"; }
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) { return  "MRA-sin-implementar"; }
    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) { return  "SRA-sin-implementar"; }
    
    
    
    public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) { 
        return ast.T.visit(this, o) + " %" + ast.I.spelling;
    }
    
    
    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) { return  "FFP-sin-implementar"; }
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) { return  "PFP-sin-implementar"; }
    
    
    
    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) { 
        return ast.T.visit(this, o) + " %" + ast.I.spelling;
    }
    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) { 
        return ""; 
    }
    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) {
        return ast.FP.visit(this, o) + ","+ ast.FPS.visit(this, o);
    }
    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) { 
        return ast.FP.visit(this, o);
    }
    
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        //return ast.E.type.visit(this, o) +" "+ ast.E.visit(this, o);
        return ast.E.visit(this, o);
    }
    
    
    
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) { return  "FAP-sin-implementar"; }
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) { return  "PAP-sin-implementar"; }
    
    
    
    public Object visitVarActualParameter(VarActualParameter ast, Object o) 
    { 
        SimpleVname v = (SimpleVname) ast.V;
        return "%" + v.I.spelling; 
    }
    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) 
    {
        return ""; 
    }
    public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) 
    { 
        return ast.AP.visit(this, o) + ","+ ast.APS.visit(this, o);
    }
    
    
    
    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o)
    {
        return ast.AP.visit(this, o);
    }
    
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) { return  "ATD-sin-implementar"; }
    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) { return  "ATD2-sin-implementar"; }
    
    
    
    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
        return mapTypeToLLVM("bool");
    }
    
    
    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) { 
        return mapTypeToLLVM("char");
    }
    
    
    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) 
    {  
        return "i32";  
    }
    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) { return  "STD-sin-implementar"; }
    
    
    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) { 
        return mapTypeToLLVM("integer");
    }
    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) { return "RTD-sin-implementar"; }
    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) { return  "MFTD-sin-implementar"; }
    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) { return  "SFTD-sin-implementar"; }
    
    
    
    
    
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) { 
        return ast.spelling; 
    }
    public Object visitIdentifier(Identifier ast, Object o) { 
        return ast.spelling;
    }
    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) { 
        return ast.spelling;
    }
    public Object visitOperator(Operator ast, Object o) {
        return ast.spelling;  // es un simbolo como +,-,*,/, etc etc. Simplemente devuelve el operador como string.
    }
    
    
    public Object visitDotVname(DotVname ast, Object o) {   
        String recordPtr = (String) ast.V.visit(this, o);
        String fieldName = ast.I.spelling;
    
        String fieldPtr = llvmContext.generateTemporary();
        llvmContext.addInstruction(fieldPtr + " = getelementptr inbounds i8, i8* " + recordPtr + ", i32 0");
    
        return fieldPtr;
    }
    
    
    
    
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        if ("true".equals(ast.I.spelling) || "false".equals(ast.I.spelling)) {
            return ast.I.spelling; // literal boolean
        }
        return "%" + ast.I.spelling; 

    }

    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
    
        String arrayPtr = (String) ast.V.visit(this, o);
        String index = (String) ast.E.visit(this, o);

        String elementPtr = llvmContext.generateTemporary();
        llvmContext.addInstruction(elementPtr + " = getelementptr i32, i32* " + arrayPtr + ", i32 " + index);
        return elementPtr;
    }
    
    
}


