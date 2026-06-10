package io.github.jamsesso.jsonlogic.compiler.gen;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
import java.util.*;

public final class TestRule implements CompiledRule {

  private final JsonLogicEvaluator fallback;
  private final JsonLogicNode[] fallbackNodes;
  private final String ruleJson;

  public TestRule(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson) {
    this.fallback = fallback;
    this.fallbackNodes = fallbackNodes;
    this.ruleJson = ruleJson;
  }

  @Override
  public String toString() {
    return "CompiledRule(" + ruleJson + ")";
  }

  @Override
  public Object apply(Object data) throws JsonLogicEvaluationException {
    final Object var_data_5 = resolveVarChecked(data, "data", null);
    Object reduceArray_1 = var_data_5;
    Object reduceAccumulator_2 = 0.0;
    Object result_0 = reduceAccumulator_2;
    if (ArrayLike.isEligible(reduceArray_1)) {
      final Object var_multiplicator_6 = resolveVarChecked(data, "multiplicator", null);
      for (Object reduceItem_3 : ArrayLike.iterable(reduceArray_1)) {
        Object arith_10;
        if (var_multiplicator_6 == null || !isNumeric(var_multiplicator_6) || reduceItem_3 == null || !isNumeric(reduceItem_3) || reduceAccumulator_2 == null || !isNumeric(reduceAccumulator_2)) {
          arith_10 = null;
        } else {
          double _d_7 = toDouble(var_multiplicator_6);
          double _d_8 = toDouble(reduceItem_3);
          double _d_9 = toDouble(reduceAccumulator_2);
          arith_10 = (_d_7 * (_d_8 + _d_9));
        }
        Object reduceBody_4 = arith_10;
        reduceAccumulator_2 = reduceBody_4;
      }
      result_0 = reduceAccumulator_2;
    }
    return result_0;
  }
}
