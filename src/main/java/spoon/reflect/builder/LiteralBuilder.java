

package spoon.reflect.builder;

import spoon.reflect.builder.template.AbstractBuilder;
import spoon.reflect.builder.template.AnnotationBuilder;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.factory.Factory;

@AnnotationBuilder(name = "literal", type = CtLiteral.class)
public class LiteralBuilder extends AbstractBuilder<CtLiteral> implements ExpressionBuilder<CtLiteral> {
    public LiteralBuilder(Factory factory, int value) {
        super(factory.Code().createLiteral(value));
    }

    @Override
    public PlusBuilder plus(ExpressionBuilder<?> right) {
        return getFactory().Builder().plus(this, right);
    }
}

