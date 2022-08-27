package com.github.cao.awa.lang.expression;

public enum ExpressionOperation {
    PLUS, SUBTRACT, MULTIPLY, DIVIDE;
    
    public static ExpressionOperation forSymbol(String name) {
        if (name == null) {
            return null;
        }
        return switch (name) {
            case "+" -> PLUS;
            case "-" -> SUBTRACT;
            case "*" -> MULTIPLY;
            case "/" -> DIVIDE;
            default -> null;
        };
    }


    public static ExpressionOperation forSymbol(char name) {
        return switch (name) {
            case '+' -> PLUS;
            case '-' -> SUBTRACT;
            case '*' -> MULTIPLY;
            case '/' -> DIVIDE;
            default -> null;
        };
    }
}
