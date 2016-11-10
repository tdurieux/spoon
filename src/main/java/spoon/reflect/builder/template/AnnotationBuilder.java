package spoon.reflect.builder.template;

import spoon.reflect.declaration.CtElement;

public @interface AnnotationBuilder {
	String name();

	Class<? extends CtElement> type();
}
