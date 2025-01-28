package org.arjun.interpreter;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;
import java.util.stream.Collectors;

public class _interpreter_ extends JavaParserBaseVisitor<String>{
    private int indentLevel = 0;
    private final Map<String,String> classFields = new HashMap<>();
    private final Set<String> localVariables = new HashSet<>();

    public _interpreter_(){}

    @Override
    public String visitCompilationUnit(JavaParser.CompilationUnitContext ctx) {
        StringBuilder result = new StringBuilder();
        if (ctx.packageDeclaration() != null) {
            result.append(visit(ctx.packageDeclaration())).append("\n\n");
        }
        for (JavaParser.ImportDeclarationContext importCtx : ctx.importDeclaration()) {
            result.append(visit(importCtx)).append("\n");
        }
        result.append("\n");
        for (JavaParser.TypeDeclarationContext typeCtx : ctx.typeDeclaration()) {
            result.append(visit(typeCtx)).append("\n");
        }
        return result.toString();
    }

    @Override
    public String visitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        return "# package " + ctx.qualifiedName().getText();
    }

    @Override
    public String visitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        String importPath = ctx.qualifiedName().getText();
        return "# import " + importPath;
    }

    @Override
    public String visitFormalParameterList(JavaParser.FormalParameterListContext ctx) {
        return ctx.formalParameter().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        String paramName = ctx.variableDeclaratorId().getText();
        String paramType = mapJavaTypeToPython(visit(ctx.typeType()));
        return paramName + ": " + paramType;
    }

    @Override
    public String visitVariableDeclarators(JavaParser.VariableDeclaratorsContext ctx) {
        List<String> declarators = ctx.variableDeclarator().stream()
                .map(this::visit)
                .collect(Collectors.toList());
        if (declarators.size() > 1) {
            return String.join(", ", declarators);
        }
        return declarators.get(0);
    }

    @Override
    public String visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
        String varName = ctx.variableDeclaratorId().getText();
        if (ctx.variableInitializer() != null) {
            String initializer = visit(ctx.variableInitializer());
            return varName.replaceAll("\\[.*\\]", "") + " = " + initializer;
        }
        if (varName.contains("[")) {
            // uninitialized arrays
            return varName.replaceAll("\\[.*\\]", "") + " = []";
        }
        return varName + " = None";
    }

    @Override
    public String visitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null) {
            return visitIfStatement(ctx);
        }else if (ctx.SWITCH() != null) {
            return visitSwitchStatement(ctx);
        } else if (ctx.FOR() != null) {
            return visitForStatement(ctx);
        } else if (ctx.WHILE() != null) {
            return visitWhileStatement(ctx);
        } else if (ctx.TRY() != null) {
            return visitTryStatement(ctx);
        } else if (ctx.DO() != null) {
            return visitDoWhileStatement(ctx);
        } else if (ctx.RETURN() != null) {
            return "return " + (ctx.expression() != null && !ctx.expression().isEmpty() ? visit(ctx.expression(0)) : "");
        } else if (ctx.BREAK() != null) {
            return "break";
        } else if (ctx.CONTINUE() != null) {
            return "continue";
        } else if (ctx.statementExpression != null) {
            return visit(ctx.statementExpression).replaceAll(";$", "") + "\n";
        } else if (ctx.THROW() != null) {
            return visitThrowStatement(ctx);
        } else if (ctx.SEMI() != null) {
            return "";
        }
        if (ctx.block() != null)
            return visit(ctx.block());
        return indent() + ctx.getText() + "\n";
    }

    @Override
    public String visitParExpression(JavaParser.ParExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public String visitPrimary(JavaParser.PrimaryContext ctx) {
        if (ctx.expression() != null) return "(" + visit(ctx.expression()) + ")";
        if (ctx.THIS() != null) return "self";
        if (ctx.SUPER() != null) return "super()";
        if (ctx.literal() != null) return visitLiteral(ctx.literal());
        if (ctx.identifier() != null) return ctx.identifier().getText();
        if (ctx.typeTypeOrVoid() != null && ctx.CLASS() != null) {
            return mapJavaTypeToPython(visit(ctx.typeTypeOrVoid())) + ".__class__";
        }
        return ctx.getText();
    }

    @Override
    public String visitLiteral(JavaParser.LiteralContext ctx) {
        if (ctx.NULL_LITERAL() != null) return "None";
        if (ctx.BOOL_LITERAL() != null) return ctx.BOOL_LITERAL().getText().toLowerCase();
        return ctx.getText();
    }

    @Override
    public String visitTypeList(JavaParser.TypeListContext ctx) {
        return ctx.typeType().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitTypeType(JavaParser.TypeTypeContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        StringBuilder result = new StringBuilder();
        String type = mapJavaTypeToPython(ctx.typeType().getText());

        List<JavaParser.VariableDeclaratorContext> declarators = ctx.variableDeclarators().variableDeclarator();
        for (JavaParser.VariableDeclaratorContext varDecl : declarators) {
            String name = varDecl.variableDeclaratorId().getText();
            classFields.put(name, type);
            result.append(indent()).append("self.").append(name).append(" = ");

            if (varDecl.variableInitializer() != null) {
                result.append(visit(varDecl.variableInitializer()));
            } else {
                result.append(getSuitableValue(type));
            }

            result.append("  # Type: ").append(type).append("\n");
        }

        return result.toString();
    }

    @Override
    public String visitLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        for (JavaParser.VariableDeclaratorContext varDecl : ctx.variableDeclarators().variableDeclarator()) {
            localVariables.add(varDecl.variableDeclaratorId().getText());
        }
        return visit(ctx.variableDeclarators());
    }

    @Override
    public String visitBlockStatement(JavaParser.BlockStatementContext ctx) {
        if (ctx.localVariableDeclaration() != null) {
            return visit(ctx.localVariableDeclaration()) + "\n";
        } else if (ctx.statement() != null) {
            return visit(ctx.statement());
        }
        return "";
    }

    @Override
    public String visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx == null) return "";

        if (ctx.ASSIGN() != null) {
            String left = visit(ctx.expression(0));
            String right = visit(ctx.expression(1));
            return left + " = " + right;
        }
        if (ctx.ADD_ASSIGN() != null) {
            String left = visit(ctx.expression(0));
            String right = visit(ctx.expression(1));
            return left + " += " + right;
        }

        if (ctx.expression().size() == 2) {
            String left = visit(ctx.expression(0));
            String right = visit(ctx.expression(1));
            String op = ctx.bop != null ? ctx.bop.getText() : "";
            switch (op) {
                case "=":
                    return left + " = " + right;
                case "+=":
                case "-=":
                case "*=":
                case "/=":
                case "%=":
                case "==":
                case "!=":
                case "<":
                case ">":
                case "<=":
                case ">=":
                case "+":
                case "-":
                case "*":
                case "/":
                case "%":
                    return left + " " + op + " " + right;
                case "||":
                    return left + " or " + right;
                case "&&":
                    return left + " and " + right;
                default:
                    return ctx.getText();
            }
        }

        // Handle object creation
        if (ctx.creator() != null) {
            return visit(ctx.creator());
        }

        // Handle method calls, including System.out.println
        if (ctx.bop != null && ctx.bop.getText().equals(".") && ctx.methodCall() != null) {
            String object = visit(ctx.expression(0));
            String methodCall = visit(ctx.methodCall());
            if ("System.out".equals(object) && methodCall.startsWith("println")) {
                return "print" + methodCall.substring(7); // Remove "println" and keep the arguments
            }
            return object + "." + methodCall;
        }

        // Handle ternary operator
        if (ctx.bop != null && ctx.bop.getText().equals("?")) {
            String condition = visit(ctx.expression(0));
            String trueExpression = visit(ctx.expression(1));
            String falseExpression = visit(ctx.expression(2));
            return "(" + trueExpression + " if " + condition + " else " + falseExpression + ")";
        }
        if (ctx.expression().size() == 1) {
            String expr = visit(ctx.expression(0));
            if (ctx.postfix != null) {
                if (ctx.postfix.getText().equals("++")) {
                    return visit(ctx.expression(0)) + " += 1";
                } else if (ctx.postfix.getText().equals("--")) {
                    return visit(ctx.expression(0)) + " -= 1";
                }
            } else if (ctx.prefix != null) {
                if (ctx.prefix.getText().equals("++")) {
                    return visit(ctx.expression(0)) + " += 1";
                } else if (ctx.prefix.getText().equals("--")) {
                    return visit(ctx.expression(0)) + " -= 1";
                }
            }
            if (ctx.bop != null && ctx.bop.getText().equals(".")) {
                String right = ctx.identifier().getText();
                if (right.equals("println")) {
                    return "print";
                }
                return expr + "." + right;
            }
            if (ctx.LBRACK() != null) {
                return expr + "[" + visit(ctx.expression(1)) + "]";
            }
        }
        if (ctx.NEW() != null) {
            return visitCreator(ctx.creator());
        }

        // Handle other expressions
        if (ctx.expression().size() == 2 && ctx.bop != null) {
            String left = visit(ctx.expression(0));
            String right = visit(ctx.expression(1));
            String operator = ctx.bop.getText();
            return left + " " + operator + " " + right;
        }

        else if (ctx.LPAREN() != null && ctx.typeType() != null) {
            // This is a cast, which we'll ignore in Python
            return visit(ctx.expression(0));
        }
        if (ctx.primary() != null && ctx.primary().identifier() != null) {
            String identifier = ctx.primary().identifier().getText();
            if (classFields.containsKey(identifier) && !localVariables.contains(identifier)) {
                return "self." + identifier;
            }
            return visit(ctx.primary());
        } else if (ctx.primary() != null) {
            return visitPrimary(ctx.primary());
        }
        return ctx.getText();
    }

    @Override
    public String visitMethodCall(JavaParser.MethodCallContext ctx) {
        if (ctx == null) return "";
        String methodName = ctx.identifier().getText();
        String args = ctx.arguments() != null ? visit(ctx.arguments()) : "";
        return methodName +"("+ args+")";
    }

    @Override
    public String visitExpressionList(JavaParser.ExpressionListContext ctx) {
        return ctx.expression().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitCreator(JavaParser.CreatorContext ctx) {
        String typeName = visit(ctx.createdName());
        if (ctx.classCreatorRest() != null) {
            String args = visitArguments(ctx.classCreatorRest().arguments());
            return typeName + "(" + args + ")";
        } else if (ctx.arrayCreatorRest() != null) {
            return visitArrayCreatorRest(ctx.arrayCreatorRest());
        }
        return typeName + "()";
    }

    public String visitArrayCreatorRest(JavaParser.ArrayCreatorRestContext ctx) {
        if (ctx.arrayInitializer() != null) {
            return visit(ctx.arrayInitializer());
        } else {
            List<String> dimensions = new ArrayList<>();
            for (JavaParser.ExpressionContext expr : ctx.expression()) {
                dimensions.add(visit(expr));
            }
            return "[None] * " + String.join(" * ", dimensions);
        }
    }

    @Override
    public String visitArrayInitializer(JavaParser.ArrayInitializerContext ctx) {
        if (ctx.variableInitializer() == null) {
            return "[]";
        }
        return "[" + ctx.variableInitializer().stream()
                .map(this::visit)
                .collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public String visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        classFields.clear();
        String className = ctx.identifier().getText();
        StringBuilder result = new StringBuilder().append("class ").append(className);

        List<String> inheritance = new ArrayList<>();
        if (ctx.EXTENDS() != null) {
            inheritance.add(ctx.typeType().getText());
        }
        if (ctx.IMPLEMENTS() != null) {
            inheritance.addAll(ctx.typeList().stream().map(ParseTree::getText).collect(Collectors.toList()));
        }

        if (!inheritance.isEmpty()) {
            result.append("(").append(String.join(", ", inheritance)).append(")");
        }
        result.append(":\n");
        List<JavaParser.ConstructorDeclarationContext> constructors = ctx.classBody().classBodyDeclaration().stream()
                .filter(bodyDecl -> bodyDecl.memberDeclaration() != null && bodyDecl.memberDeclaration().constructorDeclaration() != null)
                .map(bodyDecl -> bodyDecl.memberDeclaration().constructorDeclaration())
                .collect(Collectors.toList());
        boolean hasFields = false;
        StringBuilder classFields = new StringBuilder();
        // Handle field declarations
        for (JavaParser.ClassBodyDeclarationContext bodyDecl : ctx.classBody().classBodyDeclaration()) {
            if (bodyDecl.memberDeclaration() != null && bodyDecl.memberDeclaration().fieldDeclaration() != null) {
                classFields.append(visitFieldDeclaration(bodyDecl.memberDeclaration().fieldDeclaration()));
                hasFields = true;
            }
        }
        indentLevel++;
        if(constructors.isEmpty()){
            result.append(indent()).append("def __init__(self):\n");
            indentLevel++;
            if(!hasFields) result.append(indent()).append("pass\n");
            else result.append(indent()).append(classFields);
            indentLevel--;
        } else {
            result.append(convertConstructors(constructors));
        }

        boolean hasMainMethod = false;
        // Handle method declarations
        for (JavaParser.ClassBodyDeclarationContext bodyDecl : ctx.classBody().classBodyDeclaration()) {
            if (bodyDecl.memberDeclaration() != null && bodyDecl.memberDeclaration().methodDeclaration() != null) {
                String methodDeclaration = visitMethodDeclaration(bodyDecl.memberDeclaration().methodDeclaration());
                result.append("\n").append(methodDeclaration);
                if (methodDeclaration.contains("def main(")) {
                    hasMainMethod = true;
                }
            }
        }

        indentLevel--;

        if (hasMainMethod) {
            result.append("\n").append("if __name__ == \"__main__\":\n");
            indentLevel++;
            result.append(indent()).append(className).append(".main([])\n");
            indentLevel--;
        }
        return result.toString();
    }

    @Override
    public String visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        localVariables.clear();
        StringBuilder output = new StringBuilder(indent());
        if (name.equals("main")) {
            output.append("@staticmethod\n").append(indent());
        }
        output.append("def ").append(name).append("(");
        if (!name.equals("main")) {
            output.append("self");
            if (ctx.formalParameters().formalParameterList() != null) {
                output.append(", ");
            }
        }
        if (ctx.formalParameters().formalParameterList() != null) {
            output.append(visit(ctx.formalParameters().formalParameterList()));
        }
        output.append("):\n");
        indentLevel++;
        String body = visit(ctx.methodBody());
        if (body.trim().isEmpty()) {
            output.append(indent()).append("pass\n");
        } else {
            output.append(body);
        }
        indentLevel--;
        return output.toString();
    }

    @Override
    public String visitBlock(JavaParser.BlockContext ctx) {
        StringBuilder result = new StringBuilder();
        for (JavaParser.BlockStatementContext stmtCtx : ctx.blockStatement()) {
            String stmt = visit(stmtCtx);
            if (!stmt.isEmpty()) {
                result.append(indent()).append(stmt.trim()).append("\n");
            }
        }
        return result.toString();
    }

    @Override
    public String visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        String interfaceName = ctx.identifier().getText();
        StringBuilder result = new StringBuilder("class " + interfaceName + ":\n");
        indentLevel++;

        boolean hasMembers = false;
        for (JavaParser.InterfaceBodyDeclarationContext bodyDecl : ctx.interfaceBody().interfaceBodyDeclaration()) {
            if (bodyDecl.interfaceMemberDeclaration() != null && bodyDecl.interfaceMemberDeclaration().interfaceMethodDeclaration() != null) {
                result.append(visitInterfaceMethodDeclaration(bodyDecl.interfaceMemberDeclaration().interfaceMethodDeclaration()));
                hasMembers = true;
            }
        }

        if (!hasMembers) {
            result.append(indent()).append("pass\n");
        }

        indentLevel--;
        return result.toString();
    }

    @Override
    public String visitInterfaceMethodDeclaration(JavaParser.InterfaceMethodDeclarationContext ctx) {
        String methodName = ctx.interfaceMethodModifier().isEmpty() ?
                ctx.interfaceCommonBodyDeclaration().identifier().getText() :
                ctx.interfaceMethodModifier(0).getText();

        StringBuilder result = new StringBuilder(indent()).append("def ").append(methodName).append("(self");

        // Handle parameters
        if (ctx.interfaceCommonBodyDeclaration().formalParameters().formalParameterList() != null) {
            result.append(", ").append(visit(ctx.interfaceCommonBodyDeclaration().formalParameters().formalParameterList()));
        }

        result.append("):\n");
        indentLevel++;
        result.append(indent()).append("pass\n");
        indentLevel--;
        return result.toString();
    }

    @Override
    public String visitCreatedName(JavaParser.CreatedNameContext ctx) {
        return ctx.identifier(0).getText();
    }

    @Override
    public String visitArguments(JavaParser.ArgumentsContext ctx) {
        if (ctx.expressionList() != null) {
            return visit(ctx.expressionList());
        }
        return "";
    }

