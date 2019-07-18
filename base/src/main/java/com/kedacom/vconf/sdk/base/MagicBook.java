package com.kedacom.vconf.sdk.base;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Field;


@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "FieldCanBeLocal", "unchecked"})
final class MagicBook {
    private static MagicBook instance;

    private BiMap<String, String> nameIdMap = HashBiMap.create();
    private Table<String, String, Object> reqMap = HashBasedTable.create();
    private Table<String, String, Object> rspMap = HashBasedTable.create();

    private static int REQ_TYPE_SESSION = 0; // “请求——响应”，异步。
    private static int REQ_TYPE_GET = 1; // 如获取配置。
    private static int REQ_TYPE_SET = 2; // 如设置配置。

    private static String COL_ID = "id";
    private static String COL_METHOD = "method";
    private static String COL_OWNER = "owner";
    private static String COL_PARAS = "paras";
    private static String COL_USERPARAS = "userParas";
    private static String COL_TYPE = "type";
    private static String COL_RSPSEQ = "rspSeq";
    private static String COL_TIMEOUT = "timeout";
    private static String COL_CLZ = "clz";
    private static String COL_DELAY = "delay";

    private Gson gson = new Gson();

    private MagicBook(){
    }

    synchronized static MagicBook instance() {
        if (null == instance) {
            instance = new MagicBook();
        }

        return instance;
    }

    void addChapter(Class<?> chapter){
        if (null == chapter) {
            KLog.p(KLog.WARN,"null == chapter");
            return;
        }
        try {
            Field field = chapter.getField("nameIdMap");
            BiMap<String, String> nameIdMap = (BiMap<String, String>) field.get(null);
            for (String e : this.nameIdMap.keySet()){
                if (nameIdMap.keySet().contains(e)){
                    throw new IllegalArgumentException("duplicated msg "+e);
                }
            }
            this.nameIdMap.putAll(nameIdMap);
            field = chapter.getField("reqMap");
            reqMap.putAll((Table<String, String, Object>) field.get(null));
            field = chapter.getField("rspMap");
            rspMap.putAll((Table<String, String, Object>) field.get(null));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    String getMsgName(String msgId){
        return nameIdMap.inverse().get(msgId);
    }

    String getMsgId(String msgName){
        return nameIdMap.get(msgName);
    }

    boolean isRequest(String msgName){
        return reqMap.rowKeySet().contains(msgName);
    }

    boolean isResponse(String msgName){
        return rspMap.rowKeySet().contains(msgName);
    }

    boolean isNotification(String msgName){
        return isResponse(msgName);
    }

    boolean isSession(String reqName){
        if (null == reqMap.row(reqName)) return false;
        return REQ_TYPE_SESSION == (int)reqMap.row(reqName).get(COL_TYPE);
    }

    boolean isGet(String reqName){
        if (null == reqMap.row(reqName)) return false;
        return REQ_TYPE_GET == (int)reqMap.row(reqName).get(COL_TYPE);
    }

    boolean isSet(String reqName){
        if (null == reqMap.row(reqName)) return false;
        return REQ_TYPE_SET == (int)reqMap.row(reqName).get(COL_TYPE);
    }

    String getMethodOwner(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (String) reqMap.row(reqName).get(COL_OWNER);
    }
    String getMethod(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (String) reqMap.row(reqName).get(COL_METHOD);
    }

    Class<?>[] getParaClasses(String reqName) {
        if (null == reqMap.row(reqName)) return null;
        return (Class[]) reqMap.row(reqName).get(COL_PARAS);
    }
    Class<?>[] getUserParaClasses(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (Class[]) reqMap.row(reqName).get(COL_USERPARAS);
    }

    String[][] getRspSeqs(String reqName){
        if (null == reqMap.row(reqName)) return null;
        return (String[][]) reqMap.row(reqName).get(COL_RSPSEQ);
    }

    int getTimeout(String reqName){
        if (null == reqMap.row(reqName)) return 0;
        return (int) reqMap.row(reqName).get(COL_TIMEOUT);
    }


    Class<?> getRspClazz(String rspName){
        if (null == rspMap.row(rspName)) return null;
        return (Class<?>) rspMap.row(rspName).get(COL_CLZ);
    }

    int getRspDelay(String rspName){
        if (null == rspMap.row(rspName)) return 0;
        return (int) rspMap.row(rspName).get(COL_DELAY);
    }


    Object[] userPara2MethodPara(Object[] userParas, Class<?>[] methodParaTypes){
        if (null == methodParaTypes || userParas.length < methodParaTypes.length){
            KLog.p(KLog.ERROR, "null == methodParaTypes || userParas.length < methodParaTypes.length");
            return userParas;
        }
        Object[] methodParas = new Object[methodParaTypes.length];
        Object userPara;
        Class<?> methodParaType;
        for (int i=0; i<methodParaTypes.length; ++i){
            userPara = userParas[i];
            methodParaType = methodParaTypes[i];
            KLog.p(KLog.DEBUG,"userPara[%s].class=%s, methodPara[%s].class=%s", i, null==userPara? null : userPara.getClass(), i, methodParaType);
            if (methodParaType.isPrimitive()) {
                if ((userPara instanceof Byte && Byte.TYPE == methodParaType)
                        || (userPara instanceof Character && Character.TYPE == methodParaType)
                        || (userPara instanceof Short && Short.TYPE == methodParaType)
                        || (userPara instanceof Integer && Integer.TYPE == methodParaType)
                        || (userPara instanceof Long && Long.TYPE == methodParaType)
                        || (userPara instanceof Float && Float.TYPE == methodParaType)
                        || (userPara instanceof Double && Double.TYPE == methodParaType)
                        || (userPara instanceof Boolean && Boolean.TYPE == methodParaType)) {
                    methodParas[i] = userPara;
                } else {
                    KLog.p(KLog.ERROR, "try convert userPara %s to methodPara %s failed", null==userPara? null : userPara.getClass(), methodParaTypes[i]);
                    methodParas[i] = userPara;
                }
            }else if (userPara instanceof String && StringBuffer.class == methodParaType){
                methodParas[i] = new StringBuffer((String) userPara);
            } else{
                if (String.class == methodParaType){
                    methodParas[i] = gson.toJson(userPara);
                }else if (StringBuffer.class == methodParaType){
                    methodParas[i] = new StringBuffer(gson.toJson(userPara));
                }else {
                    KLog.p(KLog.DEBUG,"directly assign userPara %s to methodPara %s", userPara.getClass(), methodParaTypes[i]);
                    methodParas[i] = userPara;
                }
            }
        }

        return methodParas;
    }

}

