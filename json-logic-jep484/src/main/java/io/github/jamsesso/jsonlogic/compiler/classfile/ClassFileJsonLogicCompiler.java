package io.github.jamsesso.jsonlogic.compiler.classfile;

import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_boolean;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;
import static java.lang.constant.ConstantDescs.ofCallsiteBootstrap;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import io.github.jamsesso.jsonlogic.ast.JsonLogicBoolean;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNull;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNumber;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicString;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilationException;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerImplementation;
import io.github.jamsesso.jsonlogic.compiler.RuleHelpers;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClassFileJsonLogicCompiler implements JsonLogicCompilerImplementation {
  private static final Logger LOG = Logger.getLogger(ClassFileJsonLogicCompiler.class.getName());
  private static final String GENERATED_PACKAGE = "io.github.jamsesso.jsonlogic.compiler.classfile.gen";
  private static final ClassDesc CD_CALL_SITE = ClassDesc.of("java.lang.invoke.CallSite");
  private static final ClassDesc CD_BOOLEAN = ClassDesc.of(Boolean.class.getName());
  private static final ClassDesc CD_COMPILED_RULE = ClassDesc.of(CompiledRule.class.getName());
  private static final ClassDesc CD_DOUBLE = ClassDesc.of(Double.class.getName());
  private static final ClassDesc CD_EVALUATION_EXCEPTION = ClassDesc.of(JsonLogicEvaluationException.class.getName());
  private static final ClassDesc CD_JSON_LOGIC = ClassDesc.of(JsonLogic.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_EVALUATOR = ClassDesc.of(JsonLogicEvaluator.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_NODE = ClassDesc.of(JsonLogicNode.class.getName());
  private static final ClassDesc CD_LOOKUP = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");
  private static final ClassDesc CD_METHOD_TYPE = ClassDesc.of("java.lang.invoke.MethodType");
  private static final ClassDesc CD_RULE_HELPERS = ClassDesc.of(RuleHelpers.class.getName());
  private static final ClassDesc CD_STRING_CONCAT_FACTORY = ClassDesc.of("java.lang.invoke.StringConcatFactory");
  private static final MethodTypeDesc MTD_STRING_CONCAT_BOOTSTRAP = MethodTypeDesc.of(
      CD_CALL_SITE, CD_LOOKUP, CD_String, CD_METHOD_TYPE, CD_String);

  private final JsonLogicEvaluator fallbackEvaluator;
  private final boolean fallbackEnabled;
  private final boolean strictMode;

  public ClassFileJsonLogicCompiler(JsonLogicEvaluator fallbackEvaluator, boolean strictMode) {
    this(fallbackEvaluator, strictMode, true);
  }

  public ClassFileJsonLogicCompiler(
      JsonLogicEvaluator fallbackEvaluator, boolean strictMode, boolean fallbackEnabled) {
    this.fallbackEvaluator = fallbackEvaluator;
    this.strictMode = strictMode;
    this.fallbackEnabled = fallbackEnabled;
  }

  @Override
  public boolean isStrictMode() {
    return strictMode;
  }

  @Override
  public CompiledRule compile(String ruleJson, JsonLogicNode ast) throws JsonLogicCompilationException {
    if (!fallbackEnabled && !isSupported(ast)) {
      throw new JsonLogicCompilationException(
          "JEP 484 compiler does not yet support rule without fallback: " + ruleJson);
    }

    final String className = classNameFor(ruleJson);
    final String qualifiedName = GENERATED_PACKAGE + "." + className;
    final ClassDesc generatedClass = ClassDesc.of(GENERATED_PACKAGE, className);

    try {
      final byte[] classBytes = generateClass(generatedClass, ast);
      final var loader = new ByteArrayClassLoader(
          Thread.currentThread().getContextClassLoader(), Map.of(qualifiedName, classBytes));
      final Class<?> clazz = loader.loadClass(qualifiedName);
      final Constructor<?> constructor = clazz.getConstructor(
          JsonLogicEvaluator.class, JsonLogicNode.class, String.class);
      return (CompiledRule) constructor.newInstance(fallbackEvaluator, ast, ruleJson);
    } catch (InvocationTargetException e) {
      return handleFailure(ruleJson, ast, e.getCause());
    } catch (Exception e) {
      return handleFailure(ruleJson, ast, e);
    }
  }

  private byte[] generateClass(ClassDesc generatedClass, JsonLogicNode ast) {
    return ClassFile.of().build(generatedClass, classBuilder -> classBuilder
        .withFlags(Modifier.PUBLIC | Modifier.FINAL)
        .withInterfaceSymbols(CD_COMPILED_RULE)
        .withField("fallback", CD_JSON_LOGIC_EVALUATOR, Modifier.PRIVATE | Modifier.FINAL)
        .withField("ast", CD_JSON_LOGIC_NODE, Modifier.PRIVATE | Modifier.FINAL)
        .withField("ruleJson", CD_String, Modifier.PRIVATE | Modifier.FINAL)
        .withMethodBody(
            INIT_NAME,
            MethodTypeDesc.ofDescriptor("(Lio/github/jamsesso/jsonlogic/evaluator/JsonLogicEvaluator;"
                + "Lio/github/jamsesso/jsonlogic/ast/JsonLogicNode;Ljava/lang/String;)V"),
            Modifier.PUBLIC,
            codeBuilder -> codeBuilder
                .aload(0)
                .invokespecial(CD_Object, INIT_NAME, MTD_void)
                .aload(0)
                .aload(1)
                .putfield(generatedClass, "fallback", CD_JSON_LOGIC_EVALUATOR)
                .aload(0)
                .aload(2)
                .putfield(generatedClass, "ast", CD_JSON_LOGIC_NODE)
                .aload(0)
                .aload(3)
                .putfield(generatedClass, "ruleJson", CD_String)
                .return_())
        .withMethod(
            "apply",
            MethodTypeDesc.of(CD_Object, CD_Object),
            Modifier.PUBLIC,
            methodBuilder -> methodBuilder
                .with(ExceptionsAttribute.ofSymbols(CD_EVALUATION_EXCEPTION))
                .withCode(codeBuilder -> emitApplyBody(codeBuilder, generatedClass, ast)))
        .withMethodBody(
            "toString",
            MethodTypeDesc.of(CD_String),
            Modifier.PUBLIC,
            codeBuilder -> codeBuilder
                .aload(0)
                .getfield(generatedClass, "ruleJson", CD_String)
                .invokedynamic(DynamicCallSiteDesc.of(
                    ofCallsiteBootstrap(
                        CD_STRING_CONCAT_FACTORY,
                        "makeConcatWithConstants",
                        CD_CALL_SITE,
                        CD_String),
                    "makeConcatWithConstants",
                    MethodTypeDesc.of(CD_String, CD_String),
                    "CompiledRule(\u0001)"))
                .areturn()));
  }

  private void emitApplyBody(CodeBuilder codeBuilder, ClassDesc generatedClass, JsonLogicNode ast) {
    if (emitSupportedNode(codeBuilder, ast)) {
      codeBuilder.areturn();
      return;
    }

    if (!fallbackEnabled) {
      throw new IllegalStateException("Unsupported JEP 484 node reached code generation");
    }

    codeBuilder
        .aload(0)
        .getfield(generatedClass, "fallback", CD_JSON_LOGIC_EVALUATOR)
        .aload(0)
        .getfield(generatedClass, "ast", CD_JSON_LOGIC_NODE)
        .aload(1)
        .invokevirtual(
            CD_JSON_LOGIC_EVALUATOR,
            "evaluate",
            MethodTypeDesc.of(CD_Object, CD_JSON_LOGIC_NODE, CD_Object))
        .areturn();
  }

  private boolean emitSupportedNode(CodeBuilder codeBuilder, JsonLogicNode ast) {
    if (ast instanceof JsonLogicNull) {
      codeBuilder.aconst_null();
      return true;
    }
    if (ast instanceof JsonLogicBoolean) {
      final JsonLogicBoolean bool = (JsonLogicBoolean) ast;
      codeBuilder.getstatic(CD_BOOLEAN, bool.getValue() ? "TRUE" : "FALSE", CD_BOOLEAN);
      return true;
    }
    if (ast instanceof JsonLogicNumber) {
      final JsonLogicNumber number = (JsonLogicNumber) ast;
      codeBuilder
          .ldc(number.getValue())
          .invokestatic(CD_DOUBLE, "valueOf", MethodTypeDesc.of(CD_DOUBLE, ClassDesc.ofDescriptor("D")));
      return true;
    }
    if (ast instanceof JsonLogicString) {
      final JsonLogicString string = (JsonLogicString) ast;
      codeBuilder.ldc(string.getValue());
      return true;
    }
    if (ast instanceof JsonLogicVariable) {
      return emitVariable(codeBuilder, (JsonLogicVariable) ast);
    }
    if (ast instanceof JsonLogicOperation) {
      return emitOperation(codeBuilder, (JsonLogicOperation) ast);
    }
    return false;
  }

  private boolean emitVariable(CodeBuilder codeBuilder, JsonLogicVariable variable) {
    if (!(variable.getKey() instanceof JsonLogicString) || !(variable.getDefaultValue() instanceof JsonLogicNull)) {
      return false;
    }
    final JsonLogicString key = (JsonLogicString) variable.getKey();
    codeBuilder
        .aload(1)
        .ldc(key.getValue())
        .aconst_null()
        .invokestatic(
            CD_RULE_HELPERS,
            "resolveVarChecked",
            MethodTypeDesc.of(CD_Object, CD_Object, CD_String, CD_Object));
    return true;
  }

  private boolean emitOperation(CodeBuilder codeBuilder, JsonLogicOperation operation) {
    final JsonLogicArray args = operation.getArguments();
    switch (operation.getOperator()) {
      case "==":
      case "!=":
      case "===":
      case "!==":
        return emitEquality(codeBuilder, operation.getOperator(), args);
      case "!":
      case "!!":
        return emitNot(codeBuilder, operation.getOperator(), args);
      case ">":
      case ">=":
      case "<":
      case "<=":
        return emitComparison(codeBuilder, operation.getOperator(), args);
      case "+":
      case "*":
      case "-":
      case "/":
      case "%":
        return emitMath(codeBuilder, operation.getOperator(), args);
      default:
        return false;
    }
  }

  private boolean emitEquality(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (args.size() != 2 || !isSupported(args.get(0)) || !isSupported(args.get(1))) {
      return false;
    }
    emitSupportedNode(codeBuilder, args.get(0));
    emitSupportedNode(codeBuilder, args.get(1));
    codeBuilder.invokestatic(
        CD_RULE_HELPERS,
        "===".equals(operator) || "!==".equals(operator) ? "strictEq" : "looseEq",
        MethodTypeDesc.of(CD_boolean, CD_Object, CD_Object));
    if ("!=".equals(operator) || "!==".equals(operator)) {
      emitBooleanNot(codeBuilder);
    }
    boxBoolean(codeBuilder);
    return true;
  }

  private boolean emitComparison(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if ((args.size() != 2 && args.size() != 3) || !allComparable(args)) {
      return false;
    }
    emitComparePair(codeBuilder, operator, args.get(0), args.get(1));
    if (args.size() == 3) {
      emitComparePair(codeBuilder, operator, args.get(1), args.get(2));
      codeBuilder.iand();
    }
    boxBoolean(codeBuilder);
    return true;
  }

  private void emitComparePair(CodeBuilder codeBuilder, String operator, JsonLogicNode left, JsonLogicNode right) {
    emitDouble(codeBuilder, left);
    emitDouble(codeBuilder, right);
    codeBuilder.dcmpg();
    final Label trueLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();
    switch (operator) {
      case ">":
        codeBuilder.ifgt(trueLabel);
        break;
      case ">=":
        codeBuilder.ifge(trueLabel);
        break;
      case "<":
        codeBuilder.iflt(trueLabel);
        break;
      case "<=":
        codeBuilder.ifle(trueLabel);
        break;
      default:
        throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
    }
    codeBuilder
        .iconst_0()
        .goto_(endLabel)
        .labelBinding(trueLabel)
        .iconst_1()
        .labelBinding(endLabel);
  }

  private boolean emitMath(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (!allMathNumeric(args)) {
      return false;
    }
    if ("-".equals(operator) && args.size() == 1) {
      emitDouble(codeBuilder, args.get(0));
      codeBuilder.dneg();
      boxDouble(codeBuilder);
      return true;
    }
    if (("-".equals(operator) || "/".equals(operator) || "%".equals(operator)) && args.size() != 2) {
      return false;
    }
    if (("+".equals(operator) || "*".equals(operator)) && args.isEmpty()) {
      codeBuilder.aconst_null();
      return true;
    }
    emitDouble(codeBuilder, args.get(0));
    for (int i = 1; i < args.size(); i++) {
      emitDouble(codeBuilder, args.get(i));
      switch (operator) {
        case "+":
          codeBuilder.dadd();
          break;
        case "*":
          codeBuilder.dmul();
          break;
        case "-":
          codeBuilder.dsub();
          break;
        case "/":
          codeBuilder.ddiv();
          break;
        case "%":
          codeBuilder.drem();
          break;
        default:
          throw new IllegalArgumentException("Unsupported math operator: " + operator);
      }
    }
    boxDouble(codeBuilder);
    return true;
  }

  private void emitDouble(CodeBuilder codeBuilder, JsonLogicNode node) {
    emitSupportedNode(codeBuilder, node);
    codeBuilder.invokestatic(CD_RULE_HELPERS, "toComparableDouble", MethodTypeDesc.of(ClassDesc.ofDescriptor("D"), CD_Object));
  }

  private static void boxDouble(CodeBuilder codeBuilder) {
    codeBuilder.invokestatic(CD_DOUBLE, "valueOf", MethodTypeDesc.of(CD_DOUBLE, ClassDesc.ofDescriptor("D")));
  }

  private static boolean allSupported(JsonLogicArray args) {
    for (JsonLogicNode arg : args) {
      if (!isSupported(arg)) {
        return false;
      }
    }
    return true;
  }

  private static boolean allComparable(JsonLogicArray args) {
    for (JsonLogicNode arg : args) {
      if (!isComparable(arg)) {
        return false;
      }
    }
    return true;
  }

  private static boolean allMathNumeric(JsonLogicArray args) {
    for (JsonLogicNode arg : args) {
      if (!isMathNumeric(arg)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isComparable(JsonLogicNode node) {
    if (node instanceof JsonLogicNumber || node instanceof JsonLogicBoolean || node instanceof JsonLogicVariable) {
      return true;
    }
    if (node instanceof JsonLogicString) {
      return isNumericString(((JsonLogicString) node).getValue());
    }
    return false;
  }

  private static boolean isMathNumeric(JsonLogicNode node) {
    if (node instanceof JsonLogicNumber) {
      return true;
    }
    if (node instanceof JsonLogicString) {
      return isNumericString(((JsonLogicString) node).getValue());
    }
    return false;
  }

  private static boolean isNumericString(String value) {
    try {
      Double.parseDouble(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean emitNot(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (args.size() != 1 || !isSupported(args.get(0))) {
      return false;
    }
    emitSupportedNode(codeBuilder, args.get(0));
    codeBuilder.invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object));
    emitBooleanNot(codeBuilder);
    if ("!!".equals(operator)) {
      emitBooleanNot(codeBuilder);
    }
    boxBoolean(codeBuilder);
    return true;
  }

  private static void emitBooleanNot(CodeBuilder codeBuilder) {
    final Label falseLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();
    codeBuilder
        .ifeq(falseLabel)
        .iconst_0()
        .goto_(endLabel)
        .labelBinding(falseLabel)
        .iconst_1()
        .labelBinding(endLabel);
  }

  private static void boxBoolean(CodeBuilder codeBuilder) {
    codeBuilder.invokestatic(CD_BOOLEAN, "valueOf", MethodTypeDesc.of(CD_BOOLEAN, CD_boolean));
  }

  private static boolean isSupported(JsonLogicNode ast) {
    if (ast instanceof JsonLogicNull
        || ast instanceof JsonLogicBoolean
        || ast instanceof JsonLogicNumber
        || ast instanceof JsonLogicString) {
      return true;
    }
    if (ast instanceof JsonLogicVariable) {
      final JsonLogicVariable variable = (JsonLogicVariable) ast;
      return variable.getKey() instanceof JsonLogicString && variable.getDefaultValue() instanceof JsonLogicNull;
    }
    if (ast instanceof JsonLogicOperation) {
      final JsonLogicOperation operation = (JsonLogicOperation) ast;
      final JsonLogicArray args = operation.getArguments();
      switch (operation.getOperator()) {
        case "==":
        case "!=":
        case "===":
        case "!==":
          return args.size() == 2 && isSupported(args.get(0)) && isSupported(args.get(1));
        case "!":
        case "!!":
          return args.size() == 1 && isSupported(args.get(0));
        case ">":
        case ">=":
        case "<":
        case "<=":
          return (args.size() == 2 || args.size() == 3) && allComparable(args);
        case "+":
        case "*":
          return allMathNumeric(args);
        case "-":
          return (args.size() == 1 || args.size() == 2) && allMathNumeric(args);
        case "/":
        case "%":
          return args.size() == 2 && allMathNumeric(args);
        default:
          return false;
      }
    }
    return false;
  }

  private CompiledRule handleFailure(String ruleJson, JsonLogicNode ast, Throwable failure) {
    if (strictMode || !fallbackEnabled) {
      throw new JsonLogicCompilationException("Class-File API compilation failed for rule: " + ruleJson, failure);
    }
    LOG.log(Level.WARNING, "Class-File API compilation failed, falling back to interpreter. Rule: " + ruleJson, failure);
    return data -> fallbackEvaluator.evaluate(ast, data);
  }

  private static String classNameFor(String ruleJson) {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < ruleJson.length(); i++) {
      hash ^= ruleJson.charAt(i);
      hash *= 0x100000001b3L;
    }
    return "Rule_" + Long.toUnsignedString(hash, 16);
  }
}
