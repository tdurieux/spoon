

package spoon.reflect.builder;

import spoon.reflect.builder.template.AbstractBuilder;
import spoon.reflect.builder.template.AnnotationBuilder;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.factory.Factory;

@AnnotationBuilder(name = "plus", type = CtBinaryOperator.class)
public class PlusBuilder extends AbstractBuilder<CtBinaryOperator> implements ExpressionBuilder<CtBinaryOperator> {
    public PlusBuilder(Factory factory, ExpressionBuilder<?> left, ExpressionBuilder<?> right) {
        super(factory.Code().createBinaryOperator(left.build(), right.build(), BinaryOperatorKind.PLUS));
    }

    @Override
    public PlusBuilder plus(ExpressionBuilder<?> right) {
        return getFactory().Builder().plus(this, right);
    }
}

