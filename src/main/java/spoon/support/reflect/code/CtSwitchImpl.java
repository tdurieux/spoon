/**
 * Copyright (C) 2006-2017 INRIA and contributors
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
package spoon.support.reflect.code;

import spoon.diff.AddAction;
import spoon.diff.DeleteAction;
import spoon.diff.DeleteAllAction;
import spoon.diff.UpdateAction;
import spoon.diff.context.ListContext;
import spoon.diff.context.ObjectContext;
import spoon.reflect.annotations.MetamodelPropertyField;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.path.CtRole;
import spoon.reflect.visitor.CtVisitor;
import spoon.support.reflect.declaration.CtElementImpl;

import java.util.ArrayList;
import java.util.List;

import static spoon.reflect.ModelElementContainerDefaultCapacities.SWITCH_CASES_CONTAINER_DEFAULT_CAPACITY;

public class CtSwitchImpl<S> extends CtStatementImpl implements CtSwitch<S> {
	private static final long serialVersionUID = 1L;

	@MetamodelPropertyField(role = CtRole.CASE)
	List<CtCase<? super S>> cases = emptyList();

	@MetamodelPropertyField(role = CtRole.EXPRESSION)
	CtExpression<S> expression;

	@Override
	public void accept(CtVisitor visitor) {
		visitor.visitCtSwitch(this);
	}

	@Override
	public List<CtCase<? super S>> getCases() {
		return cases;
	}

	@Override
	public CtExpression<S> getSelector() {
		return expression;
	}

	@Override
	public <T extends CtSwitch<S>> T setCases(List<CtCase<? super S>> cases) {
		if (cases == null || cases.isEmpty()) {
			this.cases = CtElementImpl.emptyList();
			return (T) this;
		}
		if (getFactory().getEnvironment().buildStackChanges()) {
			getFactory().getEnvironment().pushToStack(new DeleteAllAction(new ListContext(
					this, this.cases), new ArrayList<>(this.cases)));
		}
		this.cases.clear();
		for (CtCase<? super S> aCase : cases) {
			addCase(aCase);
		}
		return (T) this;
	}

	@Override
	public <T extends CtSwitch<S>> T setSelector(CtExpression<S> selector) {
		if (selector != null) {
			selector.setParent(this);
		}
		if (getFactory().getEnvironment().buildStackChanges()) {
			getFactory().getEnvironment().pushToStack(new UpdateAction(new ObjectContext(this, "expression"), selector, this.expression));
		}
		this.expression = selector;
		return (T) this;
	}

	@Override
	public <T extends CtSwitch<S>> T addCase(CtCase<? super S> c) {
		if (c == null) {
			return (T) this;
		}
		if (cases == CtElementImpl.<CtCase<? super S>>emptyList()) {
			cases = new ArrayList<>(SWITCH_CASES_CONTAINER_DEFAULT_CAPACITY);
		}
		c.setParent(this);
		if (getFactory().getEnvironment().buildStackChanges()) {
			getFactory().getEnvironment().pushToStack(new AddAction(new ListContext(
					this, this.cases), c));
		}
		cases.add(c);
		return (T) this;
	}

	@Override
	public boolean removeCase(CtCase<? super S> c) {
		if (cases == CtElementImpl.<CtCase<? super S>>emptyList()) {
			return false;
		}
		if (getFactory().getEnvironment().buildStackChanges()) {
			getFactory().getEnvironment().pushToStack(new DeleteAction(new ListContext(
					this, cases, cases.indexOf(c)), c));
		}
		return cases.remove(c);
	}

	@Override
	public CtSwitch<S> clone() {
		return (CtSwitch<S>) super.clone();
	}
}
