package io.github.jamsesso.jsonlogic;

import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilationException;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompiler;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

public final class JsonLogic {
  private static final Logger LOG = Logger.getLogger(JsonLogic.class.getName());

  private final Map<String, JsonLogicNode> parseCache = new ConcurrentHashMap<>();
  private final Map<String, JsonLogicExpression> expressions = new ConcurrentHashMap<>();
  private JsonLogicEvaluator evaluator;

  /** Non-null when compilation is enabled (default). Pass {@code false} to {@link #JsonLogic(boolean)} to disable. */
  private JsonLogicCompiler compiler;

  public JsonLogic() {
    this(true);
  }

  public JsonLogic(boolean enableCompilation) {
    // Add default operations
    addOperation(MathExpression.ADD);
    addOperation(MathExpression.SUBTRACT);
    addOperation(MathExpression.MULTIPLY);
    addOperation(MathExpression.DIVIDE);
    addOperation(MathExpression.MODULO);
    addOperation(MathExpression.MIN);
    addOperation(MathExpression.MAX);
    addOperation(NumericComparisonExpression.GT);
    addOperation(NumericComparisonExpression.GTE);
    addOperation(NumericComparisonExpression.LT);
    addOperation(NumericComparisonExpression.LTE);
    addOperation(IfExpression.IF);
    addOperation(IfExpression.TERNARY);
    addOperation(EqualityExpression.INSTANCE);
    addOperation(InequalityExpression.INSTANCE);
    addOperation(StrictEqualityExpression.INSTANCE);
    addOperation(StrictInequalityExpression.INSTANCE);
    addOperation(NotExpression.SINGLE);
    addOperation(NotExpression.DOUBLE);
    addOperation(LogicExpression.AND);
    addOperation(LogicExpression.OR);
    addOperation(LogExpression.STDOUT);
    addOperation(MapExpression.INSTANCE);
    addOperation(FilterExpression.INSTANCE);
    addOperation(ReduceExpression.INSTANCE);
    addOperation(AllExpression.INSTANCE);
    addOperation(ArrayHasExpression.SOME);
    addOperation(ArrayHasExpression.NONE);
    addOperation(MergeExpression.INSTANCE);
    addOperation(InExpression.INSTANCE);
    addOperation(ConcatenateExpression.INSTANCE);
    addOperation(SubstringExpression.INSTANCE);
    addOperation(MissingExpression.ALL);
    addOperation(MissingExpression.SOME);

    // Enable compilation by default; fall back gracefully if no JDK compiler is present.
    if (enableCompilation) {
      try {
        this.compiler = new JsonLogicCompiler(getOrBuildEvaluator());
      } catch (IllegalStateException e) {
        LOG.warning("Compilation is unavailable. "
            + "Rules will be evaluated by the interpreter. "
            + "To suppress this warning, use new JsonLogic(false).");
        this.compiler = null;
      }
    }
  }

  /**
   * Enables or disables strict compilation mode.
   * When enabled, compilation failures throw {@link JsonLogicCompilationException} instead of
   * falling back to the interpreter. Useful for testing that all rules are compilable.
   */
  public JsonLogic setStrictCompilation(boolean strict) {
    if (compiler != null) {
      compiler.setStrictMode(strict);
    }
    return this;
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
      return !((Collection) value).isEmpty();
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
    expressions.put(expression.key(), expression);
    evaluator = null;
    if (compiler != null) {
      compiler.invalidate();
    }
    return this;
  }

  /** Returns {@code true} if compilation is currently enabled. */
  public boolean isCompilationEnabled() {
    return compiler != null;
  }

  public Object apply(String json, Object data) throws JsonLogicException {
    JsonLogicNode ast;
    try {
      ast = parseCache.computeIfAbsent(json, k -> {
        try {
          return JsonLogicParser.parse(k);
        } catch (JsonLogicException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof JsonLogicException) throw (JsonLogicException) e.getCause();
      throw e;
    }

    if (compiler != null) {
      CompiledRule rule = compiler.compile(json, ast);
      try {
        return rule.apply(data);
      } catch (JsonLogicException e) {
        e.prependPartialJsonPath("$");
        throw e;
      }
    }

    if (evaluator == null) {
      evaluator = new JsonLogicEvaluator(expressions);
    }

    try {
      return evaluator.evaluate(ast, data);
    } catch (JsonLogicException e) {
      e.prependPartialJsonPath("$");
      throw e;
    }
  }

  private JsonLogicEvaluator getOrBuildEvaluator() {
    if (evaluator == null) {
      evaluator = new JsonLogicEvaluator(expressions);
    }
    return evaluator;
  }
}
