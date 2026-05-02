package io.github.jamsesso.jsonlogic.evaluator.expressions;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;

import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.toComparableDouble;

import java.util.List;

public class NumericComparisonExpression implements PreEvaluatedArgumentsExpression {
  public static final NumericComparisonExpression GT = new NumericComparisonExpression(">");
  public static final NumericComparisonExpression GTE = new NumericComparisonExpression(">=");
  public static final NumericComparisonExpression LT = new NumericComparisonExpression("<");
  public static final NumericComparisonExpression LTE = new NumericComparisonExpression("<=");

  private final String key;

  private NumericComparisonExpression(String key) {
    this.key = key;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public Object evaluate(List arguments, Object data, String jsonPath) throws JsonLogicEvaluationException {
    // Convert the arguments to doubles
    int n = Math.min(arguments.size(), 3);

    if (n < 2) {
      throw new JsonLogicEvaluationException("'" + key + "' requires at least 2 arguments", jsonPath);
    }

    double[] values = new double[n];

    for (int i = 0; i < n; i++) {
      values[i] = toComparableDouble(arguments.get(i));
      if (Double.isNaN(values[i])) {
        return false;
      }
    }

    // Handle between comparisons
    if (arguments.size() >= 3) {
      switch (key) {
        case "<":
          return values[0] < values[1] && values[1] < values[2];

        case "<=":
          return values[0] <= values[1] && values[1] <= values[2];

        case ">":
          return values[0] > values[1] && values[1] > values[2];

        case ">=":
          return values[0] >= values[1] && values[1] >= values[2];

        default:
          throw new JsonLogicEvaluationException("'" + key + "' does not support between comparisons", jsonPath);
      }
    }

    // Handle regular comparisons
    switch (key) {
      case "<":
        return values[0] < values[1];

      case "<=":
        return values[0] <= values[1];

      case ">":
        return values[0] > values[1];

      case ">=":
        return values[0] >= values[1];

      default:
        throw new JsonLogicEvaluationException("'" + key + "' is not a comparison expression", jsonPath);
    }
  }
}
