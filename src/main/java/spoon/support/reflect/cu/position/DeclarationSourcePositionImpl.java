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
package spoon.support.reflect.cu.position;

import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.position.DeclarationSourcePosition;

import java.io.Serializable;

/**
 * This class represents the position of a Java program element in a source
 * file.
 */
public class DeclarationSourcePositionImpl extends SourcePositionImpl
		implements DeclarationSourcePosition, Serializable {

	private static final long serialVersionUID = 1L;
	private int modifierSourceEnd;
	private int modifierSourceStart;
	private int declarationSourceStart;
	private int declarationSourceEnd;

	public DeclarationSourcePositionImpl(CompilationUnit compilationUnit, int sourceStart, int sourceEnd,
			int modifierSourceStart, int modifierSourceEnd, int declarationSourceStart, int declarationSourceEnd,
			int[] lineSeparatorPositions) {
		super(compilationUnit,
				sourceStart, sourceEnd,
				lineSeparatorPositions);
		checkArgsAreAscending(declarationSourceStart, modifierSourceStart, modifierSourceEnd + 1, sourceStart, sourceEnd + 1, declarationSourceEnd + 1);
		this.modifierSourceStart = modifierSourceStart;
		this.declarationSourceStart = declarationSourceStart;
		this.declarationSourceEnd = declarationSourceEnd;
		if (this.modifierSourceStart == 0) {
			this.modifierSourceStart = declarationSourceStart;
		}
		this.modifierSourceEnd = modifierSourceEnd;
	}

	@Override
	public int getSourceEnd() {
		return declarationSourceEnd;
	}

	@Override
	public int getSourceStart() {
		return declarationSourceStart;
	}

	@Override
	public int getModifierSourceStart() {
		return modifierSourceStart;
	}

	@Override
	public int getNameStart() {
		return super.getSourceStart();
	}

	@Override
	public int getNameEnd() {
		return super.getSourceEnd();
	}

	public void setModifierSourceEnd(int modifierSourceEnd) {
		this.modifierSourceEnd = modifierSourceEnd;
	}

	@Override
	public int getModifierSourceEnd() {
		return modifierSourceEnd;
	}

	public int getEndLine() {
		return searchLineNumber(declarationSourceEnd);
	}

	/**
	 * @return origin source code of modifiers
	 */
	public String getModifierSourceFragment() {
		return getFragment(getModifierSourceStart(), getModifierSourceEnd());
	}

	/**
	 * @return origin source code of `name`
	 */
	public String getNameSourceFragment() {
		return getFragment(getNameStart(), getNameEnd());
	}

	protected String getSourceInfo() {
		return super.getSourceInfo()
				+ "\nmodifier = " + getModifierSourceFragment()
				+ "\nname = " + getNameSourceFragment();
	}

}
