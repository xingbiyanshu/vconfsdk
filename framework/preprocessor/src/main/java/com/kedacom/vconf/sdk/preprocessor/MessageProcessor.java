package com.kedacom.vconf.sdk.preprocessor;

import com.google.auto.service.AutoService;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;


/**
 * Created by Sissi on 2018/9/3.
 */

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.kedacom.vconf.sdk.annotation.Module",
        "com.kedacom.vconf.sdk.annotation.Request",
        "com.kedacom.vconf.sdk.annotation.Response",
        "com.kedacom.vconf.sdk.annotation.Notification",
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MessageProcessor extends AbstractProcessor {
    private boolean bDone = false;

    private String moduleName;
    private Table<String, String, Object> reqMap = HashBasedTable.create();
    private Table<String, String, Object> rspMap = HashBasedTable.create();
    private Table<String, String, Object> ntfMap = HashBasedTable.create();

    private String packageName;

    private Messager messager;

    private static String COL_NAME = "name";
    private static String COL_OWNER = "owner";
    private static String COL_PARAS = "paras";
    private static String COL_USERPARAS = "userParas";
    private static String COL_OutputParaIndex = "outputParaIndex";
    private static String COL_RSPSEQ = "rspSeq";
    private static String COL_TIMEOUT = "timeout";
    private static String COL_CLZ = "clz";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (bDone){
            return true;
        }
        bDone = true;

        messager = processingEnv.getMessager();

//        messager.printMessage(Diagnostic.Kind.NOTE, "START processing @Module, processor "+this);

        Set<? extends Element> msgSet = roundEnvironment.getElementsAnnotatedWith(Module.class);
        for (Element element : msgSet) {
            if (ElementKind.ENUM != element.getKind()){
                continue;
            }
            parseMessage((TypeElement) element);
            generateFile();
        }

        return true;
    }



    private void parseMessage(TypeElement msgDefClass){
        moduleName = msgDefClass.getAnnotation(Module.class).name();
        if (moduleName.trim().isEmpty()){
            throw new IllegalArgumentException(msgDefClass+": module name can not be empty!");
        }
//        messager.printMessage(Diagnostic.Kind.NOTE, "processing @Module "+moduleName);

        packageName = ((PackageElement) msgDefClass.getEnclosingElement()).getQualifiedName().toString();
        // 清除掉前一个Msg的残留数据
        reqMap.clear();
        rspMap.clear();
        ntfMap.clear();
        String defaultClz = "com.kedacom.vconf.sdk.annotation.Notification.Default";

        List<? extends Element> msgElements = msgDefClass.getEnclosedElements();
        for (Element element : msgElements) {
            if (ElementKind.ENUM_CONSTANT != element.getKind()) {
                continue;
            }
            String enumName = element.getSimpleName().toString();
            String msgId = moduleName +"_"+enumName;
            Response response  = element.getAnnotation(Response.class);
            String rspName = null;
            String rspClzFullName = null;
            if (null != response) {
                rspName = response.name().trim();
                if (rspName.isEmpty()){
                    throw new IllegalArgumentException(String.format("@Response.name() of %s cannot be empty!", msgId));
                }
                rspMap.put(msgId, COL_NAME, rspName);

                try {
                    Class<?> clz = response.clz();
                    rspClzFullName = clz.getCanonicalName();
                } catch (MirroredTypeException mte) {
                    rspClzFullName = parseClassNameFromMirroredTypeException(mte);
                }
                rspMap.put(msgId, COL_CLZ, rspClzFullName);
            }

            Notification notification  = element.getAnnotation(Notification.class);
            if (null != notification) {
                String ntfName = !notification.name().trim().isEmpty() ? notification.name().trim() :
                        rspName != null ? rspName : ""; // 尝试跟随@Response.name()
                if (ntfName.isEmpty()){
                    throw new IllegalArgumentException(String.format("@Notification.name() of %s cannot be empty!", msgId));
                }
                ntfMap.put(msgId, COL_NAME, ntfName);

                String clzFullName;
                try {
                    Class<?> clz = notification.clz();
                    clzFullName = clz.getCanonicalName();
                } catch (MirroredTypeException mte) {
                    clzFullName = parseClassNameFromMirroredTypeException(mte);
                }
                if (clzFullName.equals(defaultClz)){
                    if (rspClzFullName != null) {
                        clzFullName = rspClzFullName; // 跟随@Response.clz()
                    }else{
                        throw new IllegalArgumentException(String.format("@Notification.clz() of %s cannot be empty!", msgId));
                    }
                }
                ntfMap.put(msgId, COL_CLZ, clzFullName);
            }
        }

        for (Element element : msgElements){
            if (ElementKind.ENUM_CONSTANT != element.getKind()){
                continue;
            }
            String enumName = element.getSimpleName().toString();
            String msgId = moduleName +"_"+enumName;
            Request request = element.getAnnotation(Request.class);
            if (null != request){
                reqMap.put(msgId, COL_NAME, !request.name().isEmpty() ? request.name() : enumName);

                reqMap.put(msgId, COL_OWNER, request.owner());

                String[] paraClzNames = null;
                try {
                    request.paras();
                }catch (MirroredTypesException mte) {
                    paraClzNames = parseClassNameFromMirroredTypesException(mte);
                }
                reqMap.put(msgId, COL_PARAS, Objects.requireNonNull(paraClzNames));
                String[] userParaClzNames = null;
                try {
                    request.userParas();
                }catch (MirroredTypesException mte) {
                    userParaClzNames = parseClassNameFromMirroredTypesException(mte);
                }
                if (paraClzNames.length != Objects.requireNonNull(userParaClzNames).length){
                    throw new IllegalArgumentException(String.format("%s's paras.length must be equal with userParas.length", msgId));
                }
                reqMap.put(msgId, COL_USERPARAS, Objects.requireNonNull(userParaClzNames));

                reqMap.put(msgId, COL_OutputParaIndex, Math.min(request.outputParaIndex(), paraClzNames.length -1));

                reqMap.put(msgId, COL_RSPSEQ, processRspSeqs(
                        request.rspSeq(),
                        request.rspSeq2(),
                        request.rspSeq3(),
                        request.rspSeq4()
                        )
                );

                reqMap.put(msgId, COL_TIMEOUT, request.timeout());
            }

        }
    }


    private String[][] processRspSeqs(String[]... rspSeqs){
        List<String[]> rspSeqList = new ArrayList<>();
        for (String[] rspSeq : rspSeqs){
            List<String> modRspSeq = new ArrayList<>();
            for (String rspId : rspSeq) {
                boolean isGreedyNote = Request.GREEDY.equals(rspId);
                if (isGreedyNote && (modRspSeq.isEmpty() || Request.GREEDY.equals(modRspSeq.get(modRspSeq.size() - 1)))) {
                    //剔除掉序列首部的以及重复的greedy note
                    continue;
                }
                String completeRspId = moduleName + "_" + rspId;
                if (!isGreedyNote) {
                    // 检查请求的响应序列中的响应是否已注册为响应
                    boolean matched = false;
                    for (String registeredRspId : rspMap.rowKeySet()) {
                        if (registeredRspId.equals(completeRspId)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        // 该响应未注册！
                        throw new RuntimeException(String.format("\"%s\" has not registered as a rsp yet!", rspId));
                    }
                }
                modRspSeq.add(isGreedyNote ? rspId : completeRspId);
            }
            if (!modRspSeq.isEmpty()) {
                rspSeqList.add(modRspSeq.toArray(new String[]{}));
            }
        }

        return rspSeqList.toArray(new String[][]{});
    }


    private String parseClassNameFromMirroredTypeException(MirroredTypeException mte){
        String className;
        TypeMirror typeMirror = mte.getTypeMirror();
        if (typeMirror instanceof PrimitiveType){
            className = typeMirror.toString();
        }else {
            try { // 为普通类类型
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                className = classTypeElement.getQualifiedName().toString();
            } catch (ClassCastException e) { // 为数组类型
                ArrayType classTypeMirror = (ArrayType) mte.getTypeMirror();
                className = classTypeMirror.getComponentType().toString() + "[]";
            }
        }

        return className;
    }

    private String[] parseClassNameFromMirroredTypesException(MirroredTypesException mte){
        List<String> paraClzNames = new ArrayList<>();
        String className;
        List<? extends TypeMirror> typeMirrors = mte.getTypeMirrors();
        for (TypeMirror mirror : typeMirrors){
            if (mirror instanceof PrimitiveType){
                className = mirror.toString();
            }else {
                try { // 为普通类类型
                    DeclaredType classTypeMirror = (DeclaredType) mirror;
                    TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                    className = classTypeElement.getQualifiedName().toString();
                } catch (ClassCastException e) { // 为数组类型
                    ArrayType classTypeMirror = (ArrayType) mirror;
                    className = classTypeMirror.getComponentType().toString() + "[]";
                }
            }
            paraClzNames.add(className);

        }
        return paraClzNames.toArray(new String[]{});
    }


    private void generateFile(){
        String module = "module";
        String reqMap = "reqMap";
        String rspMap = "rspMap";
        String ntfMap = "ntfMap";
        String reqId = "reqId";
        String rspName = "rspName";
        String ntfName = "ntfName";
        String rspId = "rspId";
        String ntfId = "ntfId";
        String rspIds = "rspIds";
        String ntfIds = "ntfIds";

        // 构造函数私有化
        // 我们不希望通过常规方式实例化该对象
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        // 构建静态代码块
        CodeBlock.Builder staticCodeBlockBuilder = CodeBlock.builder()
                .addStatement("$L = $S", module, moduleName)
                .addStatement("$L = $T.create()", reqMap, HashBasedTable.class)
                .addStatement("$L = $T.create()", rspMap, HashBasedTable.class)
                .addStatement("$L = $T.create()", ntfMap, HashBasedTable.class)
                ;

        for(Table.Cell<String, String, Object> cell : this.reqMap.cellSet()){
            String row = cell.getRowKey();
            String col = cell.getColumnKey();
            if (COL_NAME.equals(col)
                    || COL_OWNER.equals(col)) {
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $S)", reqMap, row, col, cell.getValue());
            }else if (COL_PARAS.equals(col)
                    || COL_USERPARAS.equals(col)){
                StringBuffer value = new StringBuffer();
                String[] paras = (String[]) cell.getValue();
                for (String para : Objects.requireNonNull(paras)){
                    value.append(para).append(".class, ");
                }
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, new Class[]{$L})", reqMap, row, col, value);
            }else if (COL_RSPSEQ.equals(col)){
                StringBuffer value = new StringBuffer();
                String[][] rspSeq = (String[][]) cell.getValue();
                for (String[] aRspSeq : Objects.requireNonNull(rspSeq)) {
                    value.append("{");
                    for (String anARspSeq : aRspSeq) {
                        value.append("\"").append(anARspSeq).append("\", ");
                    }
                    value.append("}, ");
                }
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, new String[][]{$L})", reqMap, row, col, value);
            }else if (COL_TIMEOUT.equals(col)
                    || COL_OutputParaIndex.equals(col)){
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $L)", reqMap, row, col, cell.getValue());
            }
        }

        for(Table.Cell<String, String, Object> cell : this.rspMap.cellSet()){
            String row = cell.getRowKey();
            String col = cell.getColumnKey();
            if (COL_NAME.equals(col)) {
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $S)", rspMap, row, col, cell.getValue());
            }else if (COL_CLZ.equals(col)){
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $L.class)", rspMap, row, col, cell.getValue());
            }
        }

        for(Table.Cell<String, String, Object> cell : this.ntfMap.cellSet()){
            String row = cell.getRowKey();
            String col = cell.getColumnKey();
            if (COL_NAME.equals(col)) {
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $S)", ntfMap, row, col, cell.getValue());
            }else if (COL_CLZ.equals(col)){
                staticCodeBlockBuilder.addStatement("$L.put($S, $S, $L.class)", ntfMap, row, col, cell.getValue());
            }
        }

        // 实现IMagicBook
        ParameterSpec reqIdParameterSpec = ParameterSpec.builder(String.class, reqId).build();
        ParameterSpec rspIdParameterSpec = ParameterSpec.builder(String.class, rspId).build();
        ParameterSpec rspNameParameterSpec = ParameterSpec.builder(String.class, rspName).build();
        ParameterSpec ntfIdParameterSpec = ParameterSpec.builder(String.class, ntfId).build();
        ParameterSpec ntfNameParameterSpec = ParameterSpec.builder(String.class, ntfName).build();
        List<MethodSpec> methodSpecs = new ArrayList<>();
        MethodSpec name = MethodSpec.methodBuilder("name")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", moduleName)
                .build();
        methodSpecs.add(name);

        MethodSpec outputParaIndex = MethodSpec.methodBuilder("outputParaIndex")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(int.class)
                .addCode("Object val = $L.row($L).get($S);\n" +
                                "if (null == val) return -1;\n" +
                        "return (int)val;\n",
                        reqMap, reqId, COL_OutputParaIndex)
                .build();
        methodSpecs.add(outputParaIndex);

        MethodSpec reqName = MethodSpec.methodBuilder("reqName")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(String.class)
                .addCode("return (String)$L.row($L).get($S);\n", reqMap, reqId, COL_NAME)
                .build();
        methodSpecs.add(reqName);

        MethodSpec nativeMethodOwner = MethodSpec.methodBuilder("nativeMethodOwner")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(String.class)
                .addCode("return (String)$L.row($L).get($S);\n", reqMap, reqId, COL_OWNER)
                .build();
        methodSpecs.add(nativeMethodOwner);

        MethodSpec nativeParaClasses = MethodSpec.methodBuilder("nativeParaClasses")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class))))
                .addCode("return (Class<?>[])$L.row($L).get($S);\n", reqMap, reqId, COL_PARAS)
                .build();
        methodSpecs.add(nativeParaClasses);

        MethodSpec userParaClasses = MethodSpec.methodBuilder("userParaClasses")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class))))
                .addCode("return (Class<?>[])$L.row($L).get($S);\n", reqMap, reqId, COL_USERPARAS)
                .build();
        methodSpecs.add(userParaClasses);

        MethodSpec timeout = MethodSpec.methodBuilder("timeout")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(double.class)
                .addCode("Object val = $L.row($L).get($S);\n" +
                                "if (null == val) return 5;\n" +
                                "return (double)val;\n",
                        reqMap, reqId, COL_TIMEOUT)
                .build();
        methodSpecs.add(timeout);

        MethodSpec rspSeqs = MethodSpec.methodBuilder("rspSeqs")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(reqIdParameterSpec)
                .returns(String[][].class)
                .addCode("return (String[][])$L.row($L).get($S);\n", reqMap, reqId, COL_RSPSEQ)
                .build();
        methodSpecs.add(rspSeqs);

        MethodSpec rspClass = MethodSpec.methodBuilder("rspClass")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(rspIdParameterSpec)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                .addCode("return (Class<?>)$L.row($L).get($S);\n", rspMap, rspId, COL_CLZ)
                .build();
        methodSpecs.add(rspClass);

        MethodSpec isGreedyNote = MethodSpec.methodBuilder("isGreedyNote")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(rspIdParameterSpec)
                .returns(boolean.class)
                .addStatement("return $S.equals($L)", Request.GREEDY, rspId)
                .build();
        methodSpecs.add(isGreedyNote);


        MethodSpec methodRspIds = MethodSpec.methodBuilder("rspIds")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(rspNameParameterSpec)
                .returns(ParameterizedTypeName.get(Set.class, String.class))
                .addStatement("$T<$T> $L = new $T<>()", Set.class, String.class, rspIds, HashSet.class)
                .beginControlFlow("for ($T<$T, $T, Object> cell: $L.cellSet())", Table.Cell.class, String.class, String.class, rspMap)
                .beginControlFlow("if ($S.equals(cell.getColumnKey()) && ($L==null || $L.equals(cell.getValue())) )", COL_NAME, rspName, rspName)
                .addStatement("$L.add(cell.getRowKey())", rspIds)
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $L", rspIds)
                .build();
        methodSpecs.add(methodRspIds);

        MethodSpec ntfClass = MethodSpec.methodBuilder("ntfClass")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ntfIdParameterSpec)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                .addCode("return (Class<?>)$L.row($L).get($S);\n", ntfMap, ntfId, COL_CLZ)
                .build();
        methodSpecs.add(ntfClass);

        MethodSpec methodNtfIds = MethodSpec.methodBuilder("ntfIds")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ntfNameParameterSpec)
                .returns(ParameterizedTypeName.get(Set.class, String.class))
                .addStatement("$T<$T> $L = new $T<>()", Set.class, String.class, ntfIds, HashSet.class)
                .beginControlFlow("for ($T<$T, $T, Object> cell: $L.cellSet())", Table.Cell.class, String.class, String.class, ntfMap)
                .beginControlFlow("if ($S.equals(cell.getColumnKey()) && ($L==null || $L.equals(cell.getValue())) )", COL_NAME, ntfName, ntfName)
                .addStatement("$L.add(cell.getRowKey())", ntfIds)
                .endControlFlow()
                .endControlFlow()
                .addStatement("return $L", ntfIds)
                .build();
        methodSpecs.add(methodNtfIds);

        /*构建Class
        * 对于生成的类，我们不希望能通过常规手段访问，只允许通过反射访问（只让框架知道访问方式），
        * 所以我们将类及其成员的访问权限限制到最小。
        * */
        String className = "MagicBook$$Impl";
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.FINAL)
                .addField(FieldSpec.builder(String.class,
                        module, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        reqMap, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        rspMap, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(Table.class, String.class, String.class, Object.class),
                        ntfMap, Modifier.PRIVATE, Modifier.STATIC)
                        .build())
                .addMethod(constructor.build())
                .addStaticBlock(staticCodeBlockBuilder.build())
//                .addType(TypeSpec.classBuilder("InnerClassTest").build())
                .addSuperinterface(ClassName.get("com.kedacom.vconf.sdk.amulet", "IMagicBook"))
                .addMethods(methodSpecs)
                .build();

        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .build();

        // 生成源文件
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        messager.printMessage(Diagnostic.Kind.NOTE, "SUCCESS! generate file : "+ packageName+"."+className+".java");
    }

}
