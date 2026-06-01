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
    final Object someArray_1 = var_items_7;
    Boolean someResult_2;
    if (someArray_1 == null) {
      someResult_2 = Boolean.FALSE;
    } else if (!ArrayLike.isEligible(someArray_1)) {
      someResult_2 = fail("first argument to some must be a valid array", ".some[0]");
    } else {
      someResult_2 = Boolean.FALSE;
      final Iterator<Object> someIterator_3 = new ArrayLike(someArray_1).iterator();
      while (someIterator_3.hasNext()) {
        final Object someItem_4 = someIterator_3.next();
        if (JsonLogic.truthy(collectionBody$0(someItem_4))) {
          someResult_2 = Boolean.TRUE;
          break;
        }
      }
    }
    Object result_0 = someResult_2;
    return result_0;
  }

  private Object collectionBody$0(Object item) throws JsonLogicEvaluationException {
    final Object var_score_6 = resolveVarChecked(item, "score", null);
    boolean collectionBodyResult_5 = (toComparableDouble(var_score_6) >= 10.0);
    return collectionBodyResult_5;
  }
}
