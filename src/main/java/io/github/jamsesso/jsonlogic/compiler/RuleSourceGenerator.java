package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <h2>JSON path threading</h2>
 * <p>Every emit method receives a {@code path} string that is the JSON path of the current
 * node (e.g. {@code "$"}, {@code "$.if[2].filter"}).  When a sub-expression falls back to
 * the interpreter, the generated code passes this path so that any
 * {@link io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException} retains the
 * correct path prefix — matching the tree-walking interpreter's behaviour exactly.
 *
 * <h2>Generated class constructor</h2>
 * <pre>public RuleXxx(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson)</pre>
 */
public final class RuleSourceGenerator {

  /** Package for all generated rule classes. */
  static final String GEN_PACKAGE = "io.github.jamsesso.jsonlogic.compiler.gen";

  /** Nodes that must be evaluated by the fallback interpreter, in insertion order. */
  private final List<JsonLogicNode> fallbackNodes = new ArrayList<>();

  /**
   * Cache of simple string-key vars seen during generation.
   * Key: the variable name string (e.g. "a" or "user.age").
   * Value: the Java local variable name that holds the resolved value.
   * Variables are emitted as {@code final Object} declarations before the first use.
   */
  private final Map<String, String> varCache = new LinkedHashMap<>();

  /**
   * Statements declaring the cached var locals, accumulated during generation and
   * flushed into the method body as a preamble.
   */
  private final StringBuilder varPreamble = new StringBuilder();

