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
    final Object var_items_6 = resolveVarChecked(data, "items", null);
    Object filterArray_1 = var_items_6;
    final List<Object> filterResult_2 = new ArrayList<>();
    if (!ArrayLike.isEligible(filterArray_1)) {
      fail("first argument to filter must be a valid array", ".filter[0]");
    } else {
      for (Object filterItem_3 : ArrayLike.iterable(filterArray_1)) {
        if (filter$0(filterItem_3)) {
          filterResult_2.add(filterItem_3);
        }
      }
    }
    Object result_0 = filterResult_2;
    return result_0;
  }

  private boolean filter$0(Object item) throws JsonLogicEvaluationException {
    final Object var_score_5 = resolveVarChecked(item, "score", null);
    boolean filterBodyResult_4 = (toComparableDouble(var_score_5) >= 10.0);
    return filterBodyResult_4;
  }
}
