package io.github.jamsesso.jsonlogic.compiler.classfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.constant.ClassDesc;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class ClassFileJsonLogicCompilerBytecodeTest {

  static Stream<Object[]> scenarios() throws Exception {
    final List<Object[]> params = new ArrayList<>();
    addScenariosFrom("scenarios", params);
    addScenariosFrom("scenarios/error", params);
    return params.stream();
  }

  private static void addScenariosFrom(String resource, List<Object[]> params) throws Exception {
    final URL dir = ClassFileJsonLogicCompilerBytecodeTest.class.getClassLoader().getResource(resource);
    if (dir == null) {
      return;
    }
    try (Stream<Path> paths = Files.list(Paths.get(dir.toURI()))) {
      paths.filter(path -> path.toString().endsWith(".json"))
          .sorted()
          .forEach(jsonPath -> {
            final String base = jsonPath.getFileName().toString().replace(".json", "");
            params.add(new Object[]{base, jsonPath, jsonPath.resolveSibling(base + ".bytecode")});
          });
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  public void generatedBytecodeMatchesFixture(String scenarioName, Path jsonPath, Path bytecodePath) throws Exception {
    final String json = Files.readString(jsonPath, StandardCharsets.UTF_8).trim();
    final String actualBytecode = disassemble(json);

    if (!Files.exists(bytecodePath)) {
      Files.writeString(bytecodePath, actualBytecode, StandardCharsets.UTF_8);
      fail("Created missing fixture (re-run to verify): " + bytecodePath.getFileName());
    }

    final String expectedBytecode = Files.readString(bytecodePath, StandardCharsets.UTF_8);
    assertEquals(expectedBytecode.stripTrailing(), actualBytecode.stripTrailing(), scenarioName);
  }

  private static String disassemble(String ruleJson) throws Exception {
    final String className = ClassFileJsonLogicCompiler.classNameFor(ruleJson);
    final ClassDesc generatedClass = ClassDesc.of(ClassFileJsonLogicCompiler.GENERATED_PACKAGE, className);
    final byte[] classBytes = new ClassFileJsonLogicCompiler(new JsonLogicEvaluator(), true, true)
        .generateClass(generatedClass, JsonLogicParser.parse(ruleJson));

    final var classNode = new ClassNode();
    new ClassReader(classBytes).accept(classNode, 0);
    final var result = new StringBuilder();
    for (MethodNode method : classNode.methods) {
      if ("<init>".equals(method.name) || "toString".equals(method.name)) {
        continue;
      }
      if (result.length() > 0) {
        result.append("\n");
      }
      result.append("// ").append(method.name).append(" ").append(method.desc).append("\n");
      result.append(disassemble(method));
    }
    if (result.length() == 0) {
      throw new AssertionError("Generated class has no testable methods: " + classNode.name);
    }
    return result.toString();
  }

  private static String disassemble(MethodNode method) {
    final var printer = new Textifier();
    method.accept(new TraceMethodVisitor(printer));
    final var writer = new StringWriter();
    printer.print(new PrintWriter(writer));
    return writer.toString();
  }
}
