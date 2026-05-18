package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import javax.tools.ToolProvider;

public final class JavacJsonLogicCompilerProvider implements JsonLogicCompilerProvider {
  @Override
  public int priority() {
    return 100;
  }

  @Override
  public boolean isAvailable() {
    return ToolProvider.getSystemJavaCompiler() != null;
  }

  @Override
  public JsonLogicCompilerImplementation create(JsonLogicEvaluator fallbackEvaluator) {
    return new JavacJsonLogicCompiler(fallbackEvaluator);
  }
}
