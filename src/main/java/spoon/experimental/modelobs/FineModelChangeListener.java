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
package spoon.experimental.modelobs;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.path.CtRole;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * is the listener that creates the action on the model. This default listener does nothing.
 */
public class FineModelChangeListener {

	public void onObjectUpdate(CtElement currentElement, CtRole role, CtElement newValue, CtElement oldValue) {
	}

	public void onObjectUpdate(CtElement currentElement, CtRole role, Object newValue, Object oldValue) {
	}

	public void onObjectDelete(CtElement currentElement, CtRole role, CtElement oldValue) {
	}

	public void onListAdd(CtElement currentElement, CtRole role, List field, CtElement newValue) {
	}

	public void onListAdd(CtElement currentElement, CtRole role, List field, int index, CtElement newValue) {
	}


	public void onListDelete(CtElement currentElement, CtRole role, List field, Collection<? extends CtElement> oldValue) {
		for (CtElement ctElement : oldValue) {
			onListDelete(currentElement, role, field, field.indexOf(ctElement), ctElement);
		}
	}

	public void onListDelete(CtElement currentElement, CtRole role, List field, int index, CtElement oldValue) {
	}


	public void onListDeleteAll(CtElement currentElement, CtRole role, List field, List oldValue) {
	}


	public <K, V> void onMapAdd(CtElement currentElement, CtRole role, Map<K, V> field, K key, CtElement newValue) {
	}

	public <K, V> void onMapDeleteAll(CtElement currentElement, CtRole role, Map<K, V> field, Map<K, V> oldValue) {
	}

	public void onSetAdd(CtElement currentElement, CtRole role, Set field, CtElement newValue) {
	}

	public void onSetAdd(CtElement currentElement, CtRole role, Set field, ModifierKind newValue) {
	}


	public void onSetDelete(CtElement currentElement, CtRole role, Set field, CtElement oldValue) {
	}

	public void onSetDelete(CtElement currentElement, CtRole role, Set field, Collection<ModifierKind> oldValue) {
		for (ModifierKind modifierKind : oldValue) {
			onSetDelete(currentElement, role, field, modifierKind);
		}
	}

	public void onSetDelete(CtElement currentElement, CtRole role, Set field, ModifierKind oldValue) {
	}

	public void onSetDeleteAll(CtElement currentElement, CtRole role, Set field, Set oldValue) {
	}
}
