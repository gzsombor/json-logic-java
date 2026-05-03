package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.ast.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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
   * <h2>Security</h2>
   * <p>All user-supplied data that appears in generated Java source code (operator names,
   * var paths, string literals from the AST) is passed through {@link #javaStringLiteral}
   * before being embedded in string literal positions.  Operator names are only ever used
   * as string literal values (never as raw Java identifiers), so no additional sanitisation
   * is needed beyond the escaping already applied by {@code javaStringLiteral}.
   *
   * <h2>Variable hoisting</h2>
   * <p>Simple {@code var} accesses (string key, no default) are resolved once at the top of
   * the generated method and cached in {@code final Object} locals, regardless of which
   * conditional branches actually use them.  This is a deliberate trade-off: it avoids
   * re-resolving the same path on every evaluation but means the variable is always resolved
   * even if the branch that references it is never taken.  This diverges from the
   * tree-walking interpreter, which resolves variables lazily as execution reaches them.
   * Rules that rely on a missing-var default (e.g. {@code {"var":"x"}}) are still correct
   * because {@link io.github.jamsesso.jsonlogic.compiler.RuleHelpers#resolveVarChecked}
   * returns {@code null} for absent keys rather than throwing.
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

  private int counter = 0;
  /**
   * Static {@code Set} field declarations accumulated during generation, emitted once per
   * class between the instance fields and the constructor.  Each entry is a complete field
   * declaration line, e.g. {@code "  private static final Set<Object> SET_1 = Set.of(...);\n"}.
   */
  private final StringBuilder staticFields = new StringBuilder();

  /**
   * Cache from a canonical set-element string (e.g. {@code "\"a\",\"b\""}) to the already-
   * declared field name, so identical haystacks share a single {@code Set} constant.
   */
  private final Map<String, String> setCache = new LinkedHashMap<>();

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  public String generate(JsonLogicNode ast, String className) {
    final var body = new StringBuilder();
    final String resultVar = freshVar("result");
    // Path starts empty — JsonLogic.apply() will prepend "$" to any exception.
    emitStatement(ast, resultVar, body, "data", "");
    final boolean omitReturn = ast instanceof JsonLogicOperation
        && isControlFlow(((JsonLogicOperation) ast).getOperator())
        && ((JsonLogicOperation) ast).getArguments().isEmpty()
        && throwsOnEmptyArgs(((JsonLogicOperation) ast).getOperator());

    // Helper to determine if an operator throws when given empty arguments.
    // "and"/"or" throw; "if" sets result to null (doesn't throw).
    // So we only omit the return for and/or (which throw), not for if (which doesn't throw).

    final String header = "package " + GEN_PACKAGE + ";\n"
        + "\n"
        + "import io.github.jamsesso.jsonlogic.JsonLogic;\n"
        + "import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;\n"
        + "import io.github.jamsesso.jsonlogic.compiler.CompiledRule;\n"
        + "import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;\n"
        + "import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;\n"
        + "import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;\n"
        + "import java.util.*;\n"
        + "\n"
        + "public final class " + className + " implements CompiledRule {\n"
        + "\n"
        + "  private final JsonLogicEvaluator fallback;\n"
        + "  private final JsonLogicNode[] fallbackNodes;\n"
        + "  private final String ruleJson;\n";

    // Static set constants (may be empty) go after the instance fields.
    final String staticFieldsStr = staticFields.length() > 0
        ? "\n" + staticFields
        : "";

    final String constructor = "\n"
        + "  public " + className + "(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson) {\n"
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
        + "  public Object apply(Object data) throws JsonLogicEvaluationException {\n";

    return header
        + staticFieldsStr
        + constructor
        + varPreamble
        + body
        + (omitReturn ? "" : "    return " + resultVar + ";\n")
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
        case "missing":
        case "missing_some":
          emitMissing(op, targetVar, out, dataExpr, path);
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
    return "if".equals(op) || "?:".equals(op) || "and".equals(op) || "or".equals(op);
  }

  /** Returns true if the operator throws an exception when given empty arguments. */
  private static boolean throwsOnEmptyArgs(String op) {
    return "and".equals(op) || "or".equals(op);
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
        // Only emit as boolean when exactly 1 arg; otherwise generated code throws directly.
        return argc == 1;
      case ">":  case ">=": case "<":  case "<=":
        // Only emit as boolean when 2 or 3 args; otherwise generated code throws directly.
        return argc >= 2 && argc <= 3;
      // == != === !== require exactly 2 args; only treat them as boolean when they
      // will actually be compiled inline.
      case "==": case "!=": case "===": case "!==":
        return argc == 2;
      case "in":
        return argc == 2 && isAllPrimitiveLiteralArray(op.getArguments().get(1));
      default:
        return false;
    }
  }

  /** Returns true when the node is a {@link JsonLogicArray} whose every element is a primitive literal. */
  private static boolean isAllPrimitiveLiteralArray(JsonLogicNode node) {
    if (!(node instanceof JsonLogicArray)) {
      return false;
    }
    for (JsonLogicNode element : (JsonLogicArray) node) {
      if (!(element instanceof JsonLogicPrimitive)) {
        return false;
      }
    }
    return true;
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
    // opPath is always passed through javaStringLiteral() before being emitted into generated
    // source, so arbitrary operator strings cannot inject content into the generated Java.
    final String opPath = path + "." + operator;
    switch (operator) {
      // equality: require exactly 2 args, otherwise emit the exception directly.
      case "==":
        if (args.size() != 2) {
          return emitFailure("equality expressions expect exactly 2 arguments", opPath);
        }
        return "looseEq(" + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      case "!=":
        if (args.size() != 2) {
          return emitFailure("equality expressions expect exactly 2 arguments", opPath);
        }
        return "!looseEq(" + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      case "===":
        if (args.size() != 2) {
          return emitFailure("equality expressions expect exactly 2 arguments", opPath);
        }
        return "strictEq(" + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      case "!==":
        if (args.size() != 2) {
          return emitFailure("equality expressions expect exactly 2 arguments", opPath);
        }
        return "!strictEq(" + arg(args, 0, pre, dataExpr, opPath) + ", " + arg(args, 1, pre, dataExpr, opPath) + ")";
      // ! and !! compiled only for exactly 1 arg; otherwise emit the exception directly.
      case "!":
        if (args.size() != 1) {
          return emitFallback(op, pre, dataExpr, path);
        }
        return "!JsonLogic.truthy(" + arg(args, 0, pre, dataExpr, opPath) + ")";
      case "!!":
        if (args.size() != 1) {
          return emitFallback(op, pre, dataExpr, path);
        }
        return "JsonLogic.truthy(" + arg(args, 0, pre, dataExpr, opPath) + ")";
      // comparisons require at least 2 and at most 3 args; otherwise emit the exception directly.
      case ">":
        if (args.size() < 2) {
          return emitFailure("'>' requires at least 2 arguments", opPath);
        } else if (args.size() > 3) {
          return emitFallback(op, pre, dataExpr, path);
        }
        return numCmp(">", args, pre, dataExpr, opPath);
      case ">=":
        if (args.size() < 2) {
          return emitFailure("'>=' requires at least 2 arguments", opPath);
        } else if (args.size() > 3) {
          return emitFallback(op, pre, dataExpr, path);
        }
        return numCmp(">=", args, pre, dataExpr, opPath);
      case "<":
        if (args.size() < 2) {
          return emitFailure("'<' requires at least 2 arguments", opPath);
        } else if (args.size() > 3) {
          return emitFallback(op, pre, dataExpr, path);
        }
        return numCmp("<", args, pre, dataExpr, opPath);
      case "<=":
        if (args.size() < 2) {
          return emitFailure("'<=' requires at least 2 arguments", opPath);
        } else if (args.size() > 3) {
          return emitFallback(op, pre, dataExpr, path);
        }
        return numCmp("<=", args, pre, dataExpr, opPath);
      case "+": case "*": case "-": case "/": case "%": case "min": case "max":
        return emitArith(op, pre, dataExpr, path);
      case "cat": return emitCat(args, pre, dataExpr, opPath);
      case "substr": return emitSubstr(op, args, pre, dataExpr, opPath, path);
      case "in":  return emitIn(op, args, pre, dataExpr, opPath, path);
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

    // Emit condition.  When the condition expression is provably boolean we can inline it
    // directly into the `if` test without an intermediate local or a truthy() call.
    // When it requires statements (control-flow sub-expression) or is non-boolean, we
    // still materialise a local so the pre-statements land before the if line.
    final JsonLogicNode condNode = args.get(index);
    final String condPath = path + "[" + index + "]";
    final String ifTest;
    final boolean condIsBoolean = isBooleanExpression(condNode);
    final boolean condIsControlFlow = condNode instanceof JsonLogicOperation
        && isControlFlow(((JsonLogicOperation) condNode).getOperator());
    if (condIsBoolean && !condIsControlFlow) {
      // Pure boolean expression: inline directly — no local, no truthy()
      final var pre = new StringBuilder();
      final String expr = emitExpression(condNode, pre, dataExpr, condPath);
      out.append(pre);
      ifTest = expr;
    } else {
      // Non-boolean or control-flow: materialise a local
      final String condVar = freshVar("ifCond");
      emitStatement(condNode, condVar, out, dataExpr, condPath);
      ifTest = isBooleanExpression(condNode)
          ? condVar
          : "JsonLogic.truthy(" + condVar + ")";
    }
    out.append("    if (").append(ifTest).append(") {\n");

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
      out.append("    throw new JsonLogicEvaluationException(\"and operator expects at least 1 argument\", ")
         .append(javaStringLiteral(path)).append(");\n");
      return;
    }
    out.append("    Object ").append(targetVar).append(" = null;\n");
    emitAndChain(args, 0, targetVar, out, dataExpr, path);
  }

  private void emitAndChain(JsonLogicArray args, int idx, String targetVar, StringBuilder out,
                            String dataExpr, String path) {
    final JsonLogicNode argNode = args.get(idx);
    final String andVar = freshVar("andV");
    emitStatement(argNode, andVar, out, dataExpr, path + "[" + idx + "]");
    out.append("    ").append(targetVar).append(" = ").append(andVar).append(";\n");
    if (idx + 1 < args.size()) {
      // When andVar is a primitive boolean we can use it directly in the if test;
      // otherwise fall back to JsonLogic.truthy() on the Object targetVar.
      final String test = isBooleanExpression(argNode)
          ? andVar
          : "JsonLogic.truthy(" + targetVar + ")";
      out.append("    if (").append(test).append(") {\n");
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
      out.append("    throw new JsonLogicEvaluationException(\"or operator expects at least 1 argument\", ")
         .append(javaStringLiteral(path)).append(");\n");
      return;
    }
    out.append("    Object ").append(targetVar).append(" = null;\n");
    emitOrChain(args, 0, targetVar, out, dataExpr, path);
  }

  private void emitOrChain(JsonLogicArray args, int idx, String targetVar, StringBuilder out,
                           String dataExpr, String path) {
    final JsonLogicNode argNode = args.get(idx);
    final String orVar = freshVar("orV");
    emitStatement(argNode, orVar, out, dataExpr, path + "[" + idx + "]");
    out.append("    ").append(targetVar).append(" = ").append(orVar).append(";\n");
    if (idx + 1 < args.size()) {
      // When orVar is a primitive boolean we can negate it directly;
      // otherwise fall back to !JsonLogic.truthy() on the Object targetVar.
      final String test = isBooleanExpression(argNode)
          ? "!" + orVar
          : "!JsonLogic.truthy(" + targetVar + ")";
      out.append("    if (").append(test).append(") {\n");
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
      // Hoist mid into a local so it is evaluated only once (it may be a fallback call).
      final String midVar = freshVar("mid");
      pre.append("    final double ").append(midVar).append(" = ").append(mid).append(";\n");
      return "(" + left + " " + op + " " + midVar + " && " + midVar + " " + op + " " + right + ")";
    }
    return "(" + numericArg(args, 0, pre, dataExpr, path) + " " + op + " "
               + numericArg(args, 1, pre, dataExpr, path) + ")";
  }

  // ---- arithmetic ----

  // ---- arithmetic ----
  //
  // Strategy: when the entire arithmetic sub-tree contains no JsonLogicArray literals and no
  // single-non-literal-arg fallback for +/*, we can emit a null guard over all Object-typed
  // leaves and then evaluate the expression with pure double arithmetic.
  //
  // Generated pattern for e.g. {"+":[{"*":[{"var":"a"},{"var":"a"}]},{"*":[{"var":"b"},{"var":"b"}]}]}:
  //
  //   Object result_0;
  //   if (var_a_1 == null || !isNumeric(var_a_1) || var_b_2 == null || !isNumeric(var_b_2)) {
  //     result_0 = null;
  //   } else {
  //     double _a = toDouble(var_a_1), _b = toDouble(var_b_2);
  //     result_0 = (_a * _a) + (_b * _b);
  //   }
  //
  // Falls back to mathReduce/scalar-helpers when the tree contains array literals or a single
  // non-literal arg for +/*.

  private static boolean isArithOp(String op) {
    switch (op) {
      case "+": case "*": case "-": case "/": case "%": case "min": case "max":
        return true;
      default:
        return false;
    }
  }

  /**
   * Returns true when the entire sub-tree rooted at {@code node} can be evaluated as pure
   * double arithmetic — no JsonLogicArray literals, no single-non-literal-arg +/* that
   * MathExpression might unwrap at runtime.
   */
  private static boolean isArithInlineable(JsonLogicNode node) {
    if (node instanceof JsonLogicNumber) {
      return true;
    }
    if (!(node instanceof JsonLogicOperation)) {
      // var, string, boolean, null, fallback-op: all are leaf Object values — inlineable as leaves
      return true;
    }
    final JsonLogicOperation op = (JsonLogicOperation) node;
    final JsonLogicArray args = op.getArguments();
    switch (op.getOperator()) {
      case "+": case "*":
        if (args.isEmpty()) {
          return true;
        }
        for (int i = 0; i < args.size(); i++) {
          if (args.get(i) instanceof JsonLogicArray) {
            return false;
          }
        }
        // Single non-literal arg could resolve to an array at runtime
        if (args.size() == 1 && !(args.get(0) instanceof JsonLogicNumber)) {
          return false;
        }
        for (int i = 0; i < args.size(); i++) {
          if (!isArithInlineable(args.get(i))) {
            return false;
          }
        }
        return true;
      case "-":
        if (args.isEmpty()) {
          return true;
        }
        for (int i = 0; i < Math.min(args.size(), 2); i++) {
          if (!isArithInlineable(args.get(i))) {
            return false;
          }
        }
        return true;
      case "/": case "%":
        if (args.size() < 2) {
          return true;
        }
        return isArithInlineable(args.get(0)) && isArithInlineable(args.get(1));
      case "min": case "max":
        if (args.isEmpty()) {
          return true;
        }
        for (int i = 0; i < args.size(); i++) {
          if (!isArithInlineable(args.get(i))) {
            return false;
          }
        }
        return true;
      default:
        // Non-arithmetic op: it's a leaf — inlineable as an Object value
        return true;
    }
  }

  /**
   * Collects, in evaluation order, all Object-typed leaf nodes within an inlineable arithmetic
   * sub-tree.  Number literals are excluded (they are statically known to be non-null and
   * numeric).
   *
   * <p>Uses an {@link LinkedHashMap} keyed by AST node so that the leaf expression is
   * emitted exactly once (in {@code collectArithLeaves}) and reused by {@link #emitDoubleExpr}
   * — preventing fallback nodes from being registered twice with different indices.
   *
   * @param node   the AST node to walk
   * @param leaves identity map from leaf node → [emittedExpr, doubleLocalName] (output)
   * @param pre    pre-statement buffer (may receive statements for sub-expressions)
   * @param dataExpr data variable expression
   * @param path   current JSON path
   */
  private void collectArithLeaves(JsonLogicNode node, LinkedHashMap<JsonLogicNode, String[]> leaves,
                                   StringBuilder pre, String dataExpr, String path) {
    if (node instanceof JsonLogicNumber) {
      return; // literals are always numeric — no null check needed
    }
    if (node instanceof JsonLogicOperation) {
      final JsonLogicOperation op = (JsonLogicOperation) node;
      final JsonLogicArray args = op.getArguments();
      // path is the *positional* path of this node (e.g. "$.+[1]").
      // opBase appends this op's own name so children get full paths (e.g. "$.+[1].-[0]").
      final String opBase = path + "." + op.getOperator();
      switch (op.getOperator()) {
        case "+": case "*":
          for (int i = 0; i < args.size(); i++) {
            collectArithLeaves(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]");
          }
          return;
        case "-":
          for (int i = 0; i < Math.min(args.size(), 2); i++) {
            collectArithLeaves(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]");
          }
          return;
        case "/": case "%":
          if (args.size() >= 2) {
            collectArithLeaves(args.get(0), leaves, pre, dataExpr, opBase + "[0]");
            collectArithLeaves(args.get(1), leaves, pre, dataExpr, opBase + "[1]");
          }
          return;
        case "min": case "max":
          for (int i = 0; i < args.size(); i++) {
            collectArithLeaves(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]");
          }
          return;
        default:
          break; // non-arithmetic op falls through to leaf handling below
      }
    }
    // Leaf: emit the Object expression and register a double local. Two AST nodes whose emitted
    // expression string is identical (e.g. two {"var":"a"} nodes) share one double local and
    // one null-check, eliminating duplicates. We key by node identity but check the emitted
    // expression against existing entries so repeated vars are deduplicated correctly.
    if (leaves.containsKey(node)) {
      return;
    }
    final String expr = emitExpression(node, pre, dataExpr, path);
    // Check whether a different node already produced the same expression string.
    for (final String[] existing : leaves.values()) {
      if (existing[0].equals(expr)) {
        // Reuse the existing double local — don't allocate a new one.
        leaves.put(node, existing);
        return;
      }
    }
    leaves.put(node, new String[]{expr, freshVar("_d")});
  }

  /**
   * Returns a pure {@code double}-valued Java expression for an inlineable arithmetic sub-tree,
   * using the pre-declared double locals from {@code leaves} for Object-typed leaf nodes.
   */
  private String emitDoubleExpr(JsonLogicNode node, LinkedHashMap<JsonLogicNode, String[]> leaves,
                                 StringBuilder pre, String dataExpr, String path) {
    if (node instanceof JsonLogicNumber) {
      final double v = ((JsonLogicNumber) node).getValue();
      return Double.toString(v);
    }
    if (node instanceof JsonLogicOperation) {
      final JsonLogicOperation op = (JsonLogicOperation) node;
      final JsonLogicArray args = op.getArguments();
      switch (op.getOperator()) {
        case "+": {
          final String opBase = path + ".+";
          if (args.size() == 1) {
            return emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]");
          }
          final var sb = new StringBuilder("(");
          for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
              sb.append(" + ");
            }
            sb.append(emitDoubleExpr(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]"));
          }
          return sb.append(")").toString();
        }
        case "*": {
          final String opBase = path + ".*";
          if (args.size() == 1) {
            return emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]");
          }
          final var sb = new StringBuilder("(");
          for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
              sb.append(" * ");
            }
            sb.append(emitDoubleExpr(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]"));
          }
          return sb.append(")").toString();
        }
        case "-": {
          final String opBase = path + ".-";
          if (args.isEmpty()) {
            return "0.0";
          }
          if (args.size() == 1) {
            return "(-" + emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]") + ")";
          }
          return "(" + emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]")
              + " - " + emitDoubleExpr(args.get(1), leaves, pre, dataExpr, opBase + "[1]") + ")";
        }
        case "/": {
          final String opBase = path + "./";
          if (args.size() < 2) {
            return "0.0";
          }
          return "(" + emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]")
              + " / " + emitDoubleExpr(args.get(1), leaves, pre, dataExpr, opBase + "[1]") + ")";
        }
        case "%": {
          final String opBase = path + ".%";
          if (args.size() < 2) {
            return "0.0";
          }
          return "(" + emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]")
              + " % " + emitDoubleExpr(args.get(1), leaves, pre, dataExpr, opBase + "[1]") + ")";
        }
        case "min": {
          final String opBase = path + ".min";
          if (args.isEmpty()) {
            return "0.0";
          }
          String acc = emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]");
          for (int i = 1; i < args.size(); i++) {
            acc = "Math.min(" + acc + ", " + emitDoubleExpr(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]") + ")";
          }
          return acc;
        }
        case "max": {
          final String opBase = path + ".max";
          if (args.isEmpty()) {
            return "0.0";
          }
          String acc = emitDoubleExpr(args.get(0), leaves, pre, dataExpr, opBase + "[0]");
          for (int i = 1; i < args.size(); i++) {
            acc = "Math.max(" + acc + ", " + emitDoubleExpr(args.get(i), leaves, pre, dataExpr, opBase + "[" + i + "]") + ")";
          }
          return acc;
        }
        default:
          break; // non-arithmetic op: look up its double local below
      }
    }
    // Leaf: look up the double local registered by collectArithLeaves by node identity.
    // Duplicate AST nodes (e.g. two {"var":"a"} nodes) share the same String[] entry,
    // so they resolve to the same double local without re-calling emitExpression.
    final String[] entry = leaves.get(node);
    if (entry == null) {
      throw new IllegalStateException(
          "emitDoubleExpr: no double local for leaf node: " + node.getClass().getSimpleName());
    }
    return entry[1];
  }

  /**
   * Main entry point for all arithmetic operators.  When the sub-tree is fully inlineable,
   * emits a null-guard block with double locals and pure double arithmetic into {@code pre},
   * then returns the result local name.  Falls back to the old helper approach otherwise.
   */
  private String emitArith(JsonLogicOperation op, StringBuilder pre, String dataExpr, String path) {
    final String operator = op.getOperator();
    final JsonLogicArray args = op.getArguments();

    if (!isArithOp(operator)) {
      throw new IllegalStateException("emitArith called for non-arithmetic operator: " + operator);
    }

    // Fast-path for empty / too-few args: return null, matching MathExpression behaviour.
    if (args.isEmpty()) {
      return "null";
    }
    if (("/".equals(operator) || "%".equals(operator)) && args.size() == 1) {
      return "null";
    }

    if (!isArithInlineable(op)) {
      // Cannot inline: fall back to the old paths (mathReduce / scalar helpers)
      return emitArithFallback(op, pre, dataExpr, path);
    }

    // Collect all Object-typed leaf nodes and assign each a double local name. Node identity
    // is the key; duplicate AST nodes with the same emitted expression share one String[] entry
    // (and thus one double local and one null-check guard), eliminating redundant checks.
    final var leaves = new LinkedHashMap<JsonLogicNode, String[]>();
    collectArithLeaves(op, leaves, pre, dataExpr, path);

    // If there are no Object leaves at all (all literals), emit pure double directly.
    if (leaves.isEmpty()) {
      return emitDoubleExpr(op, leaves, pre, dataExpr, path);
    }

    // Emit the null / isNumeric guard and double locals into pre, then the result into a fresh local.
    final String resultLocal = freshVar("arith");

    // Build null-check condition: leafExpr == null || !isNumeric(leafExpr) for each distinct leaf.
    // Entries sharing the same String[] instance (deduplicated vars) are emitted only once.
    final var cond = new StringBuilder();
    final var seen = new IdentityHashMap<String[], Boolean>();
    for (final String[] entry : leaves.values()) {
      if (seen.put(entry, Boolean.TRUE) != null) {
        continue; // already emitted for this shared entry
      }
      if (cond.length() > 0) {
        cond.append(" || ");
      }
      cond.append(entry[0]).append(" == null || !isNumeric(").append(entry[0]).append(")");
    }

    // Emit double local declarations (once per distinct entry)
    final var doubleDecls = new StringBuilder();
    final var seenDecls = new IdentityHashMap<String[], Boolean>();
    for (final String[] entry : leaves.values()) {
      if (seenDecls.put(entry, Boolean.TRUE) != null) {
        continue;
      }
      doubleDecls.append("      double ").append(entry[1])
          .append(" = toDouble(").append(entry[0]).append(");\n");
    }

    final String doubleExpr = emitDoubleExpr(op, leaves, pre, dataExpr, path);

    pre.append("    Object ").append(resultLocal).append(";\n");
    pre.append("    if (").append(cond).append(") {\n");
    pre.append("      ").append(resultLocal).append(" = null;\n");
    pre.append("    } else {\n");
    pre.append(doubleDecls);
    pre.append("      ").append(resultLocal).append(" = ").append(doubleExpr).append(";\n");
    pre.append("    }\n");

    return resultLocal;
  }

  /** Fallback for non-inlineable arithmetic (array args, single-non-literal +/* arg). */
  private String emitArithFallback(JsonLogicOperation op, StringBuilder pre,
                                   String dataExpr, String path) {
    switch (op.getOperator()) {
      case "+": return emitMathReduce("+", op.getArguments(), pre, dataExpr, path);
      case "*": return emitMathReduce("*", op.getArguments(), pre, dataExpr, path);
      default:
        throw new IllegalStateException(
            "emitArithFallback called for non-reducible operator: " + op.getOperator());
    }
  }

  /**
   * Emits a call to {@code mathReduce} for {@code +} and {@code *}.
   * Used as a fallback when any argument is a {@link JsonLogicArray} literal.
   */
  private String emitMathReduce(String op, JsonLogicArray args, StringBuilder pre,
                                String dataExpr, String path) {
    final var sb = new StringBuilder("mathReduce(").append(javaStringLiteral(op))
        .append(", Arrays.<Object>asList(");
    for (int i = 0; i < args.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(arg(args, i, pre, dataExpr, path));
    }
    return sb.append("))").toString();
  }

  /** Returns a Java {@code double} expression for numeric comparisons (>, <, etc.). */
  private String numericArg(JsonLogicArray args, int index, StringBuilder pre,
                            String dataExpr, String path) {
    if (index >= args.size()) {
      return "0.0";
    }
    final JsonLogicNode node = args.get(index);
    if (node instanceof JsonLogicNull) {
      return "0.0";
    }
    if (node instanceof JsonLogicBoolean) {
      return ((JsonLogicBoolean) node).getValue() ? "1.0" : "0.0";
    }
    if (node instanceof JsonLogicNumber) {
      final double v = ((JsonLogicNumber) node).getValue();
      if (!Double.isNaN(v) && !Double.isInfinite(v)) {
        return Double.toString(v);
      }
    }
    if (node instanceof JsonLogicString) {
      try {
        return Double.toString(Double.parseDouble(((JsonLogicString) node).getValue()));
      } catch (NumberFormatException ignored) {
        return "Double.NaN";
      }
    }
    return "toComparableDouble(" + emitExpression(node, pre, dataExpr, path + "[" + index + "]") + ")";
  }

  // ---- cat ----

  private String emitCat(JsonLogicArray args, StringBuilder pre, String dataExpr, String path) {
    if (args.isEmpty()) {
      return "\"\"";
    }
    if (args.size() == 1) {
      return emitCatArg(args.get(0), pre, dataExpr, path + "[0]");
    }
    final var sb = new StringBuilder("(");
    for (int i = 0; i < args.size(); i++) {
      if (i > 0) {
        sb.append(" + ");
      }
      sb.append(emitCatArg(args.get(i), pre, dataExpr, path + "[" + i + "]"));
    }
    return sb.append(")").toString();
  }

  /**
   * Emits a {@code String}-typed expression for a single {@code cat} argument.
   * String and number literals are converted at code-generation time so the generated
   * code contains plain string literals rather than {@code catStr(...)} calls.
   * Runtime-typed values (variables, nested operations) are wrapped with {@code catStr}.
   */
  private String emitCatArg(JsonLogicNode node, StringBuilder pre, String dataExpr, String path) {
    if (node instanceof JsonLogicString) {
      return javaStringLiteral(((JsonLogicString) node).getValue());
    }
    if (node instanceof JsonLogicNumber) {
      final double v = ((JsonLogicNumber) node).getValue();
      if (v == Math.floor(v) && !Double.isInfinite(v) && !Double.isNaN(v)) {
        return javaStringLiteral(String.valueOf((long) v));
      }
      return javaStringLiteral(Double.toString(v));
    }
    if (node instanceof JsonLogicNull) {
      return "\"null\"";
    }
    if (node instanceof JsonLogicBoolean) {
      return ((JsonLogicBoolean) node).getValue() ? "\"true\"" : "\"false\"";
    }
    // Variable, nested operation, or array — value is only known at runtime.
    return "catStr(" + emitExpression(node, pre, dataExpr, path) + ")";
  }

  // ---- substr ----

  private String emitSubstr(JsonLogicOperation op, JsonLogicArray args,
                             StringBuilder pre, String dataExpr, String opPath, String path) {
    if (args.size() < 2 || args.size() > 3) {
      return emitFailure("substr expects 2 or 3 arguments", opPath);
    }
    final String strExpr   = arg(args, 0, pre, dataExpr, opPath);
    final String startExpr = arg(args, 1, pre, dataExpr, opPath);
    final String lenExpr   = args.size() == 3 ? arg(args, 2, pre, dataExpr, opPath) : "null";
    return "substr(" + strExpr + ", " + startExpr + ", " + lenExpr
        + ", " + javaStringLiteral(opPath) + ")";
  }

  // ---- in ----
  //
  // Compiled only when the haystack (arg[1]) is a literal array of primitive values.
  // The haystack is emitted as a private static final Set<Object> field so it is
  // allocated once per class, not on every invocation.  Identical haystacks reuse
  // the same field via setCache.

  private String emitIn(JsonLogicOperation op, JsonLogicArray args, StringBuilder pre,
                        String dataExpr, String opPath, String parentPath) {
    // Require exactly 2 args and a primitive-only literal array as haystack.
    if (args.size() != 2) {
      return emitFallback(op, pre, dataExpr, parentPath);
    }
    if (!isAllPrimitiveLiteralArray(args.get(1))) {
      return emitFallback(op, pre, dataExpr, parentPath);
    }

    final JsonLogicArray haystack = (JsonLogicArray) args.get(1);

    // Build the canonical key and the Set.of(...) element list in one pass.
    final StringBuilder elements = new StringBuilder();
    for (int i = 0; i < haystack.size(); i++) {
      if (i > 0) elements.append(", ");
      elements.append(emitExpression(haystack.get(i), pre, dataExpr, opPath + "[1][" + i + "]"));
    }
    final String elementsKey = elements.toString();

    // Reuse an existing field if we have already seen this exact haystack.
    String fieldName = setCache.get(elementsKey);
    if (fieldName == null) {
      fieldName = freshVar("SET");
      setCache.put(elementsKey, fieldName);
      staticFields.append("  private static final Set<Object> ").append(fieldName)
          .append(" = new HashSet<>(Arrays.asList(").append(elementsKey).append("));\n");
    }

    final String needle = emitExpression(args.get(0), pre, dataExpr, opPath + "[0]");
    return fieldName + ".contains(" + needle + ")";
  }

  // ---- missing / missing_some ----
  //
  // missing:  returns keys from arguments that are missing from data.
  // missing_some: returns empty list if enough keys are present, otherwise missing keys.
  // Fall back to interpreter for both (complex logic, Set operations).
  private void emitMissing(JsonLogicOperation op, String targetVar, StringBuilder out,
                             String dataExpr, String path) {
    out.append("    // missing/missing_some: fall back to interpreter\n");
    final int idx = fallbackNodes.size();
    fallbackNodes.add(op);
    out.append("    Object ").append(targetVar).append(" = fallback.evaluate(fallbackNodes[")
        .append(idx).append("], ").append(dataExpr).append(", ")
        .append(javaStringLiteral(path)).append(");\n");
  }

  // ---- fallback to interpreter ----

  private String emitFallback(JsonLogicOperation node, StringBuilder pre,
                              String dataExpr, String path) {
    final int idx = fallbackNodes.size();
    fallbackNodes.add(node);
    return "fallback.evaluate(fallbackNodes[" + idx + "], " + dataExpr
        + ", " + javaStringLiteral(path) + ")";
  }

  private String emitFailure(String message, String path) {
    return "fail(" + javaStringLiteral(message) + ", " + javaStringLiteral(path) + ")";
  }

  // ---- utilities ----

  private String arg(JsonLogicArray args, int index, StringBuilder pre,
                     String dataExpr, String path) {
    return index < args.size()
        ? emitExpression(args.get(index), pre, dataExpr, path + "[" + index + "]")
        : "null";
  }

  private String freshVar(String hint) {
    return hint + "_" + counter++;
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
