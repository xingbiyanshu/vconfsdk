package com.kedacom.vconf.sdk.amulet;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.lang.PrimitiveTypeHelper;
import com.kedacom.vconf.sdk.utils.lang.StringHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;


class Helper {

    static boolean checkUserPara(String reqName, Object[] reqParas, IMagicBook magicBook){
        Class[] userParaClasses = magicBook.getUserParaClasses(reqName);
        if (null==userParaClasses || 0 == userParaClasses.length){
            userParaClasses = magicBook.getNativeParaClasses(reqName);// 如果没有指定用户参数类型，则用户参数类型同native方法参数类型
        }
        if (null==userParaClasses){
            KLog.p(KLog.ERROR, "user para types for %s have not registered yet!", reqName);
            return false;
        }
        boolean isGet = magicBook.isReqTypeGet(reqName);
        int parasNum = isGet ? userParaClasses.length-1 : userParaClasses.length;
        if (reqParas.length < parasNum){
            KLog.p(KLog.ERROR, "invalid req para nums for %s, #%s expected but #%s got", reqName, parasNum, reqParas.length);
            return false;
        }
        for(int i=0; i<parasNum; ++i){
            Class<?> userParaClz = userParaClasses[i];
            Class<?> reqParaClz = reqParas[i].getClass();
            if (reqParaClz==userParaClz // 同类
                    || userParaClz.isAssignableFrom(reqParaClz) // 子类亦可接受
                    || userParaClz.isPrimitive() && reqParaClz==PrimitiveTypeHelper.getWrapperClass(userParaClz) // 注册用户参数类型为基本类型，请求参数为对应的包装类亦可接受
            ){
                continue;
            }
            KLog.p(KLog.ERROR, "invalid user para type for %s, %s expected but %s got", reqName, userParaClz, reqParaClz);
            return false;
        }

        return true;
    }

    static Object[] convertUserPara2NativePara(Object[] userParas, Class<?>[] nativeParaTypes){
        if (null == nativeParaTypes){
            KLog.p(KLog.ERROR, "null == nativeParaTypes");
            return userParas;
        }
        if (userParas.length < nativeParaTypes.length){
            KLog.p(KLog.ERROR, "userParas.length(%s) < nativeParaTypes.length(%s)", userParas.length, nativeParaTypes.length);
            return userParas;
        }
        Object[] nativeParas = new Object[nativeParaTypes.length];
        for (int i=0; i<nativeParaTypes.length; ++i){
            Object userPara = userParas[i];
            Class<?> methodParaType = nativeParaTypes[i];
            KLog.p(KLog.DEBUG,"userPara[%s].class=%s, methodPara[%s].class=%s", i, null==userPara? null : userPara.getClass(), i, methodParaType);
            if (null == userPara){
                nativeParas[i] = methodParaType.isPrimitive() ? PrimitiveTypeHelper.getDefaultValue(methodParaType) : null;
            }else if (userPara.getClass() == methodParaType
                    || methodParaType.isAssignableFrom(userPara.getClass())){
                nativeParas[i] = userPara;
            }else {
                if (StringHelper.isStringCompatible(methodParaType)) {
                    if (StringHelper.isStringCompatible(userPara.getClass())) {
                        nativeParas[i] = StringHelper.convert2CompatibleType(methodParaType, userPara);
                    }else {
                        nativeParas[i] = StringHelper.convert2CompatibleType(methodParaType, Kson.toJson(userPara));
                    }
                } else if (methodParaType.isPrimitive()) {
                    if (userPara.getClass() == PrimitiveTypeHelper.getWrapperClass(methodParaType)){
                        nativeParas[i] = userPara;
                    }else if (userPara.getClass().isEnum() && methodParaType==int.class) {
                        nativeParas[i] = Integer.valueOf(Kson.toJson(userPara));
                    }else{
                        throw new ClassCastException("trying to convert user para to native method para failed: "+userPara.getClass()+" can not cast to "+methodParaType);
                    }
                } else {
                    throw new ClassCastException("trying to convert user para to native method para failed: "+userPara.getClass()+" can not cast to "+methodParaType);
                }
            }

        }

        return nativeParas;
    }

}