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
package spoon.diff.context;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;

import java.util.Map;

public class MapContext <K, V> extends Context {
	private final Map<K, V> map;
	private  K key;

	public MapContext(CtElement element, CtRole role, Map<K, V> map) {
		super(element, role);
		this.map = map;
	}

	public MapContext(CtElement element, CtRole role, Map<K, V> map, K key) {
		this(element, role, map);
		this.key = key;
	}

	public K getKey() {
		return key;
	}

	public Map<K, V> getMap() {
		return map;
	}
}