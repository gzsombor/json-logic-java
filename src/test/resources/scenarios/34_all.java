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
    final Object var_items_7 = resolveVarChecked(data, "items", null);
    final Object allArray_1 = var_items_7;
    Boolean allResult_2;
    if (allArray_1 == null || !ArrayLike.isEligible(allArray_1) || ArrayLike.isEmpty(allArray_1)) {
      allResult_2 = Boolean.FALSE;
    } else {
      allResult_2 = Boolean.TRUE;
      final Iterator<Object> allIterator_3 = ArrayLike.iterator(allArray_1);
      while (allIterator_3.hasNext()) {
        final Object allItem_4 = allIterator_3.next();
        if (!filter$0(allItem_4)) {
          allResult_2 = Boolean.FALSE;
          break;
        }
      }
    }
    Object result_0 = allResult_2;
    return result_0;
  }

  private boolean filter$0(Object item) throws JsonLogicEvaluationException {
    final Object var_score_6 = resolveVarChecked(item, "score", null);
    boolean filterBodyResult_5 = (toComparableDouble(var_score_6) >= 10.0);
    return filterBodyResult_5;
  }
}
