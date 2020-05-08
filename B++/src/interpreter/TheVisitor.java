package interpreter;

import antlr4.BBaseVisitor;
import antlr4.BLexer;
import antlr4.BParser;
import antlr4.BParser.*;
import model.BData;
import model.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import driver.B;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TheVisitor extends BBaseVisitor<BData> {
    private static BdData returnValue = new BdData();
    private Scope scope;
    private Map<String, Function> functions;

    public TheVisitor(Scope scope, Map<String, Function> functions) {
        this.scope = scope;
        this.functions = functions;
    }

    // functionDecl
    @Override
    public BData visitFunctionDecl(FunctionDeclContext ctx) {
        return BData.VOID;
    }

    // list: '[' exprList? ']'
    @Override
    public BData visitList(ListContext ctx) {
        List<BData> list = new ArrayList<>();
        if (ctx.exprList() != null) {
            for (ExpressionContext ex : ctx.exprList().expression()) {
                list.add(this.visit(ex));
            }
        }
        return new BData(list);
    }


    // '-' expression                           #unaryMinusExpression
    @Override
    public BData visitUnaryMinusExpression(UnaryMinusExpressionContext ctx) {
        BData v = this.visit(ctx.expression());
        if (!v.isNumber()) {
            throw new BException(ctx);
        }
        return new BData(-1 * v.asDouble());
    }

    // '!' expression                           #notExpression
    @Override
    public BData visitNotExpression(NotExpressionContext ctx) {
        BData v = this.visit(ctx.expression());
        if (!v.isBoolean()) {
            throw new BException(ctx);
        }
        return new BData(!v.asBoolean());
    }

    // expression '^' expression                #powerExpression
    @Override
    public BData visitPowerExpression(PowerExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));
        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(Math.pow(lhs.asDouble(), rhs.asDouble()));
        }
        throw new BException(ctx);
    }

    // expression op=( '*' | '/' | '%' ) expression         #multExpression
    @Override
    public BData visitMultExpression(MultExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case BLexer.MUL:
                return MUL(ctx);
            case BLexer.DIV:
                return DIV(ctx);
            case BLexer.MOD:
                return MOD(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    // expression op=( '+' | '-' ) expression               #ADDExpression
    @Override
    public BData visitADDExpression(ADDExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case BLexer.ADD:
                return ADD(ctx);
            case BLexer.SUB:
                return SUB(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    // expression op=( '>=' | '<=' | '>' | '<' ) expression #compExpression
    @Override
    public BData visitCompExpression(CompExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case BLexer.LT:
                return lt(ctx);
            case BLexer.LTEQUALS:
                return ltEq(ctx);
            case BLexer.GT:
                return gt(ctx);
            case BLexer.GTEQUALS:
                return gtEq(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    // expression op=( '==' | '!=' ) expression             #eqExpression
    @Override
    public BData visitEqExpression(EqExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case BLexer.EQUALS:
                return eq(ctx);
            case BLexer.NEQUALS:
                return nEq(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    public BData MUL(MultExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        // number * number
        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() * rhs.asDouble());
        }

        // string * number
        if (lhs.isString() && rhs.isNumber()) {
            StringBuilder str = new StringBuilder();
            int stop = rhs.asDouble().intValue();
            for (int i = 0; i < stop; i++) {
                str.append(lhs.asString());
            }
            return new BData(str.toString());
        }

        // list * number
        if (lhs.isList() && rhs.isNumber()) {
            List<BData> total = new ArrayList<>();
            int stop = rhs.asDouble().intValue();
            for (int i = 0; i < stop; i++) {
                total.addAll(lhs.asList());
            }
            return new BData(total);
        }

        throw new BException(ctx);
    }

    private BData DIV(MultExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() / rhs.asDouble());
        }
        throw new BException(ctx);
    }

    private BData MOD(MultExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() % rhs.asDouble());
        }
        throw new BException(ctx);
    }

    private BData ADD(ADDExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        // number + number
        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() + rhs.asDouble());
        }

        // list + any
        if (lhs.isList()) {
            List<BData> list = lhs.asList();
            list.add(rhs);
            return new BData(list);
        }

        // string + any
        if (lhs.isString()) {
            return new BData(lhs.asString() + "" + rhs.toString());
        }

        // any + string
        if (rhs.isString()) {
            return new BData(lhs.toString() + "" + rhs.asString());
        }

        return new BData(lhs.toString() + rhs.toString());
    }

    private BData SUB(ADDExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() - rhs.asDouble());
        }
        if (lhs.isList()) {
            List<BData> list = lhs.asList();
            list.remove(rhs);
            return new BData(list);
        }
        throw new BException(ctx);
    }

    private BData gtEq(CompExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() >= rhs.asDouble());
        }
        if (lhs.isString() && rhs.isString()) {
            return new BData(lhs.asString().compareTo(rhs.asString()) >= 0);
        }
        throw new BException(ctx);
    }

    private BData ltEq(CompExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() <= rhs.asDouble());
        }
        if (lhs.isString() && rhs.isString()) {
            return new BData(lhs.asString().compareTo(rhs.asString()) <= 0);
        }
        throw new BException(ctx);
    }

    private BData gt(CompExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() > rhs.asDouble());
        }
        if (lhs.isString() && rhs.isString()) {
            return new BData(lhs.asString().compareTo(rhs.asString()) > 0);
        }
        throw new BException(ctx);
    }

    private BData lt(CompExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (lhs == null || rhs == null) {

            B.appendOutput("Null Pointer Exception: Trying to compare with null");

            throw new BException(ctx);
        }

        if (lhs.isNumber() && rhs.isNumber()) {
            return new BData(lhs.asDouble() < rhs.asDouble());
        }
        if (lhs.isString() && rhs.isString()) {
            return new BData(lhs.asString().compareTo(rhs.asString()) < 0);
        }
        throw new BException(ctx);
    }

    private BData eq(EqExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        return new BData(lhs.equals(rhs));
    }

    private BData nEq(EqExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        return new BData(!lhs.equals(rhs));
    }

    // expression '&&' expression               #andExpression
    @Override
    public BData visitAndExpression(AndExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (!lhs.isBoolean() || !rhs.isBoolean()) {
            throw new BException(ctx);
        }
        return new BData(lhs.asBoolean() && rhs.asBoolean());
    }

    // expression '||' expression               #orExpression
    @Override
    public BData visitOrExpression(OrExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (!lhs.isBoolean() || !rhs.isBoolean()) {
            throw new BException(ctx);
        }
        return new BData(lhs.asBoolean() || rhs.asBoolean());
    }

    // expression '?' expression ':' expression #ternaryExpression
    @Override
    public BData visitTernaryExpression(TernaryExpressionContext ctx) {
        BData condition = this.visit(ctx.expression(0));
        if (condition.asBoolean()) {
            return new BData(this.visit(ctx.expression(1)));
        } else {
            return new BData(this.visit(ctx.expression(2)));
        }
    }

    // expression In expression                 #inExpression
    @Override
    public BData visitInExpression(InExpressionContext ctx) {
        BData lhs = this.visit(ctx.expression(0));
        BData rhs = this.visit(ctx.expression(1));

        if (rhs.isList()) {
            for (BData val : rhs.asList()) {
                if (val.equals(lhs)) {
                    return new BData(true);
                }
            }
            return new BData(false);
        }
        throw new BException(ctx);
    }

    // Number                                   #numberExpression
    @Override
    public BData visitNumberExpression(NumberExpressionContext ctx) {
        return new BData(Double.valueOf(ctx.getText()));
    }

    // Bool                                     #boolExpression
    @Override
    public BData visitBoolExpression(BoolExpressionContext ctx) {
        return new BData(Boolean.valueOf(ctx.getText()));
    }

    // Null                                     #nullExpression
    @Override
    public BData visitNullExpression(NullExpressionContext ctx) {
        return BData.NULL;
    }

    private BData resolveIndexes(BData val, List<ExpressionContext> indexes) {
        for (ExpressionContext ec : indexes) {
            BData idx = this.visit(ec);
            if (!idx.isNumber() || (!val.isList() && !val.isString())) {
                throw new BException("Problem resolving indexes on " + val + " at " + idx, ec);
            }
            int i = idx.asDouble().intValue();
            if (val.isString()) {
                val = new BData(val.asString().substring(i, i + 1));
            } else {
                val = val.asList().get(i);
            }
        }
        return val;
    }

    private void setAtIndex(ParserRuleContext ctx, List<ExpressionContext> indexes, BData val, BData newVal) {
        if (!val.isList()) {
            throw new BException(ctx);
        }
        for (int i = 0; i < indexes.size() - 1; i++) {
            BData idx = this.visit(indexes.get(i));
            if (!idx.isNumber()) {
                throw new BException(ctx);
            }
            val = val.asList().get(idx.asDouble().intValue());
        }
        BData idx = this.visit(indexes.get(indexes.size() - 1));
        if (!idx.isNumber()) {
            throw new BException(ctx);
        }
        val.asList().set(idx.asDouble().intValue(), newVal);
    }

    // functionCall indexes?                    #functionCallExpression
    @Override
    public BData visitFunctionCallExpression(FunctionCallExpressionContext ctx) {
        BData val = this.visit(ctx.functionCall());
        if (ctx.indexes() != null) {
            List<ExpressionContext> exps = ctx.indexes().expression();
            val = resolveIndexes(val, exps);
        }
        return val;
    }

    // list indexes?                            #listExpression
    @Override
    public BData visitListExpression(ListExpressionContext ctx) {
        BData val = this.visit(ctx.list());
        if (ctx.indexes() != null) {
            List<ExpressionContext> exps = ctx.indexes().expression();
            val = resolveIndexes(val, exps);
        }
        return val;
    }

    // Identifier indexes?                      #identifierExpression
    @Override
    public BData visitIdentifierExpression(IdentifierExpressionContext ctx) {
        String id = ctx.IDENTIFIER().getText();
        BData val = scope.resolve(id);

        if (ctx.indexes() != null) {
            List<ExpressionContext> exps = ctx.indexes().expression();
            val = resolveIndexes(val, exps);
        }
        return val;
    }

    // String indexes?                          #stringExpression
    @Override
    public BData visitStringExpression(StringExpressionContext ctx) {
        String text = ctx.getText();
        text = text.substring(1, text.length() - 1).replaceAll("\\\\(.)", "$1");
        BData val = new BData(text);
        if (ctx.indexes() != null) {
            List<ExpressionContext> exps = ctx.indexes().expression();
            val = resolveIndexes(val, exps);
        }
        return val;
    }

    // '(' expression ')' indexes?              #expressionExpression
    @Override
    public BData visitExpressionExpression(ExpressionExpressionContext ctx) {
        BData val = this.visit(ctx.expression());
        if (ctx.indexes() != null) {
            List<ExpressionContext> exps = ctx.indexes().expression();
            val = resolveIndexes(val, exps);
                B.appendOutput(val + " insideinsideinside vse");
        }

//        B.appendOutput(val + "inside vse");
        return val;
    }



    // assignment
    // : Identifier indexes? '=' expression
    // ;
    @Override
    public BData visitAssignment(AssignmentContext ctx) {
        if(ctx.getText().contains("bnput()")) {
            BData newVal = new BData(B.getInput());
            String id = ctx.IDENTIFIER().getText();
            scope.assign(id, newVal);
            return BData.VOID;
        } else {
            BData newVal = this.visit(ctx.expression());
            if (ctx.indexes() != null) {
                BData val = scope.resolve(ctx.IDENTIFIER().getText());
                List<ExpressionContext> exps = ctx.indexes().expression();
                setAtIndex(ctx, exps, val, newVal);
            } else {
                String id = ctx.IDENTIFIER().getText();
                scope.assign(id, newVal);
            }
            return BData.VOID;
        }
    }

    // Identifier '(' exprList? ')' #identifierFunctionCall
    @Override
    public BData visitIdentifierFunctionCall(IdentifierFunctionCallContext ctx) {
        List<ExpressionContext> params = ctx.exprList() != null ? ctx.exprList().expression() : new ArrayList<ExpressionContext>();
        String id = ctx.IDENTIFIER().getText() + params.size();
        Function function;
        if ((function = functions.get(id)) != null) {
            return function.invoke(params, functions, scope);
        }
        throw new BException(ctx);
    }

    // Println '(' expression? ')'  #printlnFunctionCall
    @Override
    public BData visitPrintlnFunctionCall(PrintlnFunctionCallContext ctx) {
            B.appendOutput(this.visit(ctx.expression()).toString());
        return BData.VOID;
    }

    // Print '(' expression ')'     #printFunctionCall
    @Override
    public BData visitPrintFunctionCall(PrintFunctionCallContext ctx) {
            B.appendOutput(this.visit(ctx.expression()).toString());
        return BData.VOID;
    }

    // ifStatement
    //  : ifStat elseIfStat* elseStat? End
    //  ;
    //
    // ifStat
    //  : If expression Do block
    //  ;
    //
    // elseIfStat
    //  : Else If expression Do block
    //  ;
    //
    // elseStat
    //  : Else Do block
    //  ;
    @Override
    public BData visitIfStatement(IfStatementContext ctx) {

        // if ...
        if (this.visit(ctx.ifStat().expression()).asBoolean()) {
            return this.visit(ctx.ifStat().block());
        }
        // else if ...
        for (int i = 0; i < ctx.elseIfStat().size(); i++) {
            if (this.visit(ctx.elseIfStat(i).expression()).asBoolean()) {
                return this.visit(ctx.elseIfStat(i).block());
            }
        }

        // else ...
        if (ctx.elseStat() != null) {
            return this.visit(ctx.elseStat().block());
        }

        return BData.VOID;
    }

    // block
    // : (statement | functionDecl)* (Return expression)?
    // ;
    @Override
    public BData visitBlock(BlockContext ctx) {

        scope = new Scope(scope); // create new local scope
        for (StatementContext sx : ctx.statement()) {
            this.visit(sx);
        }
        ExpressionContext ex;
        if ((ex = ctx.expression()) != null) {
            returnValue.value = this.visit(ex);
            scope = scope.parent();
            throw returnValue;
        }
        scope = scope.parent();
        return BData.VOID;
    }

    // forStatement
    // : For Identifier '=' expression To expression OBrace block CBrace
    // ;
    @Override
    public BData visitForStatement(ForStatementContext ctx) {
        int start = this.visit(ctx.expression(0)).asDouble().intValue();
        int stop = this.visit(ctx.expression(1)).asDouble().intValue();
        for (int i = start; i <= stop; i++) {
            scope.assign(ctx.IDENTIFIER().getText(), new BData(i));
            BData returnValue = this.visit(ctx.block());
            if (returnValue != BData.VOID) {
                return returnValue;
            }
        }
        return BData.VOID;
    }

    // whileStatement
    // : While expression OBrace block CBrace
    // ;
    @Override
    public BData visitWhileStatement(WhileStatementContext ctx) {
        while (this.visit(ctx.expression()).asBoolean()) {
            BData returnValue = this.visit(ctx.block());
            if (returnValue != BData.VOID) {
                return returnValue;
            }
        }
        return BData.VOID;
    }

    @Override
    public BData visitDoWhileStatement(DoWhileStatementContext ctx) {
        do {
            BData returnValue = this.visit(ctx.block());
            if (returnValue != BData.VOID) {
                return returnValue;
            }
        } while (this.visit(ctx.expression()).asBoolean());
        return BData.VOID;
    }

    @Override
    public BData visitTryCatchNullStatement(TryCatchNullStatementContext ctx) {
        try{
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        } catch(NullPointerException e) {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        } finally {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        }
        return BData.VOID;
    }

    @Override public BData visitTryCatchIndexOutOfBoundsStatement(TryCatchIndexOutOfBoundsStatementContext ctx) {
        try {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        } catch(IndexOutOfBoundsException e) {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        } finally {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        }

        return BData.VOID;
    }

    @Override public BData visitTryCatchDivideByZeroStatement(TryCatchDivideByZeroStatementContext ctx) {
        try{
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        } catch(ArithmeticException e) {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        } finally {
            scope = new Scope(scope);
            for(StatementContext sx : ctx.statement()) {
                this.visit(sx);
            }
        }
        return BData.VOID;
    }
}
