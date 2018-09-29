package com.sissi.vconfsdk.startup;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.base.RequestAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2018/9/12.
 */

public class StartManager extends RequestAgent {

    public void startup(int mode, OnStartupResultListener listener){
        req(Msg.StartupReq, new MsgBeans.StartupInfo(), listener);
    }

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> rspProcessorMap = new HashMap<>();

        rspProcessorMap.put(Msg.StartupReq, this::processLoginResult);

        return rspProcessorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }

    private void processLoginResult(Msg rspId, Object rspContent, Object listener){
        if (Msg.StartupRsp.equals(rspId)){
            if (null != listener){
                ((OnStartupResultListener)listener).onStartupSuccess();
            }
        }
    }


    public interface OnStartupResultListener{
        void onStartupSuccess();
        void onStartupFailed(int errCode);
    }
}
