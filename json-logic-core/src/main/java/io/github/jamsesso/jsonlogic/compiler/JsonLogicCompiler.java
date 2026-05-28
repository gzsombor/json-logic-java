package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.util.Comparator;
import java.util.ServiceLoader;

/** Discovers and delegates to the best available JSON Logic compiler implementation. */
public final class JsonLogicCompiler {

  private final JsonLogicCompilerImplementation implementation;

  /**
   * Creates a compiler backed by the highest priority implementation available on the classpath.
   *
   * @param fallbackEvaluator used as the fallback for operators that are not natively compiled
   * @throws IllegalStateException if no compiler implementation is available
   */
  public JsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator) {
    this(fallbackEvaluator, false);
  }

  public JsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    this(loadProvider(fallbackEvaluator, strictMode));
  }

  public JsonLogicCompiler(JsonLogicCompilerImplementation implementation) {
    this.implementation = implementation;
  }

  private static JsonLogicCompilerImplementation loadProvider(
      JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    return ServiceLoader.load(JsonLogicCompilerProvider.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .filter(JsonLogicCompilerProvider::isAvailable)
        .max(Comparator.comparingInt(JsonLogicCompilerProvider::priority))
        .map(provider -> provider.create(fallbackEvaluator, strictMode))
        .orElseThrow(() -> new IllegalStateException(
            "No JSON Logic compiler implementation is available on the classpath."));
  }

  /** Returns {@code true} if strict compilation mode is enabled. */
  public boolean isStrictMode() {
    return implementation.isStrictMode();
  }

  /** Returns a {@link CompiledRule} for {@code ast}. */
  public CompiledRule compile(String ruleJson, JsonLogicNode ast) throws JsonLogicCompilationException {
    return implementation.compile(ruleJson, ast);
  }
}
