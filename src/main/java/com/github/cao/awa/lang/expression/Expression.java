package com.github.cao.awa.lang.expression;

import com.github.cao.awa.lang.expression.exception.*;
import com.github.zhuaidadaya.rikaishinikui.handler.conductor.string.builder.*;
import com.github.zhuaidadaya.rikaishinikui.handler.universal.action.*;
import com.github.zhuaidadaya.rikaishinikui.handler.universal.collection.sequence.*;
import com.github.zhuaidadaya.rikaishinikui.handler.universal.entrust.*;
import com.github.zhuaidadaya.rikaishinikui.handler.universal.entrust.function.*;
import com.github.zhuaidadaya.rikaishinikui.handler.universal.operational.*;
import it.unimi.dsi.fastutil.objects.*;

import java.math.*;
import java.util.*;
import java.util.function.*;

public class Expression {
    public static boolean optimize = true;
    private final Bilateral<Character> bilateral = EntrustParser.operation(() -> {
        Map<Character, Character> map = new Object2ObjectLinkedOpenHashMap<>();
        map.put('(', ')');
        return new Bilateral<>(map, - 1);
    });
    private final boolean isBracket;
    private boolean merged;
    private final Expression parent;
    private final List<Expression> expressions = new LinkedList<>();
    private final OperationalInteger linesTracker = new OperationalInteger();
    private final OperationalInteger charsTracker = new OperationalInteger();
    private String info;
    private boolean byPriority;

    public Expression(Expression parent, char target) {
        this.parent = parent;
        this.isBracket = false;
        this.merged = false;
        this.info = String.valueOf(target);
    }

    public Expression(Expression parent, String target, boolean bracket) throws Exception {
        this.parent = parent;
        if (bracket) {
            target = new StringBuilderConductor(target).delete(0, 1).deleteLast().toString();
        }
        if (optimize) {
            if (! parent.byPriority || target.length() == 1) {
                bracket = false;
            }
        }
        this.isBracket = bracket;
        this.merged = false;
        parse(target);
    }

    private void parse(String target) throws Exception {
        StringBuilderConductor bracket = new StringBuilderConductor();
        StringBuilderConductor info = new StringBuilderConductor();
        boolean isBracket = false;
        boolean waiting = false;
        boolean doneBracket = false;
        for (char c : target.toCharArray()) {
            charsTracker.add();
            if (c == '\n' || c == '\r') {
                linesTracker.add();
                continue;
            }
            if (c == ' ') {
                continue;
            }
            if (c == '(') {
                if (!waiting) {
                    isBracket = true;
                }
                bilateral.success(c);
            }
            if (isBracket) {
                bracket.append(c);
            } else {
                info.append(c);
                ExpressionOperation symbol = ExpressionOperation.forSymbol(c);
                if (symbol != null) {
                    doneBracket = false;
                    if (info.length() == 1 || merged || symbol == ExpressionOperation.PLUS || symbol == ExpressionOperation.SUBTRACT) {
                        if (waiting) {
                            expressions.add(new Expression(this, info.deleteLast().toString(), false, true));
                            expressions.add(new Expression(this, c));
                            waiting = false;
                            info.clear();
                            continue;
                        }
                        String expInfo = info.toString().substring(0, info.length() - 1);
                        if (this.info == null) {
                            this.info = info.deleteLast().toString();
                            expressions.add(new Expression(this, c));
                        } else {
                            expressions.add(new Expression(this, expInfo));
                            expressions.add(new Expression(this, c));
                        }
                    } else if (symbol == ExpressionOperation.MULTIPLY || symbol == ExpressionOperation.DIVIDE) {
                        waiting = true;
                        continue;
                    }
                    info.clear();
                } else {
                    if (doneBracket) {
                        throw new BracketException("Trailing symbols without operators are disallowed in bracket end: lines " + linesTracker.get() + ", chars " + charsTracker.get());
                    }
                }
            }
            if (c == ')') {
                if (doneBracket) {
                    throw new BracketException("Trailing symbols without operators are disallowed in bracket end: lines " + linesTracker.get() + ", chars " + charsTracker.get());
                }
                if (bilateral.failure(c)) {
                    throw new Exception();
                }
                if (bilateral.isDone()) {
                    isBracket = false;
                    if (bracket.length() != 2) {
                        if (expressions.size() > 0) {
                            if (ExpressionOperation.forSymbol(expressions.get(expressions.size() - 1).info) == null) {
                                expressions.add(new Expression("*"));
                            }
                        }
                        expressions.add(new Expression(this, bracket.toString(), true));
                    }
                    doneBracket = true;
                    bracket = new StringBuilderConductor();
                    continue;
                }
            }
        }
        if (target.length() > 1 && linesTracker.get() > 0) {
            char lastChar = target.charAt(target.length() - 1);
            if (lastChar == '\n' || lastChar == '\r') {
                linesTracker.reduce();
                charsTracker.set(target.length() - 1);
            }
        }
        if (info.length() > 0) {
            if (waiting) {
                expressions.add(new Expression(this, info.toString(), false, true));
                return;
            }
            expressions.add(new Expression(info.toString()));
        }
        if (!(expressions.size() > 0)) {
            return;
        }
        if (ExpressionOperation.forSymbol(expressions.get(expressions.size() - 1).info) != null) {
            StringBuilderConductor builder = new StringBuilderConductor();
            for (int i = Math.min(5, target.length() - 1); i > 1;i--) {
                builder.append(target.charAt(charsTracker.get() - i));
            }
            throw new BadEndException("Expression incorrectly end: lines " + linesTracker.get() + ", chars " + charsTracker.get() + ". at " + builder + target.charAt(charsTracker.get() - 1) + " <--, for '" + target.charAt(charsTracker.get() - 1) + "'");
        }
    }

