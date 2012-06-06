package com.amalto.core.query.user;

public class Range extends Condition {

    private final Expression expression;

    private final Expression start;

    private final Expression end;

    public Range(Expression expression, Expression start, Expression end) {
        this.expression = expression;
        this.start = start;
        this.end = end;
    }

    public Range(Expression expression, int start, int end) {
        this(expression, new IntegerConstant(start), new IntegerConstant(end));
    }

    public Expression normalize() {
        return this;
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public Expression getStart() {
        return start;
    }

    public Expression getEnd() {
        return end;
    }
}
