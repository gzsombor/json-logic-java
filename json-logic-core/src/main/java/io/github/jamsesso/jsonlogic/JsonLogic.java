package io.github.jamsesso.jsonlogic;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompiler;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerImplementation;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.PreEvaluatedArgumentsExpression;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

public final class JsonLogic {
  private static final Logger LOG = Logger.getLogger(JsonLogic.class.getName());

  private final Map<String, Rule> ruleCache = new ConcurrentHashMap<>();
  private final JsonLogicEvaluator evaluator = new JsonLogicEvaluator();

  /**
   * Non-null when compilation is enabled (default). Pass {@code false} to {@link #JsonLogic(boolean)} to disable.
   */
  private JsonLogicCompiler compiler;

  public JsonLogic() {
    this(true);
  }

  public JsonLogic(boolean enableCompilation) {
    this(enableCompilation, false);
  }

  public JsonLogic(boolean enableCompilation, boolean strictCompilation) {
    if (enableCompilation) {
      try {
        this.compiler = new JsonLogicCompiler(evaluator, strictCompilation);
      } catch (IllegalStateException e) {
        LOG.warning("Compilation is unavailable. "
            + "Rules will be evaluated by the interpreter. "
            + "To suppress this warning, use new JsonLogic(false) (" + e.getMessage() + ')');
        this.compiler = null;
      }
    }
  }

  JsonLogic(Function<JsonLogicEvaluator, JsonLogicCompilerImplementation> compilerFactory) {
    this.compiler = new JsonLogicCompiler(compilerFactory.apply(evaluator));
  }

  /** Returns {@code true} if strict compilation mode is enabled. */
  public boolean isStrictCompilation() {
    return compiler != null ? compiler.isStrictMode() : false;
  }

  public static boolean truthy(Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof Boolean) {
      return (boolean) value;
    }

    if (value instanceof Number) {
      if (value instanceof Double) {
        Double d = (Double) value;

        if (d.isNaN()) {
          return false;
        } else if (d.isInfinite()) {
          return true;
        }
      }

      if (value instanceof Float) {
        Float f = (Float) value;

        if (f.isNaN()) {
          return false;
        } else if (f.isInfinite()) {
          return true;
        }
      }

      return ((Number) value).doubleValue() != 0.0;
    }

    if (value instanceof String) {
      return !((String) value).isEmpty();
    }

    if (value instanceof Collection) {
      return !((Collection<?>) value).isEmpty();
    }

    if (value.getClass().isArray()) {
      return Array.getLength(value) > 0;
    }

    return true;
  }

  public JsonLogic addOperation(String name, Function<Object[], Object> function) {
    return addOperation(new PreEvaluatedArgumentsExpression() {
      @Override
      public Object evaluate(List arguments, Object data, String jsonPath) {
        return function.apply(arguments.toArray());
      }

      @Override
      public String key() {
        return name;
      }
    });
  }

  public JsonLogic addOperation(JsonLogicExpression expression) {
    evaluator.addOperation(expression);
    ruleCache.clear();
    return this;
  }

  /** Returns {@code true} if compilation is currently enabled. */
  public boolean isCompilationEnabled() {
    return compiler != null;
  }

  public Object apply(String json, Object data) throws JsonLogicException {
    Rule rule;
    try {
      rule = ruleCache.computeIfAbsent(json, k -> {
        try {
          final JsonLogicNode ast = JsonLogicParser.parse(k);
          final CompiledRule compiledRule = compiler == null ? null : compiler.compile(k, ast);
          return new Rule(ast, compiledRule);
        } catch (JsonLogicException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof JsonLogicException) throw (JsonLogicException) e.getCause();
      throw e;
    }

    return rule.evaluate(data, evaluator);
  }

  private static final class Rule {
    private final JsonLogicNode ast;
    private final CompiledRule compiledRule;

    private Rule(JsonLogicNode ast, CompiledRule compiledRule) {
      this.ast = ast;
      this.compiledRule = compiledRule;
    }

    Object evaluate(Object data, JsonLogicEvaluator fallbackEvaluator) throws JsonLogicException {
      if (compiledRule != null) {
        try {
          return compiledRule.apply(data);
        } catch (JsonLogicException e) {
          e.prependPartialJsonPath("$");
          throw e;
        }
      }

      try {
        return fallbackEvaluator.evaluate(ast, data);
      } catch (JsonLogicException e) {
        e.prependPartialJsonPath("$");
        throw e;
      }
    }
  }
}
