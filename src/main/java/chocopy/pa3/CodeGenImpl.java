package chocopy.pa3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chocopy.common.analysis.SymbolTable;
import chocopy.common.analysis.AbstractNodeAnalyzer;
import chocopy.common.analysis.types.ValueType;
import chocopy.common.astnodes.*;
import chocopy.common.codegen.*;

import static chocopy.common.analysis.types.Type.STR_TYPE;
import static chocopy.common.codegen.RiscVBackend.Register.*;

/**
 * This is where the main implementation of PA3 will live.
 *
 * A large part of the functionality has already been implemented
 * in the base class, CodeGenBase. Make sure to read through that
 * class, since you will want to use many of its fields
 * and utility methods in this class when emitting code.
 *
 * Also read the PDF spec for details on what the base class does and
 * what APIs it exposes for its sub-class (this one). Of particular
 * importance is knowing what all the SymbolInfo classes contain.
 */
public class CodeGenImpl extends CodeGenBase {

    /** A code generator emitting instructions to BACKEND. */
    public CodeGenImpl(RiscVBackend backend) {
        super(backend);
    }

    /** Operation on None. */
    private final Label errorNone = new Label("error.None");
    /** Division by zero. */
    private final Label errorDiv = new Label("error.Div");
    /** Index out of bounds. */
    private final Label errorOob = new Label("error.OOB");

    /**
     * Emits the top level of the program.
     *
     * This method is invoked exactly once, and is surrounded
     * by some boilerplate code that: (1) initializes the heap
     * before the top-level begins and (2) exits after the top-level
     * ends.
     *
     * You only need to generate code for statements.
     *
     * @param statements top level statements
     */
    protected void emitTopLevel(List<Stmt> statements) {
        StmtAnalyzer stmtAnalyzer = new StmtAnalyzer(null);
        backend.emitADDI(SP, SP, -2 * backend.getWordSize(),
                         "Saved FP and saved RA (unused at top level).");
        backend.emitSW(ZERO, SP, 0, "Top saved FP is 0.");
        backend.emitSW(ZERO, SP, 4, "Top saved RA is 0.");
        backend.emitADDI(FP, SP, 2 * backend.getWordSize(),
                         "Set FP to previous SP.");

        for (Stmt stmt : statements) {
            stmt.dispatch(stmtAnalyzer);
        }
        backend.emitLI(A0, EXIT_ECALL, "Code for ecall: exit");
        backend.emitEcall(null);
    }

    /**
     * Emits the code for a function described by FUNCINFO.
     *
     * This method is invoked once per function and method definition.
     * At the code generation stage, nested functions are emitted as
     * separate functions of their own. So if function `bar` is nested within
     * function `foo`, you only emit `foo`'s code for `foo` and only emit
     * `bar`'s code for `bar`.
     */
    protected void emitUserDefinedFunction(FuncInfo funcInfo) {
        backend.emitGlobalLabel(funcInfo.getCodeLabel());
        StmtAnalyzer stmtAnalyzer = new StmtAnalyzer(funcInfo);
        backend.emitADDI(SP,SP,"-@f.size","Reserve space for stack frame.");
        backend.emitSW(RA,SP,"@f.size-4","return address");
        backend.emitSW(FP,SP,"@f.size-8","control link");
        backend.emitADDI(FP,SP,"@f.size","New fp is at old SP");

        RiscVBackend.Register[] rgs= new RiscVBackend.Register[]{A0, A1,A2,A3,A4,A5,A6};
        int i=0;
        for (StackVarInfo stv : funcInfo.getLocals()) {
            if (stv.getInitialValue() instanceof IntegerLiteral){
                IntegerLiteral val = (IntegerLiteral) stv.getInitialValue();
                backend.emitLI(rgs[i],val.value,"val");
                backend.emitSW(rgs[i],FP,"-12","local var");
                backend.emitLW(rgs[i],FP,"-12","load");
            }
        }
        for (Stmt stmt : funcInfo.getStatements()) {
            stmt.dispatch(stmtAnalyzer);
        }

//       addi sp, sp, -@f.size                    # Reserve space for stack frame.
//       sw ra, @f.size-4(sp)                     # return address
//       sw fp, @f.size-8(sp)                     # control link
//       addi fp, sp, @f.size                     # New fp is at old SP.
//        li a0, 1                                 # Load integer literal 1
//        sw a0, -12(fp)                           # local variable x
//        lw a0, -12(fp)                           # Load var: f.x
//        j label_2                                # Go to return
//                mv a0, zero                              # Load None
//        j label_2                                # Jump to function epilogue
//        label_2:                                   # Epilogue
//                .equiv @f.size, 16
//        lw ra, -4(fp)                            # Get return address
//        lw fp, -8(fp)                            # Use control link to restore caller's fp
//        addi sp, sp, @f.size                     # Restore stack pointer
//        jr ra                                    # Return to caller



//        // FIXME: {... reset fp etc. ...}
//        backend.emitJR(RA, "Return to caller");
    }

