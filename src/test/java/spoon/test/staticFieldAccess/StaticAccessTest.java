package spoon.test.staticFieldAccess;

import org.junit.Before;
import org.junit.Test;
import spoon.Launcher;
import spoon.compiler.SpoonCompiler;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.CtType;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class StaticAccessTest {
	private Launcher spoon;
	private File output;

	@Before
	public void setUp() throws Exception {
		spoon = new Launcher();
		spoon.addInputResource("./src/test/java/spoon/test/staticFieldAccess/internal/");
		spoon.addInputResource("./src/test/java/spoon/test/staticFieldAccess/StaticAccessBug.java");
		spoon.setSourceOutputDirectory(output = new File("target/spooned/staticFieldAccess"));
	}

	@Test
	public void testReferences() throws Exception {
		spoon.buildModel();
		CtType<?> type = (CtType<?>) spoon.getFactory().Type().get("spoon.test.staticFieldAccess.StaticAccessBug");
		CtBlock<?> block = type.getMethod("references").getBody();
		assertTrue(block.getStatement(0).toString().contains("Extends.MY_STATIC_VALUE"));
		assertTrue(block.getStatement(1).toString().contains("Extends.MY_OTHER_STATIC_VALUE"));
	}

	@Test
	public void testProcessAndCompile() throws Exception {
		spoon.addProcessor(new InsertBlockProcessor());
		spoon.run();

		// try to reload generated datas
		SpoonCompiler compiler = new Launcher().createCompiler(SpoonResourceHelper.resources(output.getAbsolutePath()));
		assertTrue(compiler.build());
	}

}
