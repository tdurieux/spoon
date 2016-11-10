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
package spoon.reflect.builder.generator;

import spoon.Launcher;
import spoon.reflect.builder.template.AnnotationBuilder;
import spoon.reflect.builder.template.AnnotationBuilderTarget;
import spoon.reflect.builder.template.Builder;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AnnotationFilter;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.JavaOutputProcessor;
import spoon.support.reflect.code.CtLiteralImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Main {

	private static Factory factory;
	private static CtField<Factory> fieldFactory;
	private static Launcher launcher;
	private static String path = "src/main/java/spoon/reflect/builder/template";
	private static Set<CtInterface<?>> interfaces;

	public static void main(String[] args) {
		launcher = new Launcher();
		launcher.addInputResource(path);
		launcher.buildModel();
		launcher.getEnvironment().setAutoImports(true);

		factory = launcher.getFactory();

		List<CtClass> builders = launcher.getModel().getElements(new AnnotationFilter<CtClass>(AnnotationBuilder.class));

		CtClass<?> builderClass = generateBuilderClass();

		interfaces = test(builders);
		for (int i = 0; i < builders.size(); i++) {
			CtClass<?> builder = builders.get(i);
			generateBuilder(builderClass, builder);
		}
		print(builderClass);
	}

	private static Set<CtInterface<?>> test(List<CtClass> builders) {
		Set<CtInterface<?>> output = new HashSet<>();
		for (int j = 0; j < builders.size(); j++) {
			CtClass builder = builders.get(j);
			CtElement parent = builder.getParent().getParent();
			builder = builder.clone();
			builder.setParent(parent);
			Set<CtConstructor<?>> constructors = builder.getConstructors();
			for (Iterator<CtConstructor<?>> iterator = constructors.iterator(); iterator.hasNext(); ) {
				CtConstructor<?> constructor = iterator.next();
				List<CtParameter<?>> parameters = constructor.getParameters();
				for (int i = 0; i < parameters.size(); i++) {
					CtParameter<?> ctParameter = parameters.get(i);
					if ("factory".equals(ctParameter.getSimpleName())) {
						continue;
					}
					AnnotationBuilderTarget annotation = ctParameter.getAnnotation(AnnotationBuilderTarget.class);
					if (annotation != null) {
						CtInterface<?> anInterface = createInterface(ctParameter.getType());

						String name = (String) ((CtLiteralImpl) builder.getAnnotations().get(0).getValue("name")).getValue();

						List<CtParameter<?>> args = new ArrayList<>();
						for (int k = 2; k < constructor.getParameters().size(); k++) {
							CtParameter parameter = constructor.getParameters().get(k).clone();
							ctParameter.setAnnotations(Collections.EMPTY_LIST);
							if (factory.Type().createReference(CtElement.class).isSubtypeOf(ctParameter.getType())) {
								CtTypeReference<?> reference = createInterface(ctParameter.getType()).getReference();
								reference.addActualTypeArgument(factory.Core().createWildcardReference());
								parameter.setType(reference);
							}
							args.add(parameter);
						}

						factory.Method().create(anInterface, Collections.EMPTY_SET, builder.getReference(), name, args, Collections.EMPTY_SET);
						print(anInterface);
						output.add(anInterface);
						break;
					}
				}
			}
		}
		return output;
	}

	private static void generateBuilder(CtClass<?> builderClass, CtClass<?> builder) {
		CtElement parent = builder.getParent().getParent();
		builder = builder.clone();
		builder.setParent(parent);
		Set<? extends CtConstructor<?>> constructors = builder.getConstructors();
		String name = (String) ((CtLiteral) builder.getAnnotations().get(0).getValue("name")).getValue();
		CtTypeReference type = ((CtTypeAccess<?>) ((CtFieldRead) builder.getAnnotations().get(0).getValue("type")).getTarget()).getAccessedType();

		for (Iterator<CtInterface<?>> iterator = interfaces
				.iterator(); iterator.hasNext(); ) {
			CtInterface<?> inter = iterator.next();
			if (inter.isSubtypeOf(type)) {
				builder.addSuperInterface(inter.getReference());
			}
		}
		for (Iterator<? extends CtConstructor<?>> iterator = constructors.iterator(); iterator.hasNext(); ) {
			CtConstructor<?> constructor = iterator.next();

			List<CtParameter<?>> parameters = new ArrayList<>();
			for (int i = 0; i < constructor.getParameters().size(); i++) {
				CtParameter<?> parameter = constructor.getParameters().get(i);
				if (parameter.getSimpleName().equals("factory")) {
					continue;
				}
				CtParameter ctParameter = parameter;
				ctParameter.setAnnotations(Collections.EMPTY_LIST);
				if (factory.Type().createReference(CtElement.class).isSubtypeOf(ctParameter.getType())) {
					CtTypeReference<?> interfaceReference = createInterface(ctParameter.getType()).getReference();
					interfaceReference.addActualTypeArgument(factory.Core().createWildcardReference());
					ctParameter.setType(interfaceReference);
				}
				parameters.add(ctParameter);
			}
			CtInvocation superConstructorCall = constructor.getBody().getStatement(0);
			List<CtVariableRead> elements = superConstructorCall.getElements(new TypeFilter<>(CtVariableRead.class));
			for (int i = 0; i < elements.size(); i++) {
				CtVariableRead ctVariableRead =  elements.get(i);
				CtVariable declaration = ctVariableRead.getVariable().getDeclaration();
				if (declaration == null) {
					continue;
				}
				CtTypeReference declarationType = declaration.getType();
				if (interfaces.contains(declarationType.getDeclaration())) {
					ctVariableRead.replace(factory.Code().createInvocation(ctVariableRead.clone(), factory.Executable().createReference(
							declarationType, null, "build")));
				}
			}
			CtMethod<?> method = factory.Method().create(builderClass,
					Collections.singleton(ModifierKind.PUBLIC),
					builder.getReference(), name, parameters,
					Collections.EMPTY_SET);

			CtBlock block = factory.Core().createBlock();
			method.setBody(block);

			List<CtExpression> args = new ArrayList<>();
			args.add(factory.Code().createVariableRead(fieldFactory.getReference(), false));
			for (int i = 0; i < parameters.size(); i++) {
				CtParameter<?> ctParameter = parameters.get(i);
				CtVariableAccess<?> variableRead = factory.Code().createVariableRead(ctParameter.getReference(), false);
				args.add(variableRead);
			}
			CtReturn aReturn = factory.Core().createReturn();
			aReturn.setReturnedExpression(factory.Code().createConstructorCall(builder.getReference(), args.toArray(new CtExpression[]{})));
			block.addStatement(aReturn);
		}
		print(builder);
	}

	private static CtInterface<?> createInterface(CtTypeReference type) {
		String simpleName = type.getSimpleName();
		simpleName = simpleName.substring(2);
		String builderName = "spoon.reflect.builder." + simpleName + "Builder";
		CtInterface<?> builderInterface = factory.Interface().get(builderName);
		if (builderInterface == null) {
			builderInterface = factory.Interface().create(builderName);
			CtTypeParameter typeParameter = factory.Core().createTypeParameter();
			typeParameter.setSimpleName("T");
			typeParameter.setSuperclass(type.clone());
			builderInterface.addFormalCtTypeParameter(typeParameter);

			CtTypeReference<?> ctTypeReference = factory.Code().createCtTypeReference(Builder.class);
			ctTypeReference.addActualTypeArgument(typeParameter.getReference().setBounds(null));
			builderInterface.addSuperInterface(ctTypeReference);
		}
		return builderInterface;
	}

	private static CtClass<?> generateBuilderClass() {
		CtClass<?> builder = factory.Class().get("spoon.reflect.builder.BuilderFactory");
		if (builder != null) {
			builder.delete();
		}
		builder = factory.Class().create("spoon.reflect.builder.BuilderFactory");
		builder.addModifier(ModifierKind.PUBLIC);

		Set<ModifierKind> modifiers = new HashSet<>();
		modifiers.add(ModifierKind.PRIVATE);
		fieldFactory = Main.factory.Field().create(builder, modifiers, Main.factory.Type().createReference(Factory.class), "factory");

		List<CtParameter<?>> parameters = new ArrayList<>();
		CtParameter<Factory> parameter = factory.Core().createParameter();
		parameter.setType(fieldFactory.getType().clone());
		parameter.setSimpleName("factory");
		parameters.add(parameter);

		CtConstructor<?> ctConstructor = factory.Constructor().create(builder, Collections.singleton(ModifierKind.PUBLIC), parameters, Collections.EMPTY_SET);

		CtAssignment<Factory, Factory> variableAssignment = factory.Code()
				.createVariableAssignment(fieldFactory.getReference(), false,
						factory.Code().createVariableRead(parameter.getReference(), false));
		ctConstructor.setBody(factory.Core().createBlock().addStatement(variableAssignment));

		return builder;
	}

	private static void print(CtType type) {
		JavaOutputProcessor outputProcessor = new JavaOutputProcessor(new File("src/main/java"), launcher.createPrettyPrinter());
		outputProcessor.setFactory(factory);
		outputProcessor.createJavaFile(type);
	}
}