//    ================================================
//    STATIC METHODS
//    ================================================

    private String indent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private String mapJavaTypeToPython(String javaType) {
        if(javaType == null) return "None";
        switch (javaType) {
            case "int": case "long": case "short": case "byte": return "int";
            case "float": case "double": return "float";
            case "boolean": return "bool";
            case "char": case "String": return "str";
            case "String[]": return "[]";
            default: return javaType;
        }
    }

    private String getSuitableValue(String type) {
        if(type == null) return "None";
        switch (type){
            case "int":
            case "short":
            case "long": return "0";
            case "float":
            case "double":return "0.0";
            case "str":
            case "char" : return "''";
            default: return "None";
        }
    }

    private String mapJavaClassToPython(String javaClass) {
        switch (javaClass) {
            case "ArrayList": case "List": return "list";
            case "HashMap": case "Map": return "dict";
            case "HashSet": case "Set": return "set";
            default: return javaClass;
        }
    }

    private String visitStatementBody(JavaParser.StatementContext ctx) {
        return ctx.block() != null ? visitBlock(ctx.block()) : indent() + visit(ctx).trim() + "\n";
    }

    private String visitTryStatement(JavaParser.StatementContext ctx) {
        StringBuilder result = new StringBuilder();
        result.append(indent()).append("try:\n");
        indentLevel++;
        result.append(visit(ctx.block()));
        indentLevel--;

        for (JavaParser.CatchClauseContext catchClause : ctx.catchClause()) {
            result.append(indent()).append("except ");
            if (catchClause.catchType() != null && !catchClause.catchType().getText().equals("Exception")) {
                //TODO: unmapped exception types
                result.append(visit(catchClause.catchType()));
            } else {
                result.append("Exception");
            }
            result.append(" as ").append(catchClause.identifier().getText()).append(":\n");
            indentLevel++;
            String catchBlock = visit(catchClause.block());
            if (catchBlock.trim().isEmpty()) {
                result.append(indent()).append("pass\n");
            } else {
                result.append(catchBlock);
            }
            indentLevel--;
        }

        if (ctx.finallyBlock() != null) {
            result.append(indent()).append("finally:\n");
            indentLevel++;
            result.append(visit(ctx.finallyBlock().block()));
            indentLevel--;
        }

        return result.toString();
    }

    private String visitThrowStatement(JavaParser.StatementContext ctx) {
        return indent() + "raise " + visit(ctx.expression(0)) + "\n";
    }

    private String visitIfStatement(JavaParser.StatementContext ctx) {
        StringBuilder result = new StringBuilder();
        result.append(indent()).append("if ").append(visit(ctx.parExpression())).append(":\n");
        indentLevel++;
        result.append(visitStatementBody(ctx.statement(0)));
        indentLevel--;

        JavaParser.StatementContext elseStatement = ctx.statement(1);
        while (elseStatement != null && elseStatement.IF() != null) {
            result.append(indent()).append("elif ").append(visit(elseStatement.parExpression())).append(":\n");
            indentLevel++;
            result.append(visitStatementBody(elseStatement.statement(0)));
            indentLevel--;
            elseStatement = elseStatement.statement(1);
        }

        if (elseStatement != null) {
            result.append(indent()).append("else:\n");
            indentLevel++;
            result.append(visitStatementBody(elseStatement));
            indentLevel--;
        }

        return result.toString();
    }

    private String visitSwitchStatement(JavaParser.StatementContext ctx) {
        StringBuilder result = new StringBuilder();
        String switchExpression = visit(ctx.parExpression());
        result.append(indent()).append("match ").append(switchExpression).append(":\n");
        indentLevel++;

        JavaParser.SwitchBlockStatementGroupContext[] caseGroups = ctx.switchBlockStatementGroup().toArray(new JavaParser.SwitchBlockStatementGroupContext[0]);
        boolean hasDefault = false;

        for (JavaParser.SwitchBlockStatementGroupContext caseGroup : caseGroups) {
            for (JavaParser.SwitchLabelContext label : caseGroup.switchLabel()) {
                if (label.CASE() != null) {
                    result.append(indent()).append("case ").append(visit(label.constantExpression)).append(":\n");
                } else if (label.DEFAULT() != null) {
                    result.append(indent()).append("case _:\n");
                    hasDefault = true;
                }
            }
            indentLevel++;
            for (JavaParser.BlockStatementContext blockStatement : caseGroup.blockStatement()) {
                String stmt = visit(blockStatement);
                if (!stmt.trim().equals("break")) {  // Skip 'break' statements
                    result.append(indent()).append(stmt.trim()).append("\n");
                }
            }
            indentLevel--;
        }

        if (!hasDefault) {
            result.append(indent()).append("case _:\n");
            indentLevel++;
            result.append(indent()).append("pass\n");
            indentLevel--;
        }

        indentLevel--;
        return result.toString();
    }

    private String visitForStatement(JavaParser.StatementContext ctx) {
        JavaParser.ForControlContext forCtx = ctx.forControl();
        if (forCtx.enhancedForControl() != null) {
            String var = forCtx.enhancedForControl().variableDeclaratorId().getText();
            String iterable = visit(forCtx.enhancedForControl().expression());
            StringBuilder result = new StringBuilder();
            result.append(indent()).append("for ").append(var).append(" in ").append(iterable).append(":\n");
            indentLevel++;
            result.append(visit(ctx.statement(0)));
            indentLevel--;
            return result.toString();
        } else {
            // Basic for loop - convert to while loop
            String init = forCtx.forInit() != null ? visit(forCtx.forInit()) : "";
            String condition = forCtx.expression() != null ? visit(forCtx.expression()) : "True";
            String update = forCtx.forUpdate != null ? visit(forCtx.forUpdate) : "";

            StringBuilder result = new StringBuilder(init + "\n");
            result.append(indent()).append("while ").append(condition).append(":\n");
            indentLevel++;
            result.append(visit(ctx.statement(0)));
            result.append(indent()).append(update).append("\n");
            indentLevel--;
            return result.toString();
        }
    }

    private String visitWhileStatement(JavaParser.StatementContext ctx) {
        StringBuilder result = new StringBuilder();
        String condition = visit(ctx.parExpression());
        result.append(indent()).append("while ").append(condition).append(":\n");
        indentLevel++;

        JavaParser.StatementContext bodyCtx = ctx.statement(0);
        if (bodyCtx.block() != null) {
            String blockContent = visit(bodyCtx.block());
            if (blockContent.trim().isEmpty()) {
                result.append(indent()).append("pass\n");
            } else {
                result.append(blockContent);
            }
        } else {
            String stmt = visit(bodyCtx);
            if (stmt.trim().isEmpty()) {
                result.append(indent()).append("pass\n");
            } else {
                result.append(stmt);
            }
        }

        indentLevel--;
        return result.toString();
    }

    private String visitDoWhileStatement(JavaParser.StatementContext ctx) {
        StringBuilder result = new StringBuilder("while True:\n");
        indentLevel++;
        result.append(visit(ctx.statement(0)));
        result.append(indent()).append("if not (").append(visit(ctx.parExpression())).append("):\n");
        indentLevel++;
        result.append(indent()).append("break\n");
        indentLevel -= 2;
        return result.toString();
    }

    private String convertConstructors(List<JavaParser.ConstructorDeclarationContext> constructors) {
        StringBuilder result = new StringBuilder();
        result.append(indent()).append("def __init__(self");
        StringBuilder params = new StringBuilder();
        for(Map.Entry<String,String> field: classFields.entrySet()){
            params.append(field.getKey()).append(" = ").append(getSuitableValue(field.getValue()));
            params.append(", ");
        }
        if(params.length()>0) result.append(", ").append(params, 0, params.length()-2);
        result.append("):\n");
        indentLevel++;
        for (JavaParser.ConstructorDeclarationContext ctor : constructors) {
            Map<String,String> ctorParams = getConstructorParams(ctor);
            if (ctorParams.isEmpty()) {
                if(params.length() > 0) {
                    result.append(indent()).append("if all(param is None for param in [").append(params, 0, params.length() - 2).append("]):\n");
                    indentLevel++;
                    result.append(indent()).append("self.__init__(None, None)\n");
                    indentLevel--;
                }else result.append(visitBlock(ctor.block()));
            } else {
                if(params.length() == 0){
                    List<String> appendCondition = new ArrayList<>();
                    for(Map.Entry<String,String> param: ctorParams.entrySet()){
                        appendCondition.add("isInstance("+param.getKey()+","+param.getValue()+")");
                    }
                    String combinedResult = String.join(" and ", appendCondition);
                    result.append(indent()).append("if ").append(combinedResult).append(":\n");
                    indentLevel++;
                    result.append(visitBlock(ctor.block()));
                }
                else {
                    String condition = ctorParams.keySet().stream().map(param -> param + " is not None").collect(Collectors.joining(" and "));
                    result.append(indent()).append("elif ").append(condition).append(":\n");
                    indentLevel++;
                    for (String param : ctorParams.keySet()) {
                        result.append(indent()).append("self.").append(param).append(" = ").append(param).append("\n");
                    }
                }
                indentLevel--;
            }
        }
        indentLevel--;
        return result.toString();
    }

    private Map<String,String> getConstructorParams(JavaParser.ConstructorDeclarationContext ctor) {
        Map<String,String> params = new HashMap<>();
        if (ctor.formalParameters() != null && ctor.formalParameters().formalParameterList() != null) {
            for (JavaParser.FormalParameterContext param : ctor.formalParameters().formalParameterList().formalParameter()) {
                String paramName = param.variableDeclaratorId().getText();
                String type = mapJavaTypeToPython(param.typeType().getText());
                params.put(paramName,type);
            }
        }
        return params;
    }
}
