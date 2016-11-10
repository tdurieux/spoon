

package spoon.reflect.builder;

import spoon.reflect.builder.template.Builder;
import spoon.reflect.code.CtExpression;

interface ExpressionBuilder<T extends CtExpression> extends Builder<T> {
    PlusBuilder plus(ExpressionBuilder<?> right);
}

