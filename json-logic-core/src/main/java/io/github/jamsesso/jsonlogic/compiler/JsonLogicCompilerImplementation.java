package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;

public interface JsonLogicCompilerImplementation {
  boolean isStrictMode();

  CompiledRule compile(String ruleJson, JsonLogicNode ast) throws JsonLogicCompilationException;
}
