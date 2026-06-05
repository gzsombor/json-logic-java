package io.github.jamsesso.jsonlogic.inspector;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.compiler.JavacJsonLogicCompiler;
import io.github.jamsesso.jsonlogic.compiler.classfile.ClassFileJsonLogicCompiler;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public final class CompilerClassDump {

  private CompilerClassDump() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 2 || args.length > 3) {
      System.err.println("Usage: CompilerClassDump '<rule-json>' <output-dir> [--rule-file]");
      System.exit(2);
    }

    final String ruleJson = args.length == 3 && "--rule-file".equals(args[2])
        ? Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)
        : args[0];
    final Path outputDir = Path.of(args[1]);
    final JsonLogicNode ast = JsonLogicParser.parse(ruleJson);
    final JsonLogicEvaluator evaluator = new JsonLogicEvaluator(Collections.emptyList());

    writeClasses(
        outputDir.resolve("javac"),
        new JavacJsonLogicCompiler(evaluator, true).compileClassBytes(ruleJson, ast));
    writeClasses(
        outputDir.resolve("jep484"),
        new ClassFileJsonLogicCompiler(evaluator, true, false).generateClassBytes(ruleJson, ast));
  }

  private static void writeClasses(Path outputDir, Map<String, byte[]> classBytes) throws IOException {
    for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
      final Path classFile = outputDir.resolve(entry.getKey().replace('.', '/') + ".class");
      Files.createDirectories(classFile.getParent());
      Files.write(classFile, entry.getValue());
      System.out.println(classFile);
    }
  }
}