    public Expression(Expression parent, String target, boolean bracket, boolean merged) throws Exception {
        this.parent = parent;
        if (bracket) {
            target = new StringBuilderConductor(target).delete(0, 1).deleteLast().toString();
            if (target.length() == 0) {
                target = "0";
                if (optimize) {
                    bracket = false;
                }
            }
        }
        if (optimize) {
            if (! parent.byPriority || target.length() == 1) {
                bracket = false;
            }
        }
        this.merged = merged;
        this.isBracket = bracket;
        parse(target);
    }

    public Expression(Expression parent, String target) throws Exception {
        this.parent = parent;
        this.isBracket = false;
        this.merged = false;
        parse(target);
    }

    private Expression(String target) {
        this.parent = null;
        this.isBracket = false;
        this.merged = false;
        this.info = target;
    }

    public static Expression parseExp(String target) throws Exception {
        Expression expression = new Expression(null);
        expression.parse(target);
        return expression;
    }

    public void optimize() {
        collect();
        int retry = 1;
        while (retry > 0) {
            retry--;
            for (int i = - 1; i < expressions.size(); i++) {
                Expression expression = i == - 1 ? this : expressions.get(i);
                Expression last = i > 0 ? expressions.get(i - 1) : i == 0 ? this : null;
                Expression lastTwo = i > 1 ? expressions.get(i - 2) : i == 1 ? this : null;
                if (expression.info == null) {
                    continue;
                }
                if (! byPriority) {
                    byPriority = expression.info.equals("*") || expression.info.equals("/");
                }
                if (i > -1) {
                    expression.optimize();
                }
                try {
                    BigDecimal thisNumber = BigDecimal.valueOf(Double.parseDouble(expression.info));
                    expression.info = thisNumber.toString();
                } catch (Exception e) {

                }
                if (last != null) {
                    ExpressionOperation lastSymbol = ExpressionOperation.forSymbol(last.info);
                    ExpressionOperation symbol = ExpressionOperation.forSymbol(expression.info);
                    if ((lastSymbol == ExpressionOperation.SUBTRACT && symbol == ExpressionOperation.PLUS) || (lastSymbol == ExpressionOperation.PLUS && symbol == ExpressionOperation.SUBTRACT)) {
                        int index = expressions.indexOf(expression);
                        expressions.add(index, new Expression("-"));
                        expressions.remove(expression);
                        expressions.remove(last);
                    }
                    if (lastSymbol == ExpressionOperation.MULTIPLY || lastSymbol == ExpressionOperation.DIVIDE) {
                        if (expression.info.equals("1") || expression.info.equals("0")) {
                            expressions.remove(expression);
                            expressions.remove(last);
                        }
                    }
                }
                try {
                    if (! expression.merged) {
                        ExpressionOperation lastSymbol = ExpressionOperation.forSymbol(last.info);
                        BigDecimal lastTwoInteger = BigDecimal.valueOf(Double.parseDouble(lastTwo.info));
                        BigDecimal lastInteger = lastSymbol == null ? BigDecimal.valueOf(Double.parseDouble(last.info)) : BigDecimal.valueOf(- 1);
                        BigDecimal thisInteger = BigDecimal.valueOf(Double.parseDouble(expression.info));
                        if (lastInteger.compareTo(BigDecimal.ZERO) == 0) {
                            expressions.remove(last);
                        }
                        if (lastSymbol == null) {
                            continue;
                        }
                        switch (lastSymbol) {
                            case PLUS -> lastTwoInteger = lastTwoInteger.add(thisInteger);
                            case DIVIDE -> lastTwoInteger = lastTwoInteger.divide(thisInteger, RoundingMode.FLOOR);
                            case SUBTRACT -> lastTwoInteger = lastTwoInteger.subtract(thisInteger);
                            case MULTIPLY -> lastTwoInteger = lastTwoInteger.multiply(thisInteger);
                        }
                        lastTwo.info = String.valueOf(lastTwoInteger);
                        lastTwo.merged = false;
                        expressions.remove(expression);
                        expressions.remove(last);
                        retry++;
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    public void buildTree(StringBuilderConductor builder, String base) {
        if (isBracket) {
            //                        builder.append("(");
            builder.append("(").append("\n").append(base);
        }
        if (merged) {
            builder.append("<");
        }
        if (info != null) {
            builder.append(info);
        }
        for (Expression expression : expressions) {
            expression.buildTree(builder, base + "  ");
        }
        if (isBracket) {
            //                        builder.append(")");
            builder.append("\n").append(base).append(")");
        }
        if (merged) {
            builder.append(">");
        }
    }

    public void print() {
        for (Expression expression : getExpressions()) {
            StringBuilderConductor conductor = new StringBuilderConductor();
            if (this.isBracket) {
                conductor.append("(").append(expression.getInfo()).append(")");
            } else {
                conductor.append(expression.getInfo());
            }
            System.out.println(conductor);
            expression.print();
        }
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public String getInfo() {
        return info;
    }

    public Expression collect() {
        List<Expression> move = new LinkedList<>();
        for (Expression expression : expressions) {
            if (expression.info == null) {
                move.add(expression);
            }
            expression.collect();
        }

        for (Expression expression : move) {
            int offset = 0;
            for (Expression moving : expression.expressions) {
                expressions.add(expressions.indexOf(expression) + offset++, moving);
            }
            expressions.remove(expression);
        }

        return this;
    }

    public String toString() {
        return expressions.size() > 0 ? "[" + info + ", " + expressions.toString().substring(1) : info;
    }
}
