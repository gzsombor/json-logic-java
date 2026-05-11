package io.github.jamsesso.jsonlogic.evaluator.expressions;

import io.github.jamsesso.jsonlogic.compiler.RuleHelpers;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

import java.util.List;

public abstract class MissingExpression implements PreEvaluatedArgumentsExpression {
  public static final MissingExpression ALL = new MissingAllExpression();
  public static final MissingExpression SOME = new MissingSomeExpression();

  private static final class MissingAllExpression extends MissingExpression {
    @Override
    public final String key() {
      return "missing";
    }

    @Override
    public Object evaluate(List arguments, Object data, String jsonPath) {
      return RuleHelpers.missing(arguments, data);
    }
  }

  private static final class MissingSomeExpression extends MissingExpression {
    @Override
    public final String key() {
      return "missing_some";
    }

    @Override
    public Object evaluate(List arguments, Object data, String jsonPath) throws JsonLogicEvaluationException {
      if (arguments.size() < 2
          || !(arguments.get(0) instanceof Double)
          || !ArrayLike.isEligible(arguments.get(1))) {
        throw new JsonLogicEvaluationException(
            "missing_some expects first argument to be an integer and the second argument to be an array",
            jsonPath);
      }

      int requiredCount = ((Double) arguments.get(0)).intValue();
      List<?> keys = new ArrayLike(arguments.get(1));
      return RuleHelpers.missingSome(requiredCount, keys, data);
    }
  }

}
