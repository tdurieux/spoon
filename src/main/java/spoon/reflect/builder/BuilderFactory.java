

package spoon.reflect.builder;

import spoon.reflect.factory.Factory;

public class BuilderFactory {
    private Factory factory;

    public BuilderFactory(Factory factory) {
        this.factory = factory;
    }

    public LiteralBuilder literal(int value) {
        return new LiteralBuilder(this.factory, value);
    }

    public PlusBuilder plus(ExpressionBuilder<?> left, ExpressionBuilder<?> right) {
        return new PlusBuilder(this.factory, left, right);
    }
}