  private final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Set to {@code true} when the top-level statement is an unconditional {@code throw}
   * (e.g. {@code and []} or {@code or []}), so {@code generate()} can omit the
   * unreachable {@code return} that would otherwise produce a javac warning.
   */
  private boolean bodyAlwaysThrows = false;

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  public String generate(JsonLogicNode ast, String className) {
    final var body = new StringBuilder();
    final String resultVar = freshVar("result");
    // Path starts empty — JsonLogic.apply() will prepend "$" to any exception.
    emitStatement(ast, resultVar, body, "data", "");

    // Use String.format() only for the static header - body is NOT passed through it,
    // so any % characters in rule expressions are safe.
    final String header = String.format(
        "package %s;\n"
        + "\n"
        + "import io.github.jamsesso.jsonlogic.JsonLogic;\n"
        + "import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;\n"
        + "import io.github.jamsesso.jsonlogic.compiler.CompiledRule;\n"
        + "import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;\n"
        + "import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;\n"
        + "import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;\n"
        + "import java.util.*;\n"
        + "\n"
        + "public final class %s implements CompiledRule {\n"
        + "\n"
        + "  private final JsonLogicEvaluator fallback;\n"
        + "  private final JsonLogicNode[] fallbackNodes;\n"
        + "  private final String ruleJson;\n"
        + "\n"
        + "  public %s(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson) {\n"
        + "    this.fallback = fallback;\n"
        + "    this.fallbackNodes = fallbackNodes;\n"
        + "    this.ruleJson = ruleJson;\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public String toString() {\n"
        + "    return \"CompiledRule(\" + ruleJson + \")\";\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public Object apply(Object data) throws JsonLogicEvaluationException {\n",
        GEN_PACKAGE, className, className);

    return header
        + varPreamble
        + body
        + (bodyAlwaysThrows ? "" : "    return " + resultVar + ";\n")
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

  private void emitStatement(JsonLogicNode node, String targetVar, StringBuilder out,
                             String dataExpr, String path) {
    if (node instanceof JsonLogicOperation) {
      final JsonLogicOperation op = (JsonLogicOperation) node;
      switch (op.getOperator()) {
        case "if":
        case "?:":
          emitIf(op.getArguments(), targetVar, out, dataExpr, path + ".if");
          return;
        case "and":
          emitAnd(op.getArguments(), targetVar, out, dataExpr, path + ".and");
          return;
        case "or":
          emitOr(op.getArguments(), targetVar, out, dataExpr, path + ".or");
          return;
        default:
          break;
      }
    }
    // Pure expression: collect any pre-statements separately, then emit the assignment.
    final var pre = new StringBuilder();
    final String expr = emitExpression(node, pre, dataExpr, path);
    out.append(pre);
    final String javaType = isBooleanExpression(node) ? "boolean" : "Object";
    out.append("    ").append(javaType).append(" ").append(targetVar).append(" = ").append(expr).append(";\n");
  }

  // -------------------------------------------------------------------------
  // Expression-level generation
  //
  // Returns a Java expression string.  Any intermediate statements needed (e.g.
  // for lifted control-flow sub-expressions) are appended to `pre`.
  // The caller must flush `pre` to its main output buffer before writing a line
  // that uses the returned expression.
  // -------------------------------------------------------------------------

  private String emitExpression(JsonLogicNode node, StringBuilder pre, String dataExpr, String path) {
    if (node instanceof JsonLogicNull) {
      return "null";
    }
    if (node instanceof JsonLogicBoolean) {
      final JsonLogicBoolean bool = (JsonLogicBoolean) node;
      return bool.getValue() ? "Boolean.TRUE" : "Boolean.FALSE";
    }
    if (node instanceof JsonLogicNumber) {
      final JsonLogicNumber num = (JsonLogicNumber) node;
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
      return Double.toString(value);
    }
    if (node instanceof JsonLogicString) {
      return javaStringLiteral(((JsonLogicString) node).getValue());
    }
    if (node instanceof JsonLogicArray) {
      return emitArrayLiteral((JsonLogicArray) node, pre, dataExpr, path);
    }
    if (node instanceof JsonLogicVariable) {
      return emitVariable((JsonLogicVariable) node, pre, dataExpr, path);
    }
    if (node instanceof JsonLogicOperation) {
      final JsonLogicOperation op = (JsonLogicOperation) node;
      if (isControlFlow(op.getOperator())) {
        final String tmp = freshVar("ctrl");
        emitStatement(node, tmp, pre, dataExpr, path);
        return tmp;
      }
      return emitOperation(op, pre, dataExpr, path);
    }
    throw new IllegalArgumentException("Unsupported AST node: " + node.getClass().getName());
  }

  private static boolean isControlFlow(String op) {
    return op.equals("if") || op.equals("?:") || op.equals("and") || op.equals("or");
  }

  /** Returns true when a node is guaranteed to produce a primitive {@code boolean}. */
  private static boolean isBooleanExpression(JsonLogicNode node) {
    if (!(node instanceof JsonLogicOperation)) {
      return false;
    }
    final JsonLogicOperation op = (JsonLogicOperation) node;
    final int argc = op.getArguments().size();
    switch (op.getOperator()) {
      case "!":  case "!!":
        // Only emit as boolean when exactly 1 arg; otherwise falls back to interpreter
        return argc == 1;
      case ">":  case ">=": case "<":  case "<=":
        // Only emit as boolean when 2 or 3 args; <2 or >3 falls back to interpreter
        return argc >= 2 && argc <= 3;
      // == != === !== require exactly 2 args; with wrong arg count they fall back to
      // the interpreter (which returns Object), so only treat them as boolean when
      // they will actually be compiled inline.
      case "==": case "!=": case "===": case "!==":
        return argc == 2;
      default:
        return false;
    }
  }

  // ---- variable ----

  private String emitVariable(JsonLogicVariable node, StringBuilder pre, String dataExpr, String path) {
    // Optimisation: if the key is a plain string literal and the default is null,
    // resolve the variable once into a final local and reuse it on subsequent references.
    if (node.getKey() instanceof JsonLogicString && node.getDefaultValue() instanceof JsonLogicNull) {
      final String varName = ((JsonLogicString) node.getKey()).getValue();
      final String cached = varCache.get(varName);
      if (cached != null) {
        return cached;
      }
      // First occurrence: emit the declaration into the preamble.
      // Use resolveVarChecked so that any JsonLogicEvaluationException gets ".var" prepended,
      // matching the path the tree-walking interpreter produces for var errors.
      final String localName = freshVar("var_" + sanitize(varName));
      varCache.put(varName, localName);
      varPreamble.append("    final Object ").append(localName)
          .append(" = resolveVarChecked(data, ").append(javaStringLiteral(varName)).append(", null);\n");
      return localName;
    }
    // General case: key is dynamic or a non-null default is present.
    // Use the fallback evaluator so that invalid key types throw with the right path.
    // Pass the parent path; the evaluator will prepend ".var" to exceptions itself.
    final int idx = fallbackNodes.size();
    fallbackNodes.add(node);
    pre.append("    // fallback var: ").append(node.getKey().getClass().getSimpleName()).append("\n");
    return "fallback.evaluate(fallbackNodes[" + idx + "], " + dataExpr + ", " + javaStringLiteral(path) + ")";
  }

  /**
   * Strips characters that are not valid in a Java identifier from a var name so it can
   * be used as part of a local variable name.  The sanitised form is only used for
   * readability; uniqueness is guaranteed by the numeric suffix added by {@link #freshVar}.
   */
  private static String sanitize(String varName) {
    return varName.replaceAll("[^A-Za-z0-9_]", "_");
  }

  // ---- array literal ----

  private String emitArrayLiteral(JsonLogicArray node, StringBuilder pre, String dataExpr, String path) {
    if (node.isEmpty()) {
      return "Collections.emptyList()";
    }
    final var sb = new StringBuilder("Arrays.<Object>asList(");
    for (int i = 0; i < node.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(emitExpression(node.get(i), pre, dataExpr, path + "[" + i + "]"));
    }
    return sb.append(")").toString();
  }

  // ---- operations ----

  private String emitOperation(JsonLogicOperation op, StringBuilder pre, String dataExpr, String path) {
    final String operator = op.getOperator();
    final JsonLogicArray args = op.getArguments();
    // opPath is the path of this operation node; used when passing paths to sub-expressions.
    // For fallback calls we pass `path` (parent), not `opPath`, because the fallback evaluator
    // will prepend ".<operator>" itself when it catches exceptions from the sub-expression.
    final String opPath = path + "." + operator;
    switch (operator) {
      // equality: require exactly 2 args, otherwise fall back so the interpreter can throw
      case "==":  if (args.size() != 2) return emitFallback(op, pre, dataExpr, path);
                  return "looseEq("   + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      case "!=":  if (args.size() != 2) return emitFallback(op, pre, dataExpr, path);
                  return "!looseEq("  + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      case "===": if (args.size() != 2) return emitFallback(op, pre, dataExpr, path);
                  return "strictEq("  + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      case "!==": if (args.size() != 2) return emitFallback(op, pre, dataExpr, path);
                  return "!strictEq(" + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      // ! and !! compiled only for exactly 1 arg; otherwise fall back
      case "!":   if (args.size() != 1) return emitFallback(op, pre, dataExpr, path);
                  return "!JsonLogic.truthy("  + arg(args, 0, pre, dataExpr, opPath) + ")";
      case "!!":  if (args.size() != 1) return emitFallback(op, pre, dataExpr, path);
                  return  "JsonLogic.truthy("  + arg(args, 0, pre, dataExpr, opPath) + ")";
      // comparisons require at least 2 and at most 3 args; fall back otherwise so the
      // interpreter evaluates all args (and throws for errors in extra args)
      case ">":   if (args.size() < 2 || args.size() > 3) return emitFallback(op, pre, dataExpr, path);
                  return numCmp(">",  args, pre, dataExpr, opPath);
      case ">=":  if (args.size() < 2 || args.size() > 3) return emitFallback(op, pre, dataExpr, path);
                  return numCmp(">=", args, pre, dataExpr, opPath);
      case "<":   if (args.size() < 2 || args.size() > 3) return emitFallback(op, pre, dataExpr, path);
                  return numCmp("<",  args, pre, dataExpr, opPath);
      case "<=":  if (args.size() < 2 || args.size() > 3) return emitFallback(op, pre, dataExpr, path);
                  return numCmp("<=", args, pre, dataExpr, opPath);
      // + and * use mathReduce to mirror MathExpression's array-unwrapping semantics
      case "+":   return emitMathReduce("+",  args, pre, dataExpr, opPath);
      case "*":   return emitMathReduce("*",  args, pre, dataExpr, opPath);
      case "-":   return emitMinus(args, pre, dataExpr, opPath);
      // / and % with < 2 args return null (matches MathExpression interpreter behaviour)
      case "/":   return emitBinArith("/", args, pre, dataExpr, opPath);
      case "%":   return emitBinArith("%", args, pre, dataExpr, opPath);
      case "min": return minMax("min", args, pre, dataExpr, opPath);
      case "max": return minMax("max", args, pre, dataExpr, opPath);
      case "cat": return emitCat(args, pre, dataExpr, opPath);
      default:    return emitFallback(op, pre, dataExpr, path);
    }
  }

  // ---- if / nested else-if ----
  //
  // We MUST use nested if/else rather than flat else-if because the condition variable
  // for branch N is declared AFTER we close branch N-1's body, making it visible only
  // from that point inside the else block - not in a subsequent "} else if" on the same
  // nesting level.
  //
  private void emitIf(JsonLogicArray args, String targetVar, StringBuilder out,
                      String dataExpr, String path) {
    out.append("    Object ").append(targetVar).append(";\n");
    if (args.isEmpty()) {
      out.append("    ").append(targetVar).append(" = null;\n");
      return;
    }
    if (args.size() == 1) {
      final String v = freshVar("ifSingle");
      emitStatement(args.get(0), v, out, dataExpr, path + "[0]");
      out.append("    ").append(targetVar).append(" = ").append(v).append(";\n");
      return;
    }
    emitIfChain(args, 0, targetVar, out, dataExpr, path);
  }

  private void emitIfChain(JsonLogicArray args, int index, String targetVar, StringBuilder out,
                           String dataExpr, String path) {
    if (index + 1 >= args.size()) {
      // No more condition/result pairs
      if (index < args.size()) {
        // Odd trailing arg is the else branch
        final String elseVar = freshVar("ifElse");
        emitStatement(args.get(index), elseVar, out, dataExpr, path + "[" + index + "]");
        out.append("    ").append(targetVar).append(" = ").append(elseVar).append(";\n");
      } else {
        // Even arg count with no match → null
        out.append("    ").append(targetVar).append(" = null;\n");
      }
      return;
    }

    // Emit condition variable at the current scope level, then open an if block
    final String condVar = freshVar("ifCond");
    emitStatement(args.get(index), condVar, out, dataExpr, path + "[" + index + "]");
    out.append("    if (JsonLogic.truthy(").append(condVar).append(")) {\n");

    final String consVar = freshVar("ifCons");
    emitStatement(args.get(index + 1), consVar, out, dataExpr, path + "[" + (index + 1) + "]");
    out.append("      ").append(targetVar).append(" = ").append(consVar).append(";\n");

    out.append("    } else {\n");
    // The next condition variable is declared inside this else block, so it is in scope
    // for the nested if that follows - correct Java scoping.
    emitIfChain(args, index + 2, targetVar, out, dataExpr, path);
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
  private void emitAnd(JsonLogicArray args, String targetVar, StringBuilder out,
                       String dataExpr, String path) {
    if (args.isEmpty()) {
      bodyAlwaysThrows = true;
      out.append("    throw new JsonLogicEvaluationException(\"and operator expects at least 1 argument\", ")
         .append(javaStringLiteral(path)).append(");\n");
      return;
    }
    out.append("    Object ").append(targetVar).append(" = null;\n");
    emitAndChain(args, 0, targetVar, out, dataExpr, path);
  }

  private void emitAndChain(JsonLogicArray args, int idx, String targetVar, StringBuilder out,
                            String dataExpr, String path) {
    final String andVar = freshVar("andV");
    emitStatement(args.get(idx), andVar, out, dataExpr, path + "[" + idx + "]");
    out.append("    ").append(targetVar).append(" = ").append(andVar).append(";\n");
    if (idx + 1 < args.size()) {
      out.append("    if (JsonLogic.truthy(").append(targetVar).append(")) {\n");
      final var inner = new StringBuilder();
      emitAndChain(args, idx + 1, targetVar, inner, dataExpr, path);
      indentBlock(inner, out);
      out.append("    }\n");
    }
  }

  // ---- or ----
  //
  // Returns the first truthy value, or the last value when all are falsy.
  //
  private void emitOr(JsonLogicArray args, String targetVar, StringBuilder out,
                      String dataExpr, String path) {
    if (args.isEmpty()) {
      bodyAlwaysThrows = true;
      out.append("    throw new JsonLogicEvaluationException(\"or operator expects at least 1 argument\", ")
         .append(javaStringLiteral(path)).append(");\n");
      return;
    }
    out.append("    Object ").append(targetVar).append(" = null;\n");
    emitOrChain(args, 0, targetVar, out, dataExpr, path);
  }

  private void emitOrChain(JsonLogicArray args, int idx, String targetVar, StringBuilder out,
                           String dataExpr, String path) {
    final String orVar = freshVar("orV");
    emitStatement(args.get(idx), orVar, out, dataExpr, path + "[" + idx + "]");
    out.append("    ").append(targetVar).append(" = ").append(orVar).append(";\n");
    if (idx + 1 < args.size()) {
      out.append("    if (!JsonLogic.truthy(").append(targetVar).append(")) {\n");
      final var inner = new StringBuilder();
      emitOrChain(args, idx + 1, targetVar, inner, dataExpr, path);
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

  private String numCmp(String op, JsonLogicArray args, StringBuilder pre,
                        String dataExpr, String path) {
    if (args.size() >= 3) {
      final String left  = numericArg(args, 0, pre, dataExpr, path);
      final String mid   = numericArg(args, 1, pre, dataExpr, path);
      final String right = numericArg(args, 2, pre, dataExpr, path);
      return "(" + left + " " + op + " " + mid + " && " + mid + " " + op + " " + right + ")";
    }
    return "(" + numericArg(args, 0, pre, dataExpr, path) + " " + op + " "
               + numericArg(args, 1, pre, dataExpr, path) + ")";
  }

  // ---- arithmetic ----

  /**
   * Emits a call to {@code mathReduce} for {@code +} and {@code *}.
   * This mirrors MathExpression's array-unwrapping and null-propagation behaviour.
   */
  private String emitMathReduce(String op, JsonLogicArray args, StringBuilder pre,
                                String dataExpr, String path) {
    if (args.isEmpty()) {
      return "null";
    }
    final var sb = new StringBuilder("mathReduce(").append(javaStringLiteral(op))
        .append(", Arrays.<Object>asList(");
    for (int i = 0; i < args.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(arg(args, i, pre, dataExpr, path));
    }
    return sb.append("))").toString();
  }

  private String emitBinArith(String op, JsonLogicArray args, StringBuilder pre,
                              String dataExpr, String path) {
    if (args.isEmpty() || args.size() == 1) {
      // / and % with fewer than 2 args return null (matches MathExpression interpreter behaviour)
      return "null";
    }
    return "(" + numericArg(args, 0, pre, dataExpr, path) + " "
               + op + " "
               + numericArg(args, 1, pre, dataExpr, path) + ")";
  }

  private String emitMinus(JsonLogicArray args, StringBuilder pre, String dataExpr, String path) {
    if (args.isEmpty()) {
      return "null";
    }
    if (args.size() == 1) {
      return "(-" + numericArg(args, 0, pre, dataExpr, path) + ")";
    }
    return "(" + numericArg(args, 0, pre, dataExpr, path) + " - "
               + numericArg(args, 1, pre, dataExpr, path) + ")";
  }

  private String minMax(String fn, JsonLogicArray args, StringBuilder pre,
                        String dataExpr, String path) {
    if (args.isEmpty()) {
      return "null";
    }
    String acc = numericArg(args, 0, pre, dataExpr, path);
    for (int i = 1; i < args.size(); i++) {
      acc = "Math." + fn + "(" + acc + ", " + numericArg(args, i, pre, dataExpr, path) + ")";
    }
    return acc;
  }

  /**
   * Returns a Java {@code double} expression for an argument.
   * If the argument is a number literal, emits it directly (e.g. {@code 10.0}).
   * Otherwise wraps the Object expression in {@code toDouble(...)}.
   */
  private String numericArg(JsonLogicArray args, int index, StringBuilder pre,
                            String dataExpr, String path) {
    if (index >= args.size()) {
      return "toDouble(null)";
    }
    final JsonLogicNode node = args.get(index);
    if (node instanceof JsonLogicNumber) {
      final double v = ((JsonLogicNumber) node).getValue();
      if (!Double.isNaN(v) && !Double.isInfinite(v)) {
        return Double.toString(v);
      }
    }
    return "toDouble(" + emitExpression(node, pre, dataExpr, path + "[" + index + "]") + ")";
  }

  // ---- cat ----

  private String emitCat(JsonLogicArray args, StringBuilder pre, String dataExpr, String path) {
    if (args.isEmpty()) {
      return "\"\"";
    }
    if (args.size() == 1) {
      return "catStr(" + arg(args, 0, pre, dataExpr, path) + ")";
    }
    final var sb = new StringBuilder("(catStr(").append(arg(args, 0, pre, dataExpr, path)).append(")");
    for (int i = 1; i < args.size(); i++) {
      sb.append(" + catStr(").append(arg(args, i, pre, dataExpr, path)).append(")");
    }
    return sb.append(")").toString();
  }

  // ---- fallback to interpreter ----

  private String emitFallback(JsonLogicOperation node, StringBuilder pre,
                              String dataExpr, String path) {
    final int idx = fallbackNodes.size();
    fallbackNodes.add(node);
    pre.append("    // fallback operator: ").append(node.getOperator()).append("\n");
    return "fallback.evaluate(fallbackNodes[" + idx + "], " + dataExpr
        + ", " + javaStringLiteral(path) + ")";
  }

  // ---- utilities ----

  private String arg(JsonLogicArray args, int index, StringBuilder pre,
                     String dataExpr, String path) {
    return index < args.size()
        ? emitExpression(args.get(index), pre, dataExpr, path + "[" + index + "]")
        : "null";
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
