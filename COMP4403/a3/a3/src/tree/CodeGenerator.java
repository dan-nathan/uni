package tree;

import java.util.*;

import machine.Operation;
import machine.StackMachine;
import source.Errors;
import source.VisitorDebugger;
import syms.SymEntry;
import syms.Type;
import tree.StatementNode.*;

/**
 * class CodeGenerator implements code generation using the
 * visitor pattern to traverse the abstract syntax tree.
 */
public class CodeGenerator implements DeclVisitor, StatementTransform<Code>,
        ExpTransform<Code> {
    /**
     * Current static level of nesting into procedures.
     */
    private int staticLevel;

    /**
     * Table of code for each procedure
     */
    private final Procedures procedures;

    /**
     * Error message handler
     */
    private final Errors errors;

    /**
     * Debug messages are reported through the visitor debugger.
     */
    private final VisitorDebugger debug;


    public CodeGenerator(Errors errors) {
        super();
        this.errors = errors;
        debug = new VisitorDebugger("generating", errors);
        procedures = new Procedures();
    }

    /**
     * Main generate code method for the program.
     */
    public Procedures generateCode(DeclNode.ProcedureNode node) {
        beginGen("Program");
        staticLevel = node.getBlock().getBlockLocals().getLevel();
        assert staticLevel == 1;  // Main program is at static level 1
        /* Generate the code for the main program and all procedures */
        visitProcedureNode(node);
        endGen("Program");
        return procedures;
    }

    /* -------------------- Visitor methods ----------------------------*/

    /**
     * Generate code for a single procedure.
     */
    public void visitProcedureNode(DeclNode.ProcedureNode node) {
        beginGen("Procedure");
        // Generate code for the block
        Code code = visitBlockNode(node.getBlock());
            code.generateOp(Operation.RETURN);
        procedures.addProcedure(node.getProcEntry(), code);
        //System.out.println(node.getProcEntry().getIdent() + "\n" + code);
        endGen("Procedure");
    }

    /**
     * Generate code for a block.
     */
    public Code visitBlockNode(BlockNode node) {
        beginGen("Block");
        /* Generate code to allocate space for local variables on
         * procedure entry.
         */
        Code code = new Code();
        code.genAllocStack(node.getBlockLocals().getVariableSpace());
        /* Generate the code for the body */
        code.append(node.getBody().genCode(this));
        /* Generate code for local procedures.
         * Static level is one greater for the procedures.
         */
        staticLevel++;
        node.getProcedures().accept(this);
        staticLevel--;
        endGen("Block");
        return code;
    }

    /**
     * Code generation for a list of procedures
     */
    public void visitDeclListNode(DeclNode.DeclListNode node) {
        beginGen("DeclList");
        for (DeclNode decl : node.getDeclarations()) {
            decl.accept(this);
        }
        endGen("DeclList");
    }


    //**************  Statement node code generation visit methods

    /**
     * Code generation for an erroneous statement should not be attempted.
     */
    public Code visitStatementErrorNode(StatementNode.ErrorNode node) {
        errors.fatal("PL0 Internal error: generateCode for Statement Error Node",
                node.getLocation());
        return null;
    }


    /**
     * Code generation for an assignment statement.
     */
    public Code visitAssignmentNode(StatementNode.AssignmentNode node) {
        beginGen("Assignment");
        Code code = new Code();
        code.genComment("assignment:");
        /* Generate code to evaluate the expression */
        code.append(node.getExp().genCode(this));
        /* Generate the code to load the address of the variable */
        code.append(node.getVariable().genCode(this));
        /* Generate the store based on the type/size of value */
        code.genStore(node.getExp().getType());
        endGen("Assignment");
        return code;
    }

    /**
     * Generate code for a "read" statement.
     */
    public Code visitReadNode(StatementNode.ReadNode node) {
        beginGen("Read");
        Code code = new Code();
        code.genComment("read:");
        /* Read an integer from standard input */
        code.generateOp(Operation.READ);
        /* Generate the code to load the address of the LValue */
        code.append(node.getLValue().genCode(this));
        /* Generate the store based on the type/size of value */
        code.genStore(node.getLValue().getType().optDereferenceType());
        endGen("Read");
        return code;
    }

    /**
     * Generate code for a "write" statement.
     */
    public Code visitWriteNode(StatementNode.WriteNode node) {
        beginGen("Write");
        Code code = new Code();
        code.genComment("write:");
        code.append(node.getExp().genCode(this));
        code.generateOp(Operation.WRITE);
        endGen("Write");
        return code;
    }

    /**
     * Generate code for a "call" statement.
     */
    public Code visitCallNode(StatementNode.CallNode node) {
        beginGen("Call");
        SymEntry.ProcedureEntry proc = node.getEntry();
        Code code = new Code();
        code.genComment("call:");
        // Put actual parameters on top of the stack
        for (int i = node.getActualParams().size() - 1; i >= 0; i--) {
            code.append(node.getActualParams().get(i).genCode(this));
            // Ensure reference parameters give global addresses
            if (proc.getType().getFormalParams().get(i).isRef()) {
                code.generateOp(Operation.TO_GLOBAL);
            }
        }
        /* Generate the call instruction. The second parameter is the
         * procedure's symbol table entry. The actual address is resolved
         * at load time.
         */
        code.genCall(staticLevel - proc.getLevel(), proc);
        int totalParamSize = 0;
        for (int i = 0; i < node.getActualParams().size(); i++) {
            totalParamSize += node.getActualParams().get(i).getType().getSpace();
        }
        code.genDeallocStack(totalParamSize);
        endGen("Call");
        return code;
    }

    /**
     * Generate code for a statement list
     */
    public Code visitStatementListNode(StatementNode.ListNode node) {
        beginGen("StatementList");
        Code code = new Code();
        for (StatementNode s : node.getStatements()) {
            code.append(s.genCode(this));
        }
        endGen("StatementList");
        return code;
    }

    /**
     * Generate code for an "if" statement.
     */
    public Code visitIfNode(StatementNode.IfNode node) {
        beginGen("If");
        Code code = new Code();
        code.genComment("if:");
        /* Generate the code for the if-then-else
         * from the code for its components */
        code.genIfThenElse(node.getCondition().genCode(this),
                node.getThenStmt().genCode(this),
                node.getElseStmt().genCode(this));
        endGen("If");
        return code;
    }

    /**
     * Generate code for a "while" statement.
     */
    public Code visitWhileNode(StatementNode.WhileNode node) {
        beginGen("While");
        Code code = new Code();
        code.genComment("while:");
        /* Generate the code to evaluate the condition. */
        code.append(node.getCondition().genCode(this));
        /* Generate the code for the loop body */
        Code bodyCode = node.getLoopStmt().genCode(this);
        /* Add a branch over the loop body on false.
         * The offset is the size of the loop body code plus
         * the size of the branch to follow the body.
         */
        code.genJumpIfFalse(bodyCode.size() + Code.SIZE_JUMP_ALWAYS);
        /* Append the code for the body */
        code.append(bodyCode);
        /* Add a branch back to the condition.
         * The offset is the total size of the current code plus the
         * size of a Jump Always (being generated).
         */
        code.genJumpAlways(-(code.size() + Code.SIZE_JUMP_ALWAYS));
        endGen("While");
        return code;
    }
    //************* Expression node code generation visit methods

    /**
     * Code generation for an erroneous expression should not be attempted.
     */
    public Code visitErrorExpNode(ExpNode.ErrorNode node) {
        errors.fatal("PL0 Internal error: generateCode for ErrorExpNode",
                node.getLocation());
        return null;
    }

    /**
     * Generate code for a constant expression.
     */
    public Code visitConstNode(ExpNode.ConstNode node) {
        beginGen("Const");
        Code code = new Code();
        if (node.getValue() == 0) {
            code.generateOp(Operation.ZERO);
        } else if (node.getValue() == 1) {
            code.generateOp(Operation.ONE);
        } else {
            code.genLoadConstant(node.getValue());
        }
        endGen("Const");
        return code;
    }

    /**
     * Generate operator operands in order
     */
    private Code genArgs(ExpNode left, ExpNode right) {
        beginGen("ArgsInOrder");
        Code code = left.genCode(this);
        code.append(right.genCode(this));
        endGen("ArgsInOrder");
        return code;
    }

    /* Helper method to implement SHIFT_LEFT and SHIFT_RIGHT ignoring sign
    *  Assumes the value to be shifted is on top of the stack
    *  shiftAmount is code generates the amount to shift by */
    private Code logicalShift(boolean left, Code shiftAmount) {
        Code code = new Code();
        code.append(shiftAmount);
        if (left) {
            // lsl is the same as asl
            code.generateOp(Operation.SHIFT_LEFT);
        } else {
            code.generateOp(Operation.SHIFT_RIGHT);
            // Check if the result is -ve (meaning original number was -ve)
            code.generateOp(Operation.DUP);
            code.generateOp(Operation.ZERO);
            code.generateOp(Operation.LESS);

            Code negativeCode = new Code();
            // Generate a bitmask for the first 32 - shiftAmount digits
            // (ie the number 2^(32 - shiftAmount) - 1)
            negativeCode.generateOp(Operation.ONE);
            negativeCode.genLoadConstant(32);
            negativeCode.append(shiftAmount);
            negativeCode.generateOp(Operation.NEGATE);
            negativeCode.generateOp(Operation.ADD);
            negativeCode.generateOp(Operation.SHIFT_LEFT);
            negativeCode.generateOp(Operation.ONE);
            negativeCode.generateOp(Operation.NEGATE);
            negativeCode.generateOp(Operation.ADD);
            // Apply bitmask
            negativeCode.generateOp(Operation.AND);

            // Add branch and append code together
            code.genJumpIfFalse(negativeCode.size());
            code.append(negativeCode);
        }
        return code;
    }

    /**
     * Generate code for a binary operator expression.
     */
    public Code visitBinaryNode(ExpNode.BinaryNode node) {
        beginGen("Binary");
        Code code;
        ExpNode left = node.getLeft();
        ExpNode right = node.getRight();
        switch (node.getOp()) {
            case ADD_OP:
                code = genArgs(left, right);
                if (node.getLeft().getType() instanceof Type.SetType) {
                    code.generateOp(Operation.OR);
                } else {
                    code.generateOp(Operation.ADD);
                }
                break;
            case SUB_OP:
                code = genArgs(left, right);
                if (node.getLeft().getType() instanceof Type.SetType) {
                    code.generateOp(Operation.NOT);
                    code.generateOp(Operation.AND);
                } else {
                    code.generateOp(Operation.NEGATE);
                    code.generateOp(Operation.ADD);
                }
                break;
            case MUL_OP:
                code = genArgs(left, right);
                if (node.getLeft().getType() instanceof Type.SetType) {
                    code.generateOp(Operation.AND);
                } else {
                    code.generateOp(Operation.MPY);
                }
                break;
            case DIV_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.DIV);
                break;
            case EQUALS_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.EQUAL);
                break;
            case LESS_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.LESS);
                break;
            case NEQUALS_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.EQUAL);
                code.genBoolNot();
                break;
            case LEQUALS_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.LESSEQ);
                break;
            case GREATER_OP:
                /* Generate argument values in reverse order and use LESS */
                code = genArgs(right, left);
                code.generateOp(Operation.LESS);
                break;
            case GEQUALS_OP:
                /* Generate argument values in reverse order and use LESSEQ */
                code = genArgs(right, left);
                code.generateOp(Operation.LESSEQ);
                break;
            case IN_OP:
                code = new Code();
                // Failsafe - with IN_OP not being overloaded this should always evaluate to true
                if (right.getType() instanceof Type.SetType
                        && ((Type.SetType) right.getType()).getElementType()
                        instanceof Type.SubrangeType) {
                    Type.SubrangeType type = (Type.SubrangeType)((Type.SetType) right.getType())
                            .getElementType();

                    // Check if the value is in the subrange without halting if not
                    code.genLoadConstant(type.getLower());
                    code.append(left.genCode(this));
                    code.generateOp(Operation.LESSEQ);

                    Code checkUpperBound = new Code();
                    checkUpperBound.append(left.genCode(this));
                    checkUpperBound.genLoadConstant(type.getUpper());
                    checkUpperBound.generateOp(Operation.LESSEQ);

                    // Code to handle checking if the bit is set
                    Code checkBitSet = new Code();
                    // Put a 1 on the stack
                    checkBitSet.generateOp(Operation.ONE);
                    // Evaluate the left hand expression

                    // Figure out how much to shift by
                    Code shiftAmount = left.genCode(this);
                    shiftAmount.genLoadConstant(type.getLower());
                    shiftAmount.generateOp(Operation.NEGATE);
                    shiftAmount.generateOp(Operation.ADD);
                    // Left shift
                    checkBitSet.append(logicalShift(true, shiftAmount));

                    // AND operation will give 0 if and only if the element is not in the set
                    checkBitSet.append(right.genCode(this));
                    checkBitSet.generateOp(Operation.AND);

                    // Shift the bit back to the ones place
                    checkBitSet.append(logicalShift(false, shiftAmount));

                    // Skip adding 0 to the stack for this branch
                    checkBitSet.genJumpAlways(Operation.ZERO.getSize());

                    // Add branching then append code together
                    checkUpperBound.genJumpIfFalse(checkBitSet.size());
                    code.genJumpIfFalse(checkUpperBound.size() + checkBitSet.size());
                    code.append(checkUpperBound);
                    code.append(checkBitSet);
                    // Return 0 if the value was not in range
                    code.generateOp(Operation.ZERO);
                } else {
                    errors.fatal("PL0 Internal error: Invalid types for operator",
                            node.getLocation());
                    code = null;
                }
                break;
            default:
                errors.fatal("PL0 Internal error: Unknown operator",
                        node.getLocation());
                code = null;
        }
        endGen("Binary");
        return code;
    }
    /**
     * Generate code for a unary operator expression.
     */
    public Code visitUnaryNode(ExpNode.UnaryNode node) {
        beginGen("Unary");
        Code code = node.getArg().genCode(this);
        switch (node.getOp()) {
            case NEG_OP:
                code.generateOp(Operation.NEGATE);
                break;
            case COMPLEMENT_OP:
                code.generateOp(Operation.NOT);
                break;
            default:
                errors.fatal("PL0 Internal error: Unknown operator",
                        node.getLocation());
        }
        endGen("Unary");
        return code;
    }

    /**
     * Generate code to dereference an RValue.
     */
    public Code visitDereferenceNode(ExpNode.DereferenceNode node) {
        beginGen("Dereference");
        Code code = node.getLeftValue().genCode(this);
        code.genLoad(node.getType());
        endGen("Dereference");
        return code;
    }

    /**
     * Generating code for an IdentifierNode is invalid because the
     * static checker should have converted all IdentifierNodes to
     * either ConstNodes or VariableNodes.
     */
    public Code visitIdentifierNode(ExpNode.IdentifierNode node) {
        errors.fatal("Internal error: code generator called on IdentifierNode",
                node.getLocation());
        return null;
    }

    /**
     * Generate code for a variable reference.
     * It pushes the address of the variable as an offset from the frame pointer
     */
    public Code visitVariableNode(ExpNode.VariableNode node) {
        beginGen("Variable");
        SymEntry.VarEntry var = node.getVariable();
        Code code = new Code();
        code.genMemRef(staticLevel - var.getLevel(), var.getOffset());
        if (var instanceof SymEntry.ParamEntry && ((SymEntry.ParamEntry) var).isRef()) {
            code.generateOp(Operation.LOAD_FRAME);
            code.generateOp(Operation.TO_LOCAL);
        }
        endGen("Variable");
        return code;
    }


    /**
     * Generate code to perform a bounds check on a subrange.
     */
    public Code visitNarrowSubrangeNode(ExpNode.NarrowSubrangeNode node) {
        beginGen("NarrowSubrange");
        Code code = node.getExp().genCode(this);
        code.genBoundsCheck(node.getSubrangeType().getLower(),
                node.getSubrangeType().getUpper());
        endGen("NarrowSubrange");
        return code;
    }

    /**
     * Generate code to widen a subrange to an integer.
     */
    public Code visitWidenSubrangeNode(ExpNode.WidenSubrangeNode node) {
        beginGen("WidenSubrange");
        /* Widening doesn't require anything extra other than
         * generating code for its expression.
         */
        Code code = node.getExp().genCode(this);
        endGen("WidenSubrange");
        return code;
    }

    /**
     * Generate code for a set constructor
     */
    public Code visitSetNode(ExpNode.SetNode node) {
        beginGen("Set");
        Code code = new Code();
        // Failsafe - this should always evaluate to true
        if (node.getType() instanceof Type.SetType
                && ((Type.SetType)node.getType()).getElementType() instanceof Type.SubrangeType) {
            Type.SubrangeType type
                    = (Type.SubrangeType)((Type.SetType)node.getType()).getElementType();
            code.generateOp(Operation.ZERO);
            for (ExpNode elem : node.getElements()) {
                code.generateOp(Operation.ONE);
                code.append(elem.genCode(this));

                // Find the value relative to the start of the subrange
                code.genLoadConstant(type.getLower());
                code.generateOp(Operation.NEGATE);
                code.generateOp(Operation.ADD);
                // Shift the 1 left to the correct bit and add to the total value of the set
                code.generateOp(Operation.SHIFT_LEFT);
                code.generateOp(Operation.ADD);
            }
        } else {
            errors.fatal("PL0 Internal error: Invalid Type for Set Node",
                    node.getLocation());
            code = null;
        }
        endGen("Set");
        return code;
    }
    //**************************** Support Methods

    /**
     * Push current node onto debug rule stack and increase debug level
     */
    private void beginGen(String nodeName) {
        debug.beginDebug(nodeName);
    }

    /**
     * Pop current node from debug rule stack and decrease debug level
     */
    private void endGen(String node) {
        debug.endDebug(node);
    }

}