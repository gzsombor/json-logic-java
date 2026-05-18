package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;

public interface JsonLogicCompilerProvider {
  int priority();

  boolean isAvailable();

  JsonLogicCompilerImplementation create(JsonLogicEvaluator fallbackEvaluator);
}
