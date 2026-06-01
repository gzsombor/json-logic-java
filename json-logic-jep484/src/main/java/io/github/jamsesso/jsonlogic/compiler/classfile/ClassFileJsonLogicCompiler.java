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
import io.github.jamsesso.jsonlogic.ast.JsonLogicObject;
import io.github.jamsesso.jsonlogic.ast.JsonLogicOperation;
import io.github.jamsesso.jsonlogic.ast.JsonLogicString;
import io.github.jamsesso.jsonlogic.ast.JsonLogicVariable;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilationException;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompilerImplementation;
import io.github.jamsesso.jsonlogic.compiler.RuleHelpers;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClassFileJsonLogicCompiler implements JsonLogicCompilerImplementation {
  private static final Logger LOG = Logger.getLogger(ClassFileJsonLogicCompiler.class.getName());
  static final String GENERATED_PACKAGE = "io.github.jamsesso.jsonlogic.compiler.classfile.gen";
  private static final ClassDesc CD_CALL_SITE = ClassDesc.of("java.lang.invoke.CallSite");
  private static final ClassDesc CD_BOOLEAN = ClassDesc.of(Boolean.class.getName());
  private static final ClassDesc CD_ARRAY_LIST = ClassDesc.of(ArrayList.class.getName());
  private static final ClassDesc CD_COMPILED_RULE = ClassDesc.of(CompiledRule.class.getName());
  private static final ClassDesc CD_DOUBLE = ClassDesc.of(Double.class.getName());
  private static final ClassDesc CD_ARRAY_LIKE = ClassDesc.of(ArrayLike.class.getName());
  private static final ClassDesc CD_EVALUATION_EXCEPTION = ClassDesc.of(JsonLogicEvaluationException.class.getName());
  private static final ClassDesc CD_JSON_LOGIC = ClassDesc.of(JsonLogic.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_EVALUATOR = ClassDesc.of(JsonLogicEvaluator.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_NODE = ClassDesc.of(JsonLogicNode.class.getName());
  private static final ClassDesc CD_JSON_LOGIC_OPERATION = ClassDesc.of(JsonLogicOperation.class.getName());
  private static final ClassDesc CD_ITERATOR = ClassDesc.of(Iterator.class.getName());
  private static final ClassDesc CD_LINKED_HASH_MAP = ClassDesc.of(LinkedHashMap.class.getName());
  private static final ClassDesc CD_LIST = ClassDesc.of(List.class.getName());
  private static final ClassDesc CD_LOOKUP = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");
  private static final ClassDesc CD_MAP = ClassDesc.of(Map.class.getName());
  private static final ClassDesc CD_METHOD_TYPE = ClassDesc.of("java.lang.invoke.MethodType");
  private static final ClassDesc CD_RULE_HELPERS = ClassDesc.of(RuleHelpers.class.getName());
  private static final ClassDesc CD_STRING_CONCAT_FACTORY = ClassDesc.of("java.lang.invoke.StringConcatFactory");
  private static final MethodTypeDesc MTD_STRING_CONCAT_BOOTSTRAP = MethodTypeDesc.of(
      CD_CALL_SITE, CD_LOOKUP, CD_String, CD_METHOD_TYPE, CD_String);

  private final JsonLogicEvaluator fallbackEvaluator;
  private final boolean fallbackEnabled;
  private final boolean strictMode;
  private ClassDesc currentGeneratedClass;
  private int nextLocalSlot;

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

  byte[] generateClass(ClassDesc generatedClass, JsonLogicNode ast) {
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
    currentGeneratedClass = generatedClass;
    nextLocalSlot = 2;
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
    if (ast instanceof JsonLogicArray) {
      return emitArray(codeBuilder, (JsonLogicArray) ast);
    }
    if (ast instanceof JsonLogicObject) {
      return emitObject(codeBuilder, (JsonLogicObject) ast);
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
    if (!isSupported(variable.getKey()) || !isSupported(variable.getDefaultValue())) {
      return false;
    }
    codeBuilder
        .aload(1);
    emitRequiredNode(codeBuilder, variable.getKey());
    emitRequiredNode(codeBuilder, variable.getDefaultValue());
    codeBuilder
        .invokestatic(
            CD_RULE_HELPERS,
            "resolveVar",
            MethodTypeDesc.of(CD_Object, CD_Object, CD_Object, CD_Object));
    return true;
  }

  private boolean emitArray(CodeBuilder codeBuilder, JsonLogicArray array) {
    if (!allSupported(array)) {
      return false;
    }
    codeBuilder
        .new_(CD_ARRAY_LIST)
        .dup()
        .ldc(array.size())
        .invokespecial(CD_ARRAY_LIST, INIT_NAME, MethodTypeDesc.ofDescriptor("(I)V"));
    for (JsonLogicNode element : array) {
      codeBuilder.dup();
      emitRequiredNode(codeBuilder, element);
      codeBuilder
          .invokeinterface(CD_LIST, "add", MethodTypeDesc.of(CD_boolean, CD_Object))
          .pop();
    }
    return true;
  }

  private boolean emitObject(CodeBuilder codeBuilder, JsonLogicObject object) {
    for (JsonLogicNode value : object.getEntries().values()) {
      if (!isSupported(value)) {
        return false;
      }
    }
    codeBuilder
        .new_(CD_LINKED_HASH_MAP)
        .dup()
        .invokespecial(CD_LINKED_HASH_MAP, INIT_NAME, MTD_void);
    for (Map.Entry<String, JsonLogicNode> entry : object.getEntries().entrySet()) {
      codeBuilder
          .dup()
          .ldc(entry.getKey());
      emitRequiredNode(codeBuilder, entry.getValue());
      codeBuilder
          .invokeinterface(CD_MAP, "put", MethodTypeDesc.of(CD_Object, CD_Object, CD_Object))
          .pop();
    }
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
      case "min":
      case "max":
        return emitMath(codeBuilder, operation.getOperator(), args);
      case "if":
      case "?:":
        return emitIf(codeBuilder, args);
      case "and":
        return emitAnd(codeBuilder, args);
      case "or":
        return emitOr(codeBuilder, args);
      case "cat":
        return emitCat(codeBuilder, args);
      case "substr":
        return emitSubstr(codeBuilder, args);
      case "in":
        return emitIn(codeBuilder, args);
      case "missing":
        return emitMissing(codeBuilder, args);
      case "missing_some":
        return emitMissingSome(codeBuilder, args);
      case "merge":
        return emitMerge(codeBuilder, args);
      case "log":
        return emitLog(codeBuilder, args);
      case "all":
        return emitAll(codeBuilder, args);
      case "map":
        return emitMap(codeBuilder, args);
      case "filter":
        return emitFilter(codeBuilder, args);
      case "some":
        return emitSomeNone(codeBuilder, "some", args);
      case "none":
        return emitSomeNone(codeBuilder, "none", args);
      default:
        return false;
    }
  }

  private boolean emitCat(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (!allSupported(args)) {
      return false;
    }
    emitArray(codeBuilder, args);
    codeBuilder.invokestatic(CD_RULE_HELPERS, "cat", MethodTypeDesc.of(CD_String, CD_LIST));
    return true;
  }

  private boolean emitSubstr(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.size() < 2 || args.size() > 3) {
      emitFailure(codeBuilder, "substr expects 2 or 3 arguments", ".substr");
      return true;
    }
    if (!allSupported(args)) {
      return false;
    }
    emitRequiredNode(codeBuilder, args.get(0));
    emitRequiredNode(codeBuilder, args.get(1));
    if (args.size() == 3) {
      emitRequiredNode(codeBuilder, args.get(2));
    } else {
      codeBuilder.aconst_null();
    }
    codeBuilder.ldc(".substr");
    codeBuilder.invokestatic(
        CD_RULE_HELPERS,
        "substr",
        MethodTypeDesc.of(CD_String, CD_Object, CD_Object, CD_Object, CD_String));
    return true;
  }

  private static void emitFailure(CodeBuilder codeBuilder, String message, String path) {
    codeBuilder
        .ldc(message)
        .ldc(path)
        .invokestatic(CD_RULE_HELPERS, "fail", MethodTypeDesc.of(CD_Object, CD_String, CD_String));
  }

  private void emitRequiredNode(CodeBuilder codeBuilder, JsonLogicNode node) {
    if (!emitSupportedNode(codeBuilder, node)) {
      throw new IllegalStateException("Expected supported JEP 484 node was not emitted: " + node.getType());
    }
  }

  private boolean emitIn(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.size() < 2) {
      codeBuilder.getstatic(CD_BOOLEAN, "FALSE", CD_BOOLEAN);
      return true;
    }
    if (!isSupported(args.get(0)) || !isSupported(args.get(1))) {
      return false;
    }
    emitRequiredNode(codeBuilder, args.get(0));
    emitRequiredNode(codeBuilder, args.get(1));
    codeBuilder.invokestatic(CD_RULE_HELPERS, "in", MethodTypeDesc.of(CD_boolean, CD_Object, CD_Object));
    boxBoolean(codeBuilder);
    return true;
  }

  private boolean emitMissing(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (!allSupported(args)) {
      return false;
    }
    emitArray(codeBuilder, args);
    codeBuilder
        .aload(1)
        .invokestatic(CD_RULE_HELPERS, "missing", MethodTypeDesc.of(CD_LIST, CD_LIST, CD_Object));
    return true;
  }

  private boolean emitMissingSome(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.size() < 2) {
      emitFailure(
          codeBuilder,
          "missing_some expects first argument to be an integer and the second argument to be an array",
          ".missing_some");
      return true;
    }
    if (!isSupported(args.get(0)) || !isSupported(args.get(1))) {
      return false;
    }
    emitRequiredNode(codeBuilder, args.get(0));
    emitRequiredNode(codeBuilder, args.get(1));
    codeBuilder
        .aload(1)
        .invokestatic(CD_RULE_HELPERS, "missingSomeChecked", MethodTypeDesc.of(CD_LIST, CD_Object, CD_Object, CD_Object));
    return true;
  }

  private boolean emitMerge(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (!allSupported(args)) {
      return false;
    }
    emitArray(codeBuilder, args);
    codeBuilder.invokestatic(CD_RULE_HELPERS, "merge", MethodTypeDesc.of(CD_LIST, CD_LIST));
    return true;
  }

  private boolean emitLog(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.isEmpty()) {
      emitFailure(codeBuilder, "log operator requires exactly 1 argument", ".log");
      return true;
    }
    if (!isSupported(args.get(0))) {
      return false;
    }
    emitRequiredNode(codeBuilder, args.get(0));
    codeBuilder.invokestatic(CD_RULE_HELPERS, "log", MethodTypeDesc.of(CD_Object, CD_Object));
    return true;
  }

  private boolean emitAll(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.size() != 2) {
      emitFailure(codeBuilder, "all expects exactly 2 arguments", ".all");
      return true;
    }
    if (!isSupported(args.get(0))) {
      return false;
    }

    final int maybeArraySlot = allocateLocalSlot(1);
    final int arraySlot = allocateLocalSlot(1);
    final int iteratorSlot = allocateLocalSlot(1);
    final Label falseLabel = codeBuilder.newLabel();
    final Label loopLabel = codeBuilder.newLabel();
    final Label trueLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();

    emitRequiredNode(codeBuilder, args.get(0));
    codeBuilder.astore(maybeArraySlot);
    codeBuilder.aload(maybeArraySlot).ifnull(falseLabel);
    codeBuilder.aload(maybeArraySlot).invokestatic(CD_ARRAY_LIKE, "isEligible", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifeq(falseLabel);
    codeBuilder.new_(CD_ARRAY_LIKE).dup().aload(maybeArraySlot).invokespecial(CD_ARRAY_LIKE, INIT_NAME, MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V"));
    codeBuilder.astore(arraySlot);
    codeBuilder.aload(arraySlot).invokevirtual(CD_ARRAY_LIKE, "isEmpty", MethodTypeDesc.of(CD_boolean)).ifne(falseLabel);
    codeBuilder.aload(arraySlot).invokevirtual(CD_ARRAY_LIKE, "iterator", MethodTypeDesc.of(CD_ITERATOR)).astore(iteratorSlot);
    codeBuilder.labelBinding(loopLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(CD_boolean)).ifeq(trueLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(CD_Object));
    emitEvaluateCollectionBody(codeBuilder, args.get(1));
    codeBuilder.invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifeq(falseLabel).goto_(loopLabel);
    codeBuilder.labelBinding(falseLabel).getstatic(CD_BOOLEAN, "FALSE", CD_BOOLEAN).goto_(endLabel);
    codeBuilder.labelBinding(trueLabel).getstatic(CD_BOOLEAN, "TRUE", CD_BOOLEAN).labelBinding(endLabel);
    return true;
  }

  private boolean emitMap(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.size() != 2) {
      emitFailure(codeBuilder, "map expects exactly 2 arguments", ".map");
      return true;
    }
    if (!isSupported(args.get(0))) {
      return false;
    }

    final int maybeArraySlot = allocateLocalSlot(1);
    final int resultSlot = allocateLocalSlot(1);
    final int iteratorSlot = allocateLocalSlot(1);
    final Label emptyLabel = codeBuilder.newLabel();
    final Label loopLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();

    emitRequiredNode(codeBuilder, args.get(0));
    codeBuilder.astore(maybeArraySlot);
    codeBuilder.aload(maybeArraySlot).invokestatic(CD_ARRAY_LIKE, "isEligible", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifeq(emptyLabel);
    emitNewArrayList(codeBuilder);
    codeBuilder.astore(resultSlot);
    codeBuilder.new_(CD_ARRAY_LIKE).dup().aload(maybeArraySlot).invokespecial(CD_ARRAY_LIKE, INIT_NAME, MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V"));
    codeBuilder.invokevirtual(CD_ARRAY_LIKE, "iterator", MethodTypeDesc.of(CD_ITERATOR)).astore(iteratorSlot);
    codeBuilder.labelBinding(loopLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(CD_boolean)).ifeq(endLabel);
    codeBuilder.aload(resultSlot);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(CD_Object));
    emitEvaluateCollectionBody(codeBuilder, args.get(1));
    codeBuilder.invokeinterface(CD_LIST, "add", MethodTypeDesc.of(CD_boolean, CD_Object)).pop().goto_(loopLabel);
    codeBuilder.labelBinding(emptyLabel);
    emitNewArrayList(codeBuilder);
    codeBuilder.astore(resultSlot);
    codeBuilder.labelBinding(endLabel).aload(resultSlot);
    return true;
  }

  private boolean emitFilter(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.size() != 2) {
      emitFailure(codeBuilder, "filter expects exactly 2 arguments", ".filter");
      return true;
    }
    if (!isSupported(args.get(0))) {
      return false;
    }

    final int maybeArraySlot = allocateLocalSlot(1);
    final int resultSlot = allocateLocalSlot(1);
    final int iteratorSlot = allocateLocalSlot(1);
    final int itemSlot = allocateLocalSlot(1);
    final Label failLabel = codeBuilder.newLabel();
    final Label loopLabel = codeBuilder.newLabel();
    final Label skipLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();

    emitRequiredNode(codeBuilder, args.get(0));
    codeBuilder.astore(maybeArraySlot);
    codeBuilder.aload(maybeArraySlot).invokestatic(CD_ARRAY_LIKE, "isEligible", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifeq(failLabel);
    emitNewArrayList(codeBuilder);
    codeBuilder.astore(resultSlot);
    codeBuilder.new_(CD_ARRAY_LIKE).dup().aload(maybeArraySlot).invokespecial(CD_ARRAY_LIKE, INIT_NAME, MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V"));
    codeBuilder.invokevirtual(CD_ARRAY_LIKE, "iterator", MethodTypeDesc.of(CD_ITERATOR)).astore(iteratorSlot);
    codeBuilder.labelBinding(loopLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(CD_boolean)).ifeq(endLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(CD_Object)).astore(itemSlot);
    codeBuilder.aload(itemSlot);
    emitEvaluateCollectionBody(codeBuilder, args.get(1));
    codeBuilder.invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifeq(skipLabel);
    codeBuilder.aload(resultSlot).aload(itemSlot).invokeinterface(CD_LIST, "add", MethodTypeDesc.of(CD_boolean, CD_Object)).pop();
    codeBuilder.labelBinding(skipLabel).goto_(loopLabel);
    codeBuilder.labelBinding(failLabel);
    emitFailure(codeBuilder, "first argument to filter must be a valid array", ".filter[0]");
    codeBuilder.areturn();
    codeBuilder.labelBinding(endLabel).aload(resultSlot);
    return true;
  }

  private boolean emitSomeNone(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (args.size() != 2) {
      emitFailure(codeBuilder, operator + " expects exactly 2 arguments", "." + operator);
      return true;
    }
    if (!isSupported(args.get(0))) {
      return false;
    }

    final int maybeArraySlot = allocateLocalSlot(1);
    final int iteratorSlot = allocateLocalSlot(1);
    final Label nullLabel = codeBuilder.newLabel();
    final Label trueLabel = codeBuilder.newLabel();
    final Label falseLabel = codeBuilder.newLabel();
    final Label loopLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();

    emitRequiredNode(codeBuilder, args.get(0));
    codeBuilder.astore(maybeArraySlot);
    codeBuilder.aload(maybeArraySlot).ifnull(nullLabel);
    codeBuilder.aload(maybeArraySlot).invokestatic(CD_ARRAY_LIKE, "isEligible", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifeq(falseLabel);
    codeBuilder.new_(CD_ARRAY_LIKE).dup().aload(maybeArraySlot).invokespecial(CD_ARRAY_LIKE, INIT_NAME, MethodTypeDesc.ofDescriptor("(Ljava/lang/Object;)V"));
    codeBuilder.invokevirtual(CD_ARRAY_LIKE, "iterator", MethodTypeDesc.of(CD_ITERATOR)).astore(iteratorSlot);
    codeBuilder.labelBinding(loopLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(CD_boolean)).ifeq(falseLabel);
    codeBuilder.aload(iteratorSlot).invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(CD_Object));
    emitEvaluateCollectionBody(codeBuilder, args.get(1));
    codeBuilder.invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object));
    codeBuilder.ifne(trueLabel).goto_(loopLabel);

    codeBuilder.labelBinding(nullLabel);
    codeBuilder.getstatic(CD_BOOLEAN, "some".equals(operator) ? "FALSE" : "TRUE", CD_BOOLEAN).goto_(endLabel);
    codeBuilder.labelBinding(trueLabel);
    codeBuilder.getstatic(CD_BOOLEAN, "some".equals(operator) ? "TRUE" : "FALSE", CD_BOOLEAN).goto_(endLabel);
    codeBuilder.labelBinding(falseLabel);
    codeBuilder.getstatic(CD_BOOLEAN, "some".equals(operator) ? "FALSE" : "TRUE", CD_BOOLEAN).labelBinding(endLabel);
    return true;
  }

  private void emitEvaluateCollectionBody(CodeBuilder codeBuilder, JsonLogicNode body) {
    final int dataSlot = allocateLocalSlot(1);
    codeBuilder
        .astore(dataSlot)
        .aload(0)
        .getfield(currentGeneratedClass, "fallback", CD_JSON_LOGIC_EVALUATOR);
    emitSecondOperationArgument(codeBuilder);
    codeBuilder.aload(dataSlot);
    codeBuilder.invokevirtual(CD_JSON_LOGIC_EVALUATOR, "evaluate", MethodTypeDesc.of(CD_Object, CD_JSON_LOGIC_NODE, CD_Object));
  }

  private void emitSecondOperationArgument(CodeBuilder codeBuilder) {
    codeBuilder.aload(0).getfield(currentGeneratedClass, "ast", CD_JSON_LOGIC_NODE).checkcast(CD_JSON_LOGIC_OPERATION);
    codeBuilder.invokevirtual(CD_JSON_LOGIC_OPERATION, "getArguments", MethodTypeDesc.of(ClassDesc.of(JsonLogicArray.class.getName())));
    codeBuilder.ldc(1).invokeinterface(CD_LIST, "get", MethodTypeDesc.of(CD_Object, ClassDesc.ofDescriptor("I"))).checkcast(CD_JSON_LOGIC_NODE);
  }

  private static void emitNewArrayList(CodeBuilder codeBuilder) {
    codeBuilder.new_(CD_ARRAY_LIST).dup().invokespecial(CD_ARRAY_LIST, INIT_NAME, MTD_void);
  }

  private boolean emitUnsupportedCollectionOperation(
      CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (args.size() != 2) {
      emitFailure(codeBuilder, operator + " expects exactly 2 arguments", "." + operator);
      return true;
    }
    return false;
  }

  private boolean emitIf(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (!allSupported(args)) {
      return false;
    }
    if (args.isEmpty()) {
      codeBuilder.aconst_null();
      return true;
    }
    if (args.size() == 1) {
      emitRequiredNode(codeBuilder, args.get(0));
      return true;
    }
    final Label endLabel = codeBuilder.newLabel();
    for (int i = 0; i < args.size() - 1; i += 2) {
      final Label nextLabel = codeBuilder.newLabel();
      emitRequiredNode(codeBuilder, args.get(i));
      codeBuilder
          .invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object))
          .ifeq(nextLabel);
      emitRequiredNode(codeBuilder, args.get(i + 1));
      codeBuilder
          .goto_(endLabel)
          .labelBinding(nextLabel);
    }
    if ((args.size() & 1) == 0) {
      codeBuilder.aconst_null();
    } else {
      emitRequiredNode(codeBuilder, args.get(args.size() - 1));
    }
    codeBuilder.labelBinding(endLabel);
    return true;
  }

  private boolean emitAnd(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.isEmpty()) {
      emitFailure(codeBuilder, "and operator expects at least 1 argument", ".and");
      return true;
    }
    if (!allSupported(args)) {
      return false;
    }
    final Label endLabel = codeBuilder.newLabel();
    for (int i = 0; i < args.size(); i++) {
      emitRequiredNode(codeBuilder, args.get(i));
      if (i < args.size() - 1) {
        codeBuilder
            .dup()
            .invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object))
            .ifeq(endLabel)
            .pop();
      }
    }
    codeBuilder.labelBinding(endLabel);
    return true;
  }

  private boolean emitOr(CodeBuilder codeBuilder, JsonLogicArray args) {
    if (args.isEmpty()) {
      emitFailure(codeBuilder, "or operator expects at least 1 argument", ".or");
      return true;
    }
    if (!allSupported(args)) {
      return false;
    }
    final Label endLabel = codeBuilder.newLabel();
    for (int i = 0; i < args.size(); i++) {
      emitRequiredNode(codeBuilder, args.get(i));
      if (i < args.size() - 1) {
        codeBuilder
            .dup()
            .invokestatic(CD_JSON_LOGIC, "truthy", MethodTypeDesc.of(CD_boolean, CD_Object))
            .ifne(endLabel)
            .pop();
      }
    }
    codeBuilder.labelBinding(endLabel);
    return true;
  }

  private boolean emitEquality(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (args.size() != 2) {
      emitFailure(codeBuilder, "equality expressions expect exactly 2 arguments", "." + operator);
      return true;
    }
    if (!isSupported(args.get(0)) || !isSupported(args.get(1))) {
      return false;
    }
    emitRequiredNode(codeBuilder, args.get(0));
    emitRequiredNode(codeBuilder, args.get(1));
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
    if (args.size() < 2) {
      emitFailure(codeBuilder, "'" + operator + "' requires at least 2 arguments", "." + operator);
      return true;
    }
    if (!allComparable(args)) {
      return false;
    }
    emitComparePair(codeBuilder, operator, args.get(0), args.get(1));
    if (args.size() >= 3) {
      emitComparePair(codeBuilder, operator, args.get(1), args.get(2));
      codeBuilder.iand();
    }
    boxBoolean(codeBuilder);
    return true;
  }

  private void emitComparePair(CodeBuilder codeBuilder, String operator, JsonLogicNode left, JsonLogicNode right) {
    final int leftSlot = allocateLocalSlot(2);
    final int rightSlot = allocateLocalSlot(2);
    final Label falseLabel = codeBuilder.newLabel();
    final Label trueLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();

    emitDouble(codeBuilder, left);
    codeBuilder.dstore(leftSlot);
    emitDouble(codeBuilder, right);
    codeBuilder.dstore(rightSlot);

    codeBuilder
        .dload(leftSlot)
        .invokestatic(CD_DOUBLE, "isNaN", MethodTypeDesc.of(CD_boolean, ClassDesc.ofDescriptor("D")))
        .ifne(falseLabel)
        .dload(rightSlot)
        .invokestatic(CD_DOUBLE, "isNaN", MethodTypeDesc.of(CD_boolean, ClassDesc.ofDescriptor("D")))
        .ifne(falseLabel)
        .dload(leftSlot)
        .dload(rightSlot)
        .dcmpl();
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
        .labelBinding(falseLabel)
        .iconst_0()
        .goto_(endLabel)
        .labelBinding(trueLabel)
        .iconst_1()
        .labelBinding(endLabel);
  }

  private boolean emitMath(CodeBuilder codeBuilder, String operator, JsonLogicArray args) {
    if (!allSupported(args)) {
      return false;
    }

    if (args.isEmpty()) {
      codeBuilder.aconst_null();
      return true;
    }
    final JsonLogicArray effectiveArgs = effectiveMathArgs(operator, args);
    if (effectiveArgs == null || effectiveArgs.isEmpty()) {
      codeBuilder.aconst_null();
      return true;
    }
    if (("/".equals(operator) || "%".equals(operator)) && args.size() == 1) {
      codeBuilder.aconst_null();
      return true;
    }

    final int[] objectSlots = new int[effectiveArgs.size()];
    for (int i = 0; i < effectiveArgs.size(); i++) {
      objectSlots[i] = allocateLocalSlot(1);
      emitRequiredNode(codeBuilder, effectiveArgs.get(i));
      codeBuilder.astore(objectSlots[i]);
    }

    final Label nullLabel = codeBuilder.newLabel();
    final Label endLabel = codeBuilder.newLabel();
    for (int objectSlot : objectSlots) {
      codeBuilder.aload(objectSlot).ifnull(nullLabel);
      codeBuilder.aload(objectSlot).invokestatic(CD_RULE_HELPERS, "isNumeric", MethodTypeDesc.of(CD_boolean, CD_Object));
      codeBuilder.ifeq(nullLabel);
    }

    for (int i = 0; i < objectSlots.length; i++) {
      codeBuilder.aload(objectSlots[i]);
      codeBuilder.invokestatic(CD_RULE_HELPERS, "toDouble", MethodTypeDesc.of(ClassDesc.ofDescriptor("D"), CD_Object));
      if (i == 0 && "-".equals(operator) && effectiveArgs.size() == 1) {
        codeBuilder.dneg();
        break;
      }
      if (i == 0) {
        continue;
      }
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
        case "min":
          codeBuilder.invokestatic(CD_DOUBLE, "min", MethodTypeDesc.ofDescriptor("(DD)D"));
          break;
        case "max":
          codeBuilder.invokestatic(CD_DOUBLE, "max", MethodTypeDesc.ofDescriptor("(DD)D"));
          break;
        default:
          return false;
      }
    }
    boxDouble(codeBuilder);
    codeBuilder
        .goto_(endLabel)
        .labelBinding(nullLabel)
        .aconst_null()
        .labelBinding(endLabel);
    return true;
  }

  private int allocateLocalSlot(int width) {
    final int slot = nextLocalSlot;
    nextLocalSlot += width;
    return slot;
  }

  private static boolean containsArrayArgument(JsonLogicArray args) {
    for (JsonLogicNode arg : args) {
      if (arg instanceof JsonLogicArray) {
        return true;
      }
    }
    return false;
  }

  private static JsonLogicArray effectiveMathArgs(String operator, JsonLogicArray args) {
    if (("+".equals(operator) || "*".equals(operator)) && args.size() == 1 && args.get(0) instanceof JsonLogicArray) {
      return flattenMathArray((JsonLogicArray) args.get(0));
    }
    if ("+".equals(operator) || "*".equals(operator)) {
      return flattenMathArray(args);
    }
    return args;
  }

  private static JsonLogicArray flattenMathArray(JsonLogicArray args) {
    final List<JsonLogicNode> flattened = new ArrayList<>();
    for (JsonLogicNode arg : args) {
      JsonLogicNode current = arg;
      while (current instanceof JsonLogicArray) {
        final JsonLogicArray array = (JsonLogicArray) current;
        if (array.isEmpty()) {
          return null;
        }
        current = array.get(0);
      }
      flattened.add(current);
    }
    return new JsonLogicArray(flattened);
  }

  private void emitDouble(CodeBuilder codeBuilder, JsonLogicNode node) {
    emitRequiredNode(codeBuilder, node);
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
    if (node instanceof JsonLogicNull
        || node instanceof JsonLogicNumber
        || node instanceof JsonLogicBoolean
        || node instanceof JsonLogicString
        || node instanceof JsonLogicVariable
        || node instanceof JsonLogicArray) {
      return true;
    }
    if (node instanceof JsonLogicOperation) {
      return isSupported(node);
    }
    return false;
  }

  private static boolean isMathNumeric(JsonLogicNode node) {
    if (node instanceof JsonLogicNumber || node instanceof JsonLogicString) {
      return true;
    }
    if (node instanceof JsonLogicVariable || node instanceof JsonLogicArray || node instanceof JsonLogicOperation) {
      return isSupported(node);
    }
    if (node instanceof JsonLogicNull || node instanceof JsonLogicBoolean || node instanceof JsonLogicString) {
      return true;
    }
    if (node instanceof JsonLogicArray) {
      return isSupported(node);
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
    if (args.isEmpty()) {
      codeBuilder.getstatic(CD_BOOLEAN, "!".equals(operator) ? "TRUE" : "FALSE", CD_BOOLEAN);
      return true;
    }
    if (!isSupported(args.get(0))) {
      return false;
    }
    emitRequiredNode(codeBuilder, args.get(0));
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
    if (ast instanceof JsonLogicArray) {
      return allSupported((JsonLogicArray) ast);
    }
    if (ast instanceof JsonLogicObject) {
      final JsonLogicObject object = (JsonLogicObject) ast;
      for (JsonLogicNode value : object.getEntries().values()) {
        if (!isSupported(value)) {
          return false;
        }
      }
      return true;
    }
    if (ast instanceof JsonLogicVariable) {
      final JsonLogicVariable variable = (JsonLogicVariable) ast;
      return isSupported(variable.getKey()) && isSupported(variable.getDefaultValue());
    }
    if (ast instanceof JsonLogicOperation) {
      final JsonLogicOperation operation = (JsonLogicOperation) ast;
      final JsonLogicArray args = operation.getArguments();
      switch (operation.getOperator()) {
        case "==":
        case "!=":
        case "===":
        case "!==":
          return args.size() != 2 || (isSupported(args.get(0)) && isSupported(args.get(1)));
        case "!":
        case "!!":
          return args.isEmpty() || isSupported(args.get(0));
        case ">":
        case ">=":
        case "<":
        case "<=":
          return args.size() >= 2 && allComparable(args);
        case "+":
        case "*":
          return allMathNumeric(args);
        case "-":
          return (args.size() == 1 || args.size() == 2) && allMathNumeric(args);
        case "/":
        case "%":
        case "min":
        case "max":
          return allSupported(args);
        case "if":
        case "?:":
          return allSupported(args);
        case "and":
        case "or":
          return args.isEmpty() || allSupported(args);
        case "cat":
          return allSupported(args);
        case "substr":
          return args.size() < 2 || args.size() > 3 || allSupported(args);
        case "in":
          return args.size() < 2 || (isSupported(args.get(0)) && isSupported(args.get(1)));
        case "missing":
          return allSupported(args);
        case "missing_some":
          return args.size() < 2 || (isSupported(args.get(0)) && isSupported(args.get(1)));
        case "merge":
          return allSupported(args);
        case "log":
          return args.isEmpty() || isSupported(args.get(0));
        case "all":
        case "map":
        case "filter":
        case "some":
        case "none":
          return true;
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

  static String classNameFor(String ruleJson) {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < ruleJson.length(); i++) {
      hash ^= ruleJson.charAt(i);
      hash *= 0x100000001b3L;
    }
    return "Rule_" + Long.toUnsignedString(hash, 16);
  }
}
