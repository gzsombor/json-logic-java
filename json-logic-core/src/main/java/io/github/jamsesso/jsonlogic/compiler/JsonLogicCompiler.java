package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Optional;
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
    this(fallbackEvaluator, false, true);
  }

  public JsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    this(fallbackEvaluator, strictMode, true);
  }

  public JsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator, boolean strictMode, boolean fallbackEnabled) {
    this(loadProvider(fallbackEvaluator, strictMode, fallbackEnabled));
  }

  public JsonLogicCompiler(JsonLogicCompilerImplementation implementation) {
    this.implementation = implementation;
  }

  private static JsonLogicCompilerImplementation loadProvider(
      JsonLogicEvaluator fallbackEvaluator, boolean strictMode, boolean fallbackEnabled) {
    return selectProvider(discoverProviders())
            .map(provider -> provider.create(fallbackEvaluator, strictMode, fallbackEnabled))
            .orElseThrow(() -> new IllegalStateException(
                    "No JSON Logic compiler implementation is available on the classpath."));
  }

  /**
   * Finds every {@link JsonLogicCompilerProvider} registered on the classpath,just those that are compiled for a newer JVM
   * are skipped.
   */
  public static List<JsonLogicCompilerProvider> discoverProviders() {
    List<JsonLogicCompilerProvider> result = new ArrayList<>();
    Iterator<JsonLogicCompilerProvider> it =
        ServiceLoader.load(JsonLogicCompilerProvider.class).iterator();
    while (true) {
      try {
        if (!it.hasNext()) {
          return result;
        }
        result.add(it.next());
      } catch (LinkageError | ServiceConfigurationError e) {
        //
      }
    }
  }

  /** Returns the available provider with the highest priority, if there is any. */
  public static Optional<JsonLogicCompilerProvider> selectProvider(
      List<JsonLogicCompilerProvider> providers) {
    return providers.stream()
        .filter(JsonLogicCompilerProvider::isAvailable)
        .max(Comparator.comparingInt(JsonLogicCompilerProvider::priority));
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
