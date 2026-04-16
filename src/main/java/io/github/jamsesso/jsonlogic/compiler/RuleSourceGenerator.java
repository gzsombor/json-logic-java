package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Walks a {@link JsonLogicNode} AST and produces a self-contained Java source string
 * for a class that implements {@link CompiledRule}.
 *
 * <p>This is a stateful, single-use instance: create one per rule, call
 * {@link #generate(JsonLogicNode, String)}, then read {@link #getFallbackNodes()} to obtain
 * any AST nodes that were delegated back to the interpreter (for operators that are not
 * natively compiled).
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>Primitives and {@code var} accesses are inlined as Java expressions.</li>
 *   <li>Built-in operators with pure-expression semantics ({@code ==}, {@code ===},
 *       {@code !=}, {@code !==}, numeric comparisons, {@code !}, {@code !!},
 *       {@code cat}) are emitted as Java expressions.</li>
 *   <li>Control-flow operators ({@code if}/{@code ?:}, {@code and}, {@code or}) are emitted
 *       as proper Java {@code if/else} blocks preserving short-circuit semantics.</li>
 *   <li>{@code +} and {@code *} delegate to a {@code mathReduce} helper in the generated
 *       class to preserve MathExpression's array-unwrapping semantics.</li>
 *   <li>All other operators fall back to the tree-walking interpreter via a stored
 *       {@code JsonLogicNode} array injected at construction time.</li>
 * </ul>
 *
 * <h2>Pre-statement separation</h2>
 * <p>Whenever {@link #emitExpression} needs to lift a control-flow sub-expression into a
 * temporary variable, those statements are written to the {@code pre} buffer passed to the
 * method rather than directly to the main output buffer.  {@link #emitStatement} flushes
 * the {@code pre} buffer into {@code out} before writing the assignment line.  This ensures
 * generated statements never appear inside a partially-written expression.
 *
 * <h2>Generated class constructor</h2>
 * <pre>public RuleXxx(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson)</pre>
 */
public final class RuleSourceGenerator {

  /** Package for all generated rule classes. */
  static final String GEN_PACKAGE = "io.github.jamsesso.jsonlogic.compiler.gen";

  /** Nodes that must be evaluated by the fallback interpreter, in insertion order. */
  private final List<JsonLogicNode> fallbackNodes = new ArrayList<>();

  private final AtomicInteger counter = new AtomicInteger(0);

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  public String generate(JsonLogicNode ast, String className) {
    final var body = new StringBuilder();
    final String resultVar = freshVar("result");
    emitStatement(ast, resultVar, body, "data");

    // Use .formatted() only for the static header — body is NOT passed through it,
    // so any % characters in rule expressions are safe.
    final String header = """
        package %s;

        import io.github.jamsesso.jsonlogic.JsonLogic;
        import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
        import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
        import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
        import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
        import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
        import java.util.*;

        public final class %s implements CompiledRule {

          private final JsonLogicEvaluator fallback;
          private final JsonLogicNode[] fallbackNodes;
          private final String ruleJson;

          public %s(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson) {
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
        """.formatted(GEN_PACKAGE, className, className);

    return header
        + body
        + "    return " + resultVar + ";\n"
        + "  }\n"
        + "}\n";
  }

  public List<JsonLogicNode> getFallbackNodes() {
    return fallbackNodes;
  }

  // -------------------------------------------------------------------------
  // Statement-level generation
  //
  // Appends statements to `out` that ultimately assign a value to `targetVar`.
  //
  // KEY INVARIANT: `emitStatement` never leaves a partially-written line in `out`.
  // All expressions are fully evaluated into a pre-buffer before being concatenated
  // onto an assignment line.  This prevents lifted control-flow statements from
  // appearing inside a partially-written assignment.
  // -------------------------------------------------------------------------

  private void emitStatement(JsonLogicNode node, String targetVar, StringBuilder out, String dataExpr) {
    if (node instanceof JsonLogicOperation op) {
      switch (op.getOperator()) {
        case "if":
        case "?:":
          emitIf(op.getArguments(), targetVar, out, dataExpr);
          return;
        case "and":
          emitAnd(op.getArguments(), targetVar, out, dataExpr);
          return;
        case "or":
          emitOr(op.getArguments(), targetVar, out, dataExpr);
          return;
        default:
          break;
      }
    }
    // Pure expression: collect any pre-statements separately, then emit the assignment.
    final var pre = new StringBuilder();
    final String expr = emitExpression(node, pre, dataExpr);
    out.append(pre);
    out.append("    Object ").append(targetVar).append(" = ").append(expr).append(";\n");
  }

  // -------------------------------------------------------------------------
  // Expression-level generation
  //
  // Returns a Java expression string.  Any intermediate statements needed (e.g.
  // for lifted control-flow sub-expressions) are appended to `pre`.
  // The caller must flush `pre` to its main output buffer before writing a line
  // that uses the returned expression.
  // -------------------------------------------------------------------------

  private String emitExpression(JsonLogicNode node, StringBuilder pre, String dataExpr) {
    if (node instanceof JsonLogicNull) {
      return "null";
    }
    if (node instanceof JsonLogicBoolean bool) {
      return bool.getValue() ? "Boolean.TRUE" : "Boolean.FALSE";
    }
    if (node instanceof JsonLogicNumber num) {
      final double value = num.getValue();
      if (Double.isNaN(value)) {
        return "Double.NaN";
      }
      if (value == Double.POSITIVE_INFINITY) {
        return "Double.POSITIVE_INFINITY";
      }
      if (value == Double.NEGATIVE_INFINITY) {
        return "Double.NEGATIVE_INFINITY";
      }
      return "Double.longBitsToDouble(" + Double.doubleToLongBits(value) + "L)";
    }
    if (node instanceof JsonLogicString str) {
      return javaStringLiteral(str.getValue());
    }
    if (node instanceof JsonLogicArray arr) {
      return emitArrayLiteral(arr, pre, dataExpr);
    }
    if (node instanceof JsonLogicVariable var) {
      return emitVariable(var, pre, dataExpr);
    }
    if (node instanceof JsonLogicOperation op) {
      // Control-flow ops need statement context — lift into a temp var
      if (isControlFlow(op.getOperator())) {
        final String tmp = freshVar("ctrl");
        emitStatement(node, tmp, pre, dataExpr);
        return tmp;
      }
      return emitOperation(op, pre, dataExpr);
    }
    throw new IllegalArgumentException("Unsupported AST node: " + node.getClass().getName());
  }

  private static boolean isControlFlow(String op) {
    return op.equals("if") || op.equals("?:") || op.equals("and") || op.equals("or");
  }

  // ---- variable ----

  private String emitVariable(JsonLogicVariable node, StringBuilder pre, String dataExpr) {
    final String keyExpr     = emitExpression(node.getKey(), pre, dataExpr);
    final String defaultExpr = emitExpression(node.getDefaultValue(), pre, dataExpr);
    return "resolveVar(" + dataExpr + ", " + keyExpr + ", " + defaultExpr + ")";
  }

  // ---- array literal ----

  private String emitArrayLiteral(JsonLogicArray node, StringBuilder pre, String dataExpr) {
    if (node.isEmpty()) {
      return "Collections.emptyList()";
    }
    final var sb = new StringBuilder("Arrays.<Object>asList(");
    for (int i = 0; i < node.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(emitExpression(node.get(i), pre, dataExpr));
    }
    return sb.append(")").toString();
  }

  // ---- operations ----

  private String emitOperation(JsonLogicOperation op, StringBuilder pre, String dataExpr) {
    final String operator = op.getOperator();
    final JsonLogicArray args = op.getArguments();
    switch (operator) {
      case "==":  return "looseEq("   + arg(args,0,pre,dataExpr) + ", " + arg(args,1,pre,dataExpr) + ")";
      case "!=":  return "!looseEq("  + arg(args,0,pre,dataExpr) + ", " + arg(args,1,pre,dataExpr) + ")";
      case "===": return "strictEq("  + arg(args,0,pre,dataExpr) + ", " + arg(args,1,pre,dataExpr) + ")";
      case "!==": return "!strictEq(" + arg(args,0,pre,dataExpr) + ", " + arg(args,1,pre,dataExpr) + ")";
      case "!":   return "!JsonLogic.truthy("  + arg(args,0,pre,dataExpr) + ")";
      case "!!":  return  "JsonLogic.truthy("  + arg(args,0,pre,dataExpr) + ")";
      case ">":   return numCmp(">",  args, pre, dataExpr);
      case ">=":  return numCmp(">=", args, pre, dataExpr);
      case "<":   return numCmp("<",  args, pre, dataExpr);
      case "<=":  return numCmp("<=", args, pre, dataExpr);
      // + and * use mathReduce to mirror MathExpression's array-unwrapping semantics
      case "+":   return emitMathReduce("+",  args, pre, dataExpr);
      case "*":   return emitMathReduce("*",  args, pre, dataExpr);
      case "-":   return emitMinus(args, pre, dataExpr);
      case "/":   return emitBinArith("/", args, pre, dataExpr);
      case "%":   return emitBinArith("%", args, pre, dataExpr);
      case "min": return minMax("min", args, pre, dataExpr);
      case "max": return minMax("max", args, pre, dataExpr);
      case "cat": return emitCat(args, pre, dataExpr);
      default:    return emitFallback(op, pre, dataExpr);
    }
  }

  // ---- if / nested else-if ----
  //
  // We MUST use nested if/else rather than flat else-if because the condition variable
  // for branch N is declared AFTER we close branch N-1's body, making it visible only
  // from that point inside the else block — not in a subsequent "} else if" on the same
  // nesting level.
  //
  private void emitIf(JsonLogicArray args, String targetVar, StringBuilder out, String dataExpr) {
    out.append("    Object ").append(targetVar).append(";\n");
    if (args.isEmpty()) {
      out.append("    ").append(targetVar).append(" = null;\n");
      return;
    }
    if (args.size() == 1) {
      final String v = freshVar("ifSingle");
      emitStatement(args.get(0), v, out, dataExpr);
      out.append("    ").append(targetVar).append(" = ").append(v).append(";\n");
      return;
    }
    emitIfChain(args, 0, targetVar, out, dataExpr);
  }

  private void emitIfChain(JsonLogicArray args, int index, String targetVar, StringBuilder out, String dataExpr) {
    if (index + 1 >= args.size()) {
      // No more condition/result pairs
      if (index < args.size()) {
        // Odd trailing arg is the else branch
        final String elseVar = freshVar("ifElse");
        emitStatement(args.get(index), elseVar, out, dataExpr);
        out.append("    ").append(targetVar).append(" = ").append(elseVar).append(";\n");
      } else {
        // Even arg count with no match → null
        out.append("    ").append(targetVar).append(" = null;\n");
      }
      return;
    }

    // Emit condition variable at the current scope level, then open an if block
    final String condVar = freshVar("ifCond");
    emitStatement(args.get(index), condVar, out, dataExpr);
    out.append("    if (JsonLogic.truthy(").append(condVar).append(")) {\n");

    final String consVar = freshVar("ifCons");
    emitStatement(args.get(index + 1), consVar, out, dataExpr);
    out.append("      ").append(targetVar).append(" = ").append(consVar).append(";\n");

    out.append("    } else {\n");
    // The next condition variable is declared inside this else block, so it is in scope
    // for the nested if that follows — correct Java scoping.
    emitIfChain(args, index + 2, targetVar, out, dataExpr);
    out.append("    }\n");
  }

  // ---- and ----
  //
  // Returns the first falsy value, or the last value when all are truthy.
  // Generated pattern (nested ifs, short-circuit):
  //   Object t = null;
  //   Object v0 = <arg0>; t = v0;
  //   if (truthy(t)) {
  //     Object v1 = <arg1>; t = v1;
  //     if (truthy(t)) { ... }
  //   }
  //
  private void emitAnd(JsonLogicArray args, String targetVar, StringBuilder out, String dataExpr) {
    out.append("    Object ").append(targetVar).append(" = null;\n");
    if (!args.isEmpty()) {
      emitAndChain(args, 0, targetVar, out, dataExpr);
    }
  }

  private void emitAndChain(JsonLogicArray args, int idx, String targetVar, StringBuilder out, String dataExpr) {
    final String andVar = freshVar("andV");
    emitStatement(args.get(idx), andVar, out, dataExpr);
    out.append("    ").append(targetVar).append(" = ").append(andVar).append(";\n");
    if (idx + 1 < args.size()) {
      out.append("    if (JsonLogic.truthy(").append(targetVar).append(")) {\n");
      final var inner = new StringBuilder();
      emitAndChain(args, idx + 1, targetVar, inner, dataExpr);
      indentBlock(inner, out);
      out.append("    }\n");
    }
  }

  // ---- or ----
  //
  // Returns the first truthy value, or the last value when all are falsy.
  //
  private void emitOr(JsonLogicArray args, String targetVar, StringBuilder out, String dataExpr) {
    out.append("    Object ").append(targetVar).append(" = null;\n");
    if (!args.isEmpty()) {
      emitOrChain(args, 0, targetVar, out, dataExpr);
    }
  }

  private void emitOrChain(JsonLogicArray args, int idx, String targetVar, StringBuilder out, String dataExpr) {
    final String orVar = freshVar("orV");
    emitStatement(args.get(idx), orVar, out, dataExpr);
    out.append("    ").append(targetVar).append(" = ").append(orVar).append(";\n");
    if (idx + 1 < args.size()) {
      out.append("    if (!JsonLogic.truthy(").append(targetVar).append(")) {\n");
      final var inner = new StringBuilder();
      emitOrChain(args, idx + 1, targetVar, inner, dataExpr);
      indentBlock(inner, out);
      out.append("    }\n");
    }
  }

  /** Appends each non-empty line from {@code inner} to {@code out} with 2 extra spaces. */
  private static void indentBlock(StringBuilder inner, StringBuilder out) {
    for (final String line : inner.toString().split("\n", -1)) {
      if (!line.isEmpty()) {
        out.append("  ").append(line).append("\n");
      }
    }
  }

  // ---- numeric comparisons ----

  private String numCmp(String op, JsonLogicArray args, StringBuilder pre, String dataExpr) {
    if (args.size() >= 3) {
      final String left  = arg(args, 0, pre, dataExpr);
      final String mid   = arg(args, 1, pre, dataExpr);
      final String right = arg(args, 2, pre, dataExpr);
      return "(toDouble(" + left + ") " + op + " toDouble(" + mid + ") "
          + "&& toDouble(" + mid + ") " + op + " toDouble(" + right + "))";
    }
    return "(toDouble(" + arg(args,0,pre,dataExpr) + ") "
        + op + " toDouble(" + arg(args,1,pre,dataExpr) + "))";
  }

  // ---- arithmetic ----

  /**
   * Emits a call to {@code mathReduce} for {@code +} and {@code *}.
   * This mirrors MathExpression's array-unwrapping and null-propagation behaviour.
   */
  private String emitMathReduce(String op, JsonLogicArray args, StringBuilder pre, String dataExpr) {
    if (args.isEmpty()) {
      return "null";
    }
    final var sb = new StringBuilder("mathReduce(").append(javaStringLiteral(op)).append(", Arrays.<Object>asList(");
    for (int i = 0; i < args.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(arg(args, i, pre, dataExpr));
    }
    return sb.append("))").toString();
  }

  private String emitBinArith(String op, JsonLogicArray args, StringBuilder pre, String dataExpr) {
    if (args.isEmpty()) {
      return "null";
    }
    if (args.size() == 1) {
      return "toDouble(" + arg(args, 0, pre, dataExpr) + ")";
    }
    return "(toDouble(" + arg(args,0,pre,dataExpr) + ") " + op + " toDouble(" + arg(args,1,pre,dataExpr) + "))";
  }

  private String emitMinus(JsonLogicArray args, StringBuilder pre, String dataExpr) {
    if (args.isEmpty()) {
      return "null";
    }
    if (args.size() == 1) {
      return "(-toDouble(" + arg(args,0,pre,dataExpr) + "))";
    }
    return "(toDouble(" + arg(args,0,pre,dataExpr) + ") - toDouble(" + arg(args,1,pre,dataExpr) + "))";
  }

  private String minMax(String fn, JsonLogicArray args, StringBuilder pre, String dataExpr) {
    if (args.isEmpty()) {
      return "null";
    }
    String acc = "toDouble(" + arg(args, 0, pre, dataExpr) + ")";
    for (int i = 1; i < args.size(); i++) {
      acc = "Math." + fn + "(" + acc + ", toDouble(" + arg(args, i, pre, dataExpr) + "))";
    }
    return acc;
  }

  // ---- cat ----

  private String emitCat(JsonLogicArray args, StringBuilder pre, String dataExpr) {
    if (args.isEmpty()) {
      return "\"\"";
    }
    if (args.size() == 1) {
      return "catStr(" + arg(args, 0, pre, dataExpr) + ")";
    }
    final var sb = new StringBuilder("(catStr(").append(arg(args, 0, pre, dataExpr)).append(")");
    for (int i = 1; i < args.size(); i++) {
      sb.append(" + catStr(").append(arg(args, i, pre, dataExpr)).append(")");
    }
    return sb.append(")").toString();
  }

  // ---- fallback to interpreter ----

  private String emitFallback(JsonLogicOperation node, StringBuilder pre, String dataExpr) {
    final int idx = fallbackNodes.size();
    fallbackNodes.add(node);
    pre.append("    // fallback operator: ").append(node.getOperator()).append("\n");
    return "fallback.evaluate(fallbackNodes[" + idx + "], " + dataExpr + ")";
  }

  // ---- utilities ----

  private String arg(JsonLogicArray args, int index, StringBuilder pre, String dataExpr) {
    return index < args.size() ? emitExpression(args.get(index), pre, dataExpr) : "null";
  }

  private String freshVar(String hint) {
    return hint + "_" + counter.getAndIncrement();
  }

  // -------------------------------------------------------------------------
  // Utility
  // -------------------------------------------------------------------------

  public static String javaStringLiteral(String s) {
    if (s == null) {
      return "null";
    }
    final var sb = new StringBuilder("\"");
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      switch (ch) {
        case '\\': sb.append("\\\\"); break;
        case '"':  sb.append("\\\""); break;
        case '\n': sb.append("\\n");  break;
        case '\r': sb.append("\\r");  break;
        case '\t': sb.append("\\t");  break;
        default:
          if (ch < 0x20 || ch > 0x7e) {
            sb.append(String.format("\\u%04x", (int) ch));
          } else {
            sb.append(ch);
          }
      }
    }
    return sb.append("\"").toString();
  }
}
