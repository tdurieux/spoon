package spoon.reflect.builder.template;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public abstract class AbstractBuilder<T extends CtElement> implements Builder<T> {

	private final Factory factory;
	private T element;

	public AbstractBuilder(T element) {
		this.factory = element.getFactory();
		this.element = element;
	}

	public Factory getFactory() {
		return factory;
	}

	@Override
	public T build() {
		return element;
	}

	@Override
	public String toString() {
		if (element == null) {
			return "<empty>";
		}
		return element.toString();
	}
}
