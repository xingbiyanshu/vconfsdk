package com.sissi.vconfsdk.base.engine;

import com.sissi.annotation.Consumer;
import com.sissi.annotation.Message;

import java.util.Map;
import java.util.Set;

/**
 * 消息注册表
 *
 * Created by Sissi on 1/9/2017.
 */

@Consumer(Message.class)
final class MessageRegister {
    private static MessageRegister instance;

    private Set<String> reqSet;

    private Set<String> rspSet;

    private Set<String> ntfSet;

    private Set<String> gets;

    private Set<String> sets;

    private Map<String, Class> reqParaMap; // 请求——请求参数对应的类

    private Map<String, String[][]> reqRspsMap; // 请求——响应序列

    private Map<String, Integer> reqTimeoutMap; // 请求——超时时限. 单位: 秒.

    private Map<String, Class> rspClazzMap; // 响应——响应对应的类

    private Map<String, Class> ntfClazzMap; // 通知——通知对应的类

    private Map<String, Class> getParaClazzMap; // 获取参数——参数对应的类

    private Map<String, Class> getResultClazzMap; // 获取结果——结果对应的类

    private Map<String, Class> setParaClazzMap; // 设置参数——参数对应的类
    
//
//    private final EnumSet<EmRsp> whiteList; // 白名单
//    private final EnumSet<EmRsp> blackList; // 黑名单

    private MessageRegister(){
        reqParaMap = Message$$Generated.reqParaMap;
        reqRspsMap = Message$$Generated.reqRspsMap;
        reqTimeoutMap = Message$$Generated.reqTimeoutMap;
        rspClazzMap = Message$$Generated.rspClazzMap;
        ntfClazzMap = Message$$Generated.ntfClazzMap;
        getParaClazzMap = Message$$Generated.getParaClazzMap;
        getResultClazzMap = Message$$Generated.getResultClazzMap;
        setParaClazzMap = Message$$Generated.setParaClazzMap;

        reqSet = reqParaMap.keySet();
        rspSet = rspClazzMap.keySet();
        ntfSet = ntfClazzMap.keySet();
        gets = getParaClazzMap.keySet();
        sets = setParaClazzMap.keySet();
    }

    synchronized static MessageRegister instance() {
        if (null == instance) {
            instance = new MessageRegister();
        }

        return instance;
    }

    boolean isRequest(String msg){
        return reqSet.contains(msg);
    }

    boolean isResponse(String msg){
        return rspSet.contains(msg);
    }

    boolean isNotification(String msg){
        return ntfSet.contains(msg);
    }

    boolean isGet(String msg){
        return gets.contains(msg);
    }

    boolean isSet(String msg){
        return sets.contains(msg);
    }

    int getTimeout(String req){
        Integer timeoutVal = reqTimeoutMap.get(req);
        if (null == timeoutVal) {
            return 0;
        }

        return timeoutVal;
    }

    String[][] getRsps(String req){
        return reqRspsMap.get(req);
    }

    Class<?> getRspClazz(String rsp){
        return rspClazzMap.get(rsp);
    }

    Class<?> getNtfClazz(String ntf){
        return ntfClazzMap.get(ntf);
    }

//    EmRsp getRsp(String rspName){
//        try {
//            return EmRsp.valueOf(rspName);
//        }catch (IllegalArgumentException e){
//            PcTrace.p(PcTrace.WARN, "%s is not a constant in %s", rspName, EmRsp.class.getName());
//            return null;
//        }
//    }

//    Class<?> getConfClazz(EmReq req){
//        return confReqClazzs.get(req);
//    }

//    boolean isInWhiteList(EmRsp rsp){
//        return whiteList.contains(rsp);
//    }
//
//    boolean isInBlackList(EmRsp rsp){
//        return blackList.contains(rsp);
//    }


//    private void initWhiteList(){
//    }
//
//    private void initBlackList(){
//    }

//    /**注册映射关系：获取配置请求——配置对应的类*/
//    private void initConfMap(){
////        map(EmReq.GetCallCapPlusCmd, ReqRspBeans.SetCallCapPlusCmd.class);
//        map(EmReq.GetSkyShareLoginState, ReqRspBeans.SkyShareLoginState.class);
//    }
//

}

