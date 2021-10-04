package tree;

import machine.Operation;
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
        /* Generate code to evaluate the expression */
        Code code = node.getExp().genCode(this);
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
        Code code = node.getExp().genCode(this);
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
        /* Generate the call instruction. The second parameter is the
         * procedure's symbol table entry. The actual address is resolved
         * at load time.
         */
        code.genCall(staticLevel - proc.getLevel(), proc);
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
        /* Generate the code to evaluate the condition. */
        Code code = node.getCondition().genCode(this);
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

    /**
     * Generate code for a "for" statement.
     */
    public Code visitForNode(StatementNode.ForNode node) {
        beginGen("For");

        /* BRANCH STRUCTURE
         *
         * (1) Setup Loop
         * (2) Bounds Check
         * (3) Loop Body & Overflow Check
         * (4) Increment Loop Control
         * (5) Cleanup
         *
         * The jumps are:
         * end of (2) -> start of (5) : jump over (3) and (4)
         * end of (3) -> start of (5) : jump over (4)
         * end of (4) -> start of (2) : jump backwards over (4), (3) and (2)
         *
         * (1) - (4) are each represented by separate Code objects. They are created separately
         * then branch instructions are appended to the end of them after all have been completed
         * (which allows their size to be calculated)
         */

        // Assign loop variable to lower bound
        Code setupLoop = node.getLower().genCode(this);
        setupLoop.append(node.getCtrlVar().genCode(this));
        setupLoop.genStore(node.getLower().getType().optDereferenceType());
        // Calculate upper bound and store on the stack frame
        setupLoop.append(node.getUpper().genCode(this));

        Code boundsCheck = new Code();
        // Create a copy of the upper bound for comparison
        boundsCheck.generateOp(Operation.DUP);
        boundsCheck.append(node.getCtrlVar().genCode(this));
        boundsCheck.genLoad(node.getCtrlVar().getType());
        // If the upper bound is >= the loop counter
        boundsCheck.generateOp(Operation.SWAP);
        boundsCheck.generateOp(Operation.LESSEQ);

        // Generate loop contents for code
        Code bodyCode = node.getLoopStmt().genCode(this);
        // Load loop control variable to check if it is about to overflow
        bodyCode.append(node.getCtrlVar().genCode(this));
        bodyCode.genLoad(node.getCtrlVar().getType());
        bodyCode.generateOp(Operation.DUP);
        // Max integer value
        bodyCode.genLoadConstant(0x7FFFFFFF);
        bodyCode.generateOp(Operation.EQUAL);

        Code incrementCode = new Code();
        // Increment the loop control variable
        incrementCode.generateOp(Operation.ONE);
        incrementCode.generateOp(Operation.ADD);
        incrementCode.append(node.getCtrlVar().genCode(this));
        incrementCode.genStore(node.getCtrlVar().getType());

        // Code to exit loop if control variable overflows
        /* genJumpAlways consists of LOAD_CON and BR, which are 2 and 1 words respectively (total 3)
         * Combine with POP to get jump offset of 4. If false continue on to incrementCode
         */
        bodyCode.genJumpIfFalse(4);
        // Get rid of the other loop control variable from the stack
        bodyCode.generateOp(Operation.POP);
        // Jump over incrementCode plus the branch back to boundsCheck
        bodyCode.genJumpAlways(incrementCode.size() + Code.SIZE_JUMP_ALWAYS);

        /* Add a branch over the loop body on false.
         * The offset is the size of the loop body code plus
         * the size of the branch to follow the body.
         */
        boundsCheck.genJumpIfFalse(bodyCode.size() + incrementCode.size()
                + Code.SIZE_JUMP_ALWAYS);

        /* Add a branch back to the condition.
         * The offset is the total size of the current code plus the
         * size of a Jump Always (being generated).
         */
        incrementCode.genJumpAlways(-(boundsCheck.size() + bodyCode.size()
                + incrementCode.size() + Code.SIZE_JUMP_ALWAYS));

        // Append code sections together
        setupLoop.append(boundsCheck);
        setupLoop.append(bodyCode);
        setupLoop.append(incrementCode);
        // Remove the upper bound that was stored on the stack machine
        setupLoop.generateOp(Operation.POP);
        endGen("For");
        return setupLoop;
    }
    //************* Expression node code generation visit methods

    /**
     * Code generation for an erroneous expression should not be attempted.
     */
    public Code visitErrorExpNode(ExpNode.ErrorNode node) {
        errors.fatal("PL0 Internal error: generateCode for ErrorExpNode",
                node.getLocation());
        return new Code();
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
                code.generateOp(Operation.ADD);
                break;
            case SUB_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.NEGATE);
                code.generateOp(Operation.ADD);
                break;
            case MUL_OP:
                code = genArgs(left, right);
                code.generateOp(Operation.MPY);
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
            case SUCC_OP:
                code.generateOp(Operation.ONE);
                code.generateOp(Operation.ADD);
                // Check the type before casting to EnumerationType as a failsafe
                if (node.getArg().getType() instanceof Type.EnumerationType) {
                    int elems = ((Type.EnumerationType)node.getArg().getType()).numberOfElements();
                    // Create a copy for bounds checking
                    code.generateOp(Operation.DUP);
                    code.genLoadConstant(elems);
                    code.generateOp(Operation.EQUAL);
                    // If it needs to loop around to the 1st element
                    code.genJumpIfFalse(2);
                    // True - replace element with 0
                    code.generateOp(Operation.POP);
                    code.generateOp(Operation.ZERO);
                    // False - do nothing
                }
                break;
            case PRED_OP:
                code.generateOp(Operation.ONE);
                code.generateOp(Operation.NEGATE);
                code.generateOp(Operation.ADD);
                if (node.getArg().getType() instanceof Type.EnumerationType) {
                    int elems = ((Type.EnumerationType)node.getArg().getType()).numberOfElements();
                    // Create a copy for bounds checking
                    code.generateOp(Operation.DUP);
                    code.generateOp(Operation.ZERO);
                    code.generateOp(Operation.LESS);
                    // If it needs to loop around to the 1st element
                    code.genJumpIfFalse(3);
                    // True - replace element with 0
                    code.generateOp(Operation.POP);
                    code.genLoadConstant(elems - 1);
                    // False - do nothing
                }
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
        return new Code();
    }

    /**
     * Generate code for an array index
     */
    public Code visitArrayIndexNode(ExpNode.ArrayIndexNode node) {
        beginGen("ArrayIndex");
        Code code = node.getIndex().genCode(this);
        if (node.getIndex().getType() instanceof Type.SubrangeType) {
            Type.SubrangeType indexSubrange = ((Type.SubrangeType)node.getIndex().getType());
            /* If the index is an uninitialised subrange variable, its node will not be a
             * NarrowSubrangeNode, meaning bounds checking code will not have been generated when
             * it was visited. Manually generate the code here.
             */
            if (!(node.getIndex() instanceof ExpNode.NarrowSubrangeNode)) {
                code.genBoundsCheck(indexSubrange.getLower(), indexSubrange.getUpper());
            }

            // Subtract lower bound from index so that lower bound corresponds with index 0
            code.genLoadConstant(indexSubrange.getLower());
            code.generateOp(Operation.NEGATE);
            code.generateOp(Operation.ADD);
            // Load size of array entry (sizeof array / num elements)
            code.genLoadConstant(node.getId().getType().optDereferenceType().getSpace()
                    / (indexSubrange.getUpper() + 1 - indexSubrange.getLower()));
        } else if (node.getIndex().getType() instanceof Type.EnumerationType) {
            Type.EnumerationType enumType = ((Type.EnumerationType)node.getIndex().getType());
            // Load size of array entry (sizeof array / num elements)
            code.genLoadConstant(node.getId().getType().optDereferenceType().getSpace()
                    / enumType.numberOfElements());
        } else {
            // Error report - this code should never run if static checking is done correctly
            errors.fatal("Internal error: Array index isn't subrange or enum",
                    node.getLocation());
        }
        // Multiply by size of element to get address offset from start of array
        code.generateOp(Operation.MPY);
        // Append code to get address of identifier variable
        code.append(node.getId().genCode(this));
        code.generateOp(Operation.ADD);
        endGen("ArrayIndex");
        return code;
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
