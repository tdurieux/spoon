/**
 * Copyright (C) 2006-2016 INRIA and contributors
 * Spoon - http://spoon.gforge.inria.fr/
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package spoon.reflect.visitor.printer;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Find local variables catch, parameters, fields, super fields
 */
public class AccessibleMethodsFinder {

	private CtElement expression;

	public AccessibleMethodsFinder(CtElement expression) {
		this.expression = expression;
	}

	public List<CtMethod> find() {
		if (expression.isParentInitialized()) {
			return getMethods(expression.getParent(CtType.class));
		}
		return Collections.emptyList();
	}

	private List<CtMethod> getMethods(final CtType type) {
		final List<CtMethod> methods = new ArrayList<>();
		if (type == null) {
			return methods;
		}
		methods.addAll(type.getAllMethods());
		methods.addAll(getMethods(type.getParent(CtType.class)));
		return methods;
	}
}
