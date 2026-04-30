package io.github.jamsesso.jsonlogic.compiler.gen;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
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
    final Object var_price_1 = resolveVarChecked(data, "price", null);
    final Object var_discount_3 = resolveVarChecked(data, "discount", null);
    Object arith_5;
    if (var_price_1 == null || !isNumeric(var_price_1) || var_discount_3 == null || !isNumeric(var_discount_3)) {
      arith_5 = null;
    } else {
      double _d_2 = toDouble(var_price_1);
      double _d_4 = toDouble(var_discount_3);
      arith_5 = (_d_2 * (1.0 - (_d_4 / 100.0)));
    }
    Object result_0 = arith_5;
    return result_0;
  }
}

