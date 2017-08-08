package spoon.support.compiler.jdt;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.visitor.filter.NameFilter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by urli on 08/08/2017.
 */
public class JDTImportBuilder {

    private final CompilationUnitDeclaration declarationUnit;
    private String filePath;
    private CompilationUnit spoonUnit;
    private ICompilationUnit sourceUnit;
    private Factory factory;
    private Set<CtReference> imports;

    JDTImportBuilder(CompilationUnitDeclaration declarationUnit,  Factory factory) {
        this.declarationUnit = declarationUnit;
        this.factory = factory;
        this.sourceUnit = declarationUnit.compilationResult.compilationUnit;
        this.filePath = CharOperation.charToString(sourceUnit.getFileName());
        this.spoonUnit = factory.CompilationUnit().create(filePath);
        this.imports = new HashSet<>();
    }

    public void build() {
        if (declarationUnit.imports == null || declarationUnit.imports.length == 0) {
            return;
        }

        for (ImportReference importRef : declarationUnit.imports) {
            String importName = importRef.toString();
            if (!importRef.isStatic()) {
                CtClass klass = this.getOrLoadClass(importName);
                if (klass != null) {
                    this.imports.add(klass.getReference());
                }
            } else {
                int lastDot = importName.lastIndexOf(".");
                String className = importName.substring(0, lastDot);
                String methodOrFieldName = importName.substring(lastDot+1);

                CtClass klass = this.getOrLoadClass(className);
                if (klass != null) {

                    if (methodOrFieldName.equals("*")) {
                        Collection<CtFieldReference<?>> fields = klass.getAllFields();
                        Set<CtMethod> methods = klass.getAllMethods();

                        for (CtFieldReference fieldReference : fields) {
                            if (fieldReference.isStatic()) {
                                this.imports.add(fieldReference.clone());
                            }
                        }

                        for (CtMethod method : methods) {
                            if (method.hasModifier(ModifierKind.STATIC)) {
                                this.imports.add(method.getReference());
                            }
                        }
                    } else {
                        List<CtField> fields = klass.getElements(new NameFilter<CtField>(methodOrFieldName));
                        List<CtMethod> methods = klass.getElements(new NameFilter<CtMethod>(methodOrFieldName));

                        if (fields.size() > 0) {
                            this.imports.add(fields.get(0).getReference());
                        } else if (methods.size() > 0) {
                            this.imports.add(methods.get(0).getReference());
                        }
                    }
                }
            }
        }

        spoonUnit.setImports(this.imports);
    }

    private CtClass getOrLoadClass(String className) {
        CtClass klass = this.factory.Class().get(className);

        if (klass == null) {
            try {
                Class zeClass = this.getClass().getClassLoader().loadClass(className);

                klass = this.factory.Class().get(zeClass);
                return klass;
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return klass;
    }
}
