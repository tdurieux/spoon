

package spoon.reflect.builder.template;

import spoon.reflect.declaration.CtElement;

public interface Builder<T extends CtElement> {
    T build();
}

