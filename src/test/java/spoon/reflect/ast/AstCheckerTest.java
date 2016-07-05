package spoon.reflect.ast;

import org.junit.Test;
import spoon.Launcher;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.cu.CtLineElementComparator;
import spoon.template.TemplateMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AstCheckerTest {
	@Test
	public void testStackChanges() throws Exception {
		final Launcher launcher = new Launcher();
		launcher.getEnvironment().setNoClasspath(true);
		// interfaces.
		launcher.addInputResource("./src/main/java/spoon/reflect/code");
		launcher.addInputResource("./src/main/java/spoon/reflect/declaration");
		launcher.addInputResource("./src/main/java/spoon/reflect/reference");
		launcher.addInputResource("./src/main/java/spoon/reflect/internal");
		// Implementations.
		launcher.addInputResource("./src/main/java/spoon/support/reflect/code");
		launcher.addInputResource("./src/main/java/spoon/support/reflect/declaration");
		launcher.addInputResource("./src/main/java/spoon/support/reflect/reference");
		launcher.addInputResource("./src/main/java/spoon/support/reflect/internal");
		// Utils.
		launcher.addInputResource("./src/test/java/spoon/reflect/ast/AstCheckerTest.java");
		launcher.buildModel();

		final GetterListChecker getterListChecker = new GetterListChecker(launcher.getFactory());
		getterListChecker.scan(launcher.getModel().getRootPackage());
		if (getterListChecker.result != null) {
			throw new AssertionError(getterListChecker.result);
		}
	}

	@Test
	public void testAvoidSetCollectionSavedOnAST() throws Exception {
		final Launcher launcher = new Launcher();
		launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setBuildStackChanges(true);
		launcher.addInputResource("src/main/java");
		launcher.buildModel();

		final Factory factory = launcher.getFactory();
		final List<CtTypeReference<?>> collectionsRef = Arrays.asList(factory.Type().createReference(Collection.class), factory.Type().createReference(List.class), factory.Type().createReference(Set
						.class),
				factory.Type().createReference(Map.class));

		final List<CtInvocation<?>> invocations = Query.getElements(factory, new TypeFilter<CtInvocation<?>>(CtInvocation.class) {
			@Override
			public boolean matches(CtInvocation<?> element) {
				if (!element.getExecutable().getSimpleName().startsWith("get")) {
					return false;
				}
				if (!collectionsRef.contains(element.getType())) {
					return false;
				}
				if (!element.getExecutable().getDeclaringType().getSimpleName().startsWith("Ct")) {
					return false;
				}
				if (!(element.getParent() instanceof CtInvocation)) {
					return false;
				}
				final CtInvocation<?> parent = (CtInvocation<?>) element.getParent();
				if (!parent.getTarget().equals(element)) {
					return false;
				}
				final String simpleName = parent.getExecutable().getSimpleName();
				return simpleName.startsWith("add") || simpleName.startsWith("remove");
			}
		});
		if (invocations.size() > 0) {
			final String error = invocations.stream() //
					.sorted(new CtLineElementComparator()) //
					.map(i -> "see " + i.getPosition().getFile().getName() + " at " + i.getPosition().getLine()) //
					.collect(Collectors.joining(",\n"));
			throw new AssertionError(error);
		}
	}

	private class GetterListChecker extends CtScanner {
		private final List<CtTypeReference<?>> COLLECTIONS;
		private final CtExpression<Boolean> conditionExpected;
		private String result;

		GetterListChecker(Factory factory) {
			COLLECTIONS = Arrays.asList(factory.Type().createReference(Collection.class), factory.Type().createReference(List.class), factory.Type().createReference(Set.class));
			final CtType<Object> templateClass = factory.Type().get(Template.class);
			conditionExpected = ((CtIf) templateClass.getMethod("template").getBody().getStatement(0)).getCondition();
		}

		private boolean isToBeProcessed(CtMethod<?> candidate) {
			return candidate.getBody() != null //
					&& candidate.getParameters().size() == 0 //
					&& candidate.getDeclaringType().getSimpleName().startsWith("Ct") //
					&& COLLECTIONS.contains(candidate.getType()) //
					&& isConditionExpected(candidate.getBody().getStatement(0)) //
					&& isReturnCollection(candidate.getBody().getLastStatement());
		}

		private boolean isConditionExpected(CtStatement statement) {
			final TemplateMatcher matcher = new TemplateMatcher(conditionExpected);
			return matcher.find(statement).size() == 0;
		}

		private boolean isReturnCollection(CtStatement statement) {
			return statement instanceof CtReturn //
					&& ((CtReturn) statement).getReturnedExpression() instanceof CtFieldRead<?> //
					&& COLLECTIONS.contains(((CtFieldRead) ((CtReturn) statement).getReturnedExpression()).getVariable().getType());
		}

		private void process(CtMethod<?> element) {
			result += element.getSignature() + " on " + element.getDeclaringType().getQualifiedName() + "\n";
		}

		@Override
		public <T> void visitCtMethod(CtMethod<T> m) {
			if (isToBeProcessed(m)) {
				process(m);
			}
			super.visitCtMethod(m);
		}
	}

	class Template {
		public void template() {
			if (getFactory().getEnvironment().buildStackChanges()) {
			}
		}

		public Factory getFactory() {
			return null;
		}
	}
}
