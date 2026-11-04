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
    final Object noneArray_1 = var_items_7;
    Boolean noneResult_2;
    if (noneArray_1 == null) {
      noneResult_2 = Boolean.TRUE;
    } else if (!ArrayLike.isEligible(noneArray_1)) {
      noneResult_2 = fail("first argument to none must be a valid array", ".none[0]");
    } else {
      noneResult_2 = Boolean.TRUE;
      final Iterator<Object> noneIterator_3 = ArrayLike.iterator(noneArray_1);
      while (noneIterator_3.hasNext()) {
        final Object noneItem_4 = noneIterator_3.next();
        if (filter$0(noneItem_4)) {
          noneResult_2 = Boolean.FALSE;
          break;
        }
      }
    }
    Object result_0 = noneResult_2;
    return result_0;
  }

  private boolean filter$0(Object item) throws JsonLogicEvaluationException {
    final Object var_score_6 = resolveVarChecked(item, "score", null);
    boolean filterBodyResult_5 = (toComparableDouble(var_score_6) >= 10.0);
    return filterBodyResult_5;
  }
}
