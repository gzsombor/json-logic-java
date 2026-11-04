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
    final Object var_a_3 = resolveVarChecked(data, "a", null);
    final Object var_b_5 = resolveVarChecked(data, "b", null);
    final List<Object> mergeResult_1 = new ArrayList<>();
    final Object mergeArg_2 = var_a_3;
    if (ArrayLike.isEligible(mergeArg_2)) { mergeResult_1.addAll(ArrayLike.toList(mergeArg_2)); } else { mergeResult_1.add(mergeArg_2); }
    final Object mergeArg_4 = var_b_5;
    if (ArrayLike.isEligible(mergeArg_4)) { mergeResult_1.addAll(ArrayLike.toList(mergeArg_4)); } else { mergeResult_1.add(mergeArg_4); }
    mergeResult_1.add(99.0);
    Object result_0 = mergeResult_1;
    return result_0;
  }
}