    /** An analyzer that encapsulates code generation for statments. */
    private class StmtAnalyzer extends AbstractNodeAnalyzer<Void> {
        /*
         * The symbol table has all the info you need to determine
         * what a given identifier 'x' in the current scope is. You can
         * use it as follows:
         *   SymbolInfo x = sym.get("x");
         *
         * A SymbolInfo can be one the following:
         * - ClassInfo: a descriptor for classes
         * - FuncInfo: a descriptor for functions/methods
         * - AttrInfo: a descriptor for attributes
         * - GlobalVarInfo: a descriptor for global variables
         * - StackVarInfo: a descriptor for variables allocated on the stack,
         *      such as locals and parameters
         *
         * Since the input program is assumed to be semantically
         * valid and well-typed at this stage, you can always assume that
         * the symbol table contains valid information. For example, in
         * an expression `foo()` you KNOW that sym.get("foo") will either be
         * a FuncInfo or ClassInfo, but not any of the other infos
         * and never null.
         *
         * The symbol table in funcInfo has already been populated in
         * the base class: CodeGenBase. You do not need to add anything to
         * the symbol table. Simply query it with an identifier name to
         * get a descriptor for a function, class, variable, etc.
         *
         * The symbol table also maps nonlocal and global vars, so you
         * only need to lookup one symbol table and it will fetch the
         * appropriate info for the var that is currently in scope.
         */

        /** Symbol table for my statements. */
        private SymbolTable<SymbolInfo> sym;

        /** Label of code that exits from procedure. */
        protected Label epilogue;

        /** The descriptor for the current function, or null at the top
         *  level. */
        private FuncInfo funcInfo;

        /** An analyzer for the function described by FUNCINFO0, which is null
         *  for the top level. */
        StmtAnalyzer(FuncInfo funcInfo0) {
            funcInfo = funcInfo0;
            if (funcInfo == null) {
                sym = globalSymbols;
            } else {
                sym = funcInfo.getSymbolTable();
            }
            epilogue = generateLocalLabel();
        }

        // FIXME: Example of statement.
        @Override
        public Void analyze(ReturnStmt stmt) {
            // FIXME: Here, we emit an instruction that does nothing. Clearly,
            // this is wrong, and you'll have to fix it.
            // This is here just to demonstrate how to emit a
            // RISC-V instruction.
//            backend.emitMV(ZERO, ZERO, "No-op");
            //        j label_2                                # Jump to function epilogue
//        label_2:                                   # Epilogue
//                .equiv @f.size, 16
//        lw ra, -4(fp)                            # Get return address
//        lw fp, -8(fp)                            # Use control link to restore caller's fp
//        addi sp, sp, @f.size                     # Restore stack pointer
//        jr ra                                    # Return to caller
            Label l= new Label("label_2");
            backend.emitJ(l,"");
            backend.emitLocalLabel(l,"return");
            backend.emitInsn(".equiv @f.size, 16","");
            backend.emitLW(RA,FP,"-4","");
            backend.emitLW(FP,FP,"-8","");
            backend.emitADDI(SP,SP,"@f.size","");
            backend.emitJR(RA,"return to caller");

            return null;
        }

        @Override
        public Void analyze(ExprStmt stmt) {
            String kind = stmt.expr.kind;
            switch(kind){
                case "CallExpr":
                    CallExpr callexpr = (CallExpr) stmt.expr;
                        analyze(callexpr);
                    break;
                default:
                    break;
            }
            return null;
        }

