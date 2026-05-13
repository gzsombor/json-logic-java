package io.github.jamsesso.jsonlogic.compiler.gen;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
import static io.github.jamsesso.jsonlogic.utils.MapHelpers.reduceContext;
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
    Object reduceArray_1 = Arrays.<Object>asList(1.0, 2.0, 3.0);
    Object reduceAccumulator_2 = 0.0;
    Object result_0 = reduceAccumulator_2;
    if (ArrayLike.isEligible(reduceArray_1)) {
      Map<String, Object> reduceContext_3 = reduceContext(data, reduceAccumulator_2);
      for (Object reduceItem_4 : new ArrayLike(reduceArray_1)) {
        reduceContext_3.put("current", reduceItem_4);
        final Object var_current_6 = resolveVarChecked(reduceContext_3, "current", null);
        final Object var_accumulator_8 = resolveVarChecked(reduceContext_3, "accumulator", null);
        Object arith_10;
        if (var_current_6 == null || !isNumeric(var_current_6) || var_accumulator_8 == null || !isNumeric(var_accumulator_8)) {
          arith_10 = null;
        } else {
          double _d_7 = toDouble(var_current_6);
          double _d_9 = toDouble(var_accumulator_8);
          arith_10 = (_d_7 + _d_9);
        }
        Object reduceBody_5 = arith_10;
        reduceContext_3.put("accumulator", reduceBody_5);
      }
      result_0 = reduceContext_3.get("accumulator");
    }
    return result_0;
  }
}
