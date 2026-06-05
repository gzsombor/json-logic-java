package io.github.jamsesso.jsonlogic.bench;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.compiler.JavacJsonLogicCompiler;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompiler;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerImplementation;
import io.github.jamsesso.jsonlogic.compiler.classfile.ClassFileJsonLogicCompiler;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.util.Collections;
import java.util.function.Function;

final class BenchmarkCompilerEngines {

  private BenchmarkCompilerEngines() {}

  static JsonLogic jsonLogicFor(String engine) {
    if ("interpreter".equals(engine)) {
      return new JsonLogic(false);
    }
    return JsonLogic.withCompiler(compilerFactoryFor(engine));
  }

  static JsonLogicCompiler compilerFor(String compilerEngine) {
    final JsonLogicEvaluator evaluator = new JsonLogicEvaluator(Collections.emptyList());
    return new JsonLogicCompiler(compilerFactoryFor(compilerEngine).apply(evaluator));
  }

  private static Function<JsonLogicEvaluator, JsonLogicCompilerImplementation> compilerFactoryFor(
      String compilerEngine) {
    switch (compilerEngine) {
      case "javac":
        return evaluator -> new JavacJsonLogicCompiler(evaluator, true);
      case "jep484":
        return evaluator -> new ClassFileJsonLogicCompiler(evaluator, true, false);
      default:
        throw new IllegalArgumentException("Unknown compiler engine: " + compilerEngine);
    }
  }
}
