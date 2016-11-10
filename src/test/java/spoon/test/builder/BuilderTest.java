package spoon.test.builder;

import org.junit.Test;
import spoon.Launcher;
import spoon.reflect.builder.BuilderFactory;

public class BuilderTest {

	@Test
	public void test() {
		BuilderFactory b = new Launcher().getFactory().Builder();
		System.out.println(b.literal(1).plus(b.literal(2).plus(b.literal(5))));
	}

}