        @Override
        public Void analyze(CallExpr stmt) {
            String name = stmt.function.name;
            switch (name){
                case "print":
                    ArrayList args = (ArrayList) stmt.args;
                    for(int i=0; i<args.size(); i++){
                        if(args.get(i) instanceof BooleanLiteral){
                            Label l= new Label("print_9");
                            BooleanLiteral arg = (BooleanLiteral) args.get(i);
                            if (arg.value==true){
                                Label t= new Label("const_3");
                                backend.emitLI(T0,1,"print true");
                                backend.emitSW(T0,A0,getAttrOffset(boolClass,"__bool__"),"store bool value");
                            }else{
                                backend.emitLI(T0,0,"print false");
                                backend.emitSW(T0,A0,getAttrOffset(boolClass,"__bool__"),"store bool value");
                            }
                            backend.emitJAL(l,"print bool");
                        }else if(args.get(i) instanceof IntegerLiteral){
                            Label l= new Label("print_7");
                            IntegerLiteral arg = (IntegerLiteral) args.get(i);

                            backend.emitLI(T0,arg.value,"print int");
                            backend.emitSW(T0,A0,getAttrOffset(intClass,"__int__"),"store int value");
                            backend.emitJAL(l,"print int");

                        }else if(args.get(i) instanceof StringLiteral){
                            Label l= new Label("print_8");
                            Label m= new Label("const_2");
                            StringLiteral arg = (StringLiteral) args.get(i);
                            emitConstant(arg,STR_TYPE,"msg");
                            backend.emitLA(A0,m,"message");
                            backend.emitJAL(l,"print str");
//
                        }else if(args.get(i) instanceof Identifier){
                            Label l= new Label(String.format("$%s",((Identifier) args.get(i)).name));
                            Label pt= new Label("$print");
                            backend.emitLW(A0,l,"load global");
                            Identifier arg = (Identifier) args.get(i);

                            if(arg.getInferredType().className().equals("int")){
                                Label mkint= new Label("makeint");
                                backend.emitJAL(mkint,"makeint");
                            }
                            backend.emitSW(A0,FP,-8,"push arg");
                            backend.emitADDI(SP,FP,-8,"set SP");
                            backend.emitJAL(pt,"print");


                        }else if(args.get(i) instanceof CallExpr){
                            CallExpr callexpr = (CallExpr) args.get(i);
                            analyze(callexpr);
                            Label pt= new Label("$print");
                            backend.emitJAL(pt,"print");
                        }
                    }

                    break;
                default:
//                    addi sp, fp, -@..main.size               # Set SP to stack frame top.
//  jal makeint                              # Box integer
//  sw a0, -16(fp)                           # Push argument 0 from last.
//  addi sp, fp, -16                         # Set SP to last argument.
                    backend.emitADDI(SP,FP,"-16","");
                    Label l = new Label("$"+stmt.function.name);
                    backend.emitJAL(l,"");
//                    backend.emitADDI(SP,FP,"-@..main.size", "");
                    Label m= new Label("makeint");
                    backend.emitJAL(m,"");
                    backend.emitSW(A0,FP,"-16","");
                    backend.emitADDI(SP,FP,"-16","");

            }

            return null;
        }
    }


    /**
     * Emits custom code in the CODE segment.
     *
     * This method is called after emitting the top level and the
     * function bodies for each function.
     *
     * You can use this method to emit anything you want outside of the
     * top level or functions, e.g. custom routines that you may want to
     * call from within your code to do common tasks. This is not strictly
     * needed. You might not modify this at all and still complete
     * the assignment.
     *
     * To start you off, here is an implementation of three routines that
     * will be commonly needed from within the code you will generate
     * for statements.
     *
     * The routines are error handlers for operations on None, index out
     * of bounds, and division by zero. They never return to their caller.
     * Just jump to one of these routines to throw an error and
     * exit the program. For example, to throw an OOB error:
     *   backend.emitJ(errorOob, "Go to out-of-bounds error and abort");
     *
     */
    protected void emitCustomCode() {
        emitErrorFunc(errorNone, "Operation on None");
        emitErrorFunc(errorDiv, "Division by zero");
        emitErrorFunc(errorOob, "Index out of bounds");

        backend.emitGlobalLabel(new Label("makeint"));
        backend.emitInsn("addi sp, sp, -8","");
        backend.emitInsn("sw ra, 4(sp)","");
        backend.emitInsn("sw a0, 0(sp)","");
        backend.emitInsn("la a0, $int$prototype","");
        backend.emitInsn("jal ra, alloc","");
        backend.emitInsn("lw t0, 0(sp)","");
        backend.emitInsn("sw t0, @.__int__(a0)","");
        backend.emitInsn("lw ra, 4(sp)","");
        backend.emitInsn("addi sp, sp, 8","");
        backend.emitInsn("jr ra","");

//        emitStdFunc("$f");

    }

    /** Emit an error routine labeled ERRLABEL that aborts with message MSG. */
    private void emitErrorFunc(Label errLabel, String msg) {
        backend.emitGlobalLabel(errLabel);
        backend.emitLI(A0, ERROR_NONE, "Exit code for: " + msg);
        backend.emitLA(A1, constants.getStrConstant(msg),
                       "Load error message as str");
        backend.emitADDI(A1, A1, getAttrOffset(strClass, "__str__"),
                         "Load address of attribute __str__");
        backend.emitJ(abortLabel, "Abort");
    }
}
