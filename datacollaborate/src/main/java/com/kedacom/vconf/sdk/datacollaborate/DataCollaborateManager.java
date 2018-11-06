package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.preference.PreferenceActivity;

import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.MsgConst;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.ResultCode;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintInfo;

//import static com.kedacom.vconf.sdk.base.MsgBeans.*; // TODO 使用static import？

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class DataCollaborateManager extends RequestAgent {

    // 终端类型
    public static final int Terminal_TrueLinkPc = 1; // 致邻PC版
    public static final int Terminal_TrueLinkIosPhone = 2; // 致邻IOS手机版
    public static final int Terminal_TrueLinkIosPad = 3; // 致邻IOS平板版
    public static final int Terminal_TrueLinkAndroidPhone = 4; // 致邻Android手机版
    public static final int Terminal_TrueLinkAndroidPad = 5; // 致邻Android平板版
    public static final int Terminal_TrueSens = 6; // 硬终端
    public static final int Terminal_Imix = 7; // 网呈IMIX
    public static final int Terminal_Other = 8; // 其他终端

    private IDCPainter painter;

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCLogin, this::onLoginResponses);
        processorMap.put(Msg.DCSCreateConfReq, this::onCreateDcConfResponses);
        return processorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        Map<Msg, NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DcsCurrentWhiteBoard_Ntf, this::onCurrentWhiteBoardNtf);
        processorMap.put(Msg.DcsNewWhiteBoard_Ntf, this::onNewWhiteBoardNtf);
        processorMap.put(Msg.DcsSwitch_Ntf, this::onSwitchWhiteBoardNtf);
        processorMap.put(Msg.DcsElementOperBegin_Ntf, this::onOpBeginNtf);
        processorMap.put(Msg.DcsOperLineOperInfo_Ntf, this::onLineOpNtf);
        processorMap.put(Msg.DcsOperCircleOperInfo_Ntf, this::onCircleOpNtf);
        processorMap.put(Msg.DcsOperRectangleOperInfo_Ntf, this::onRectangleOpNtf);
        processorMap.put(Msg.DcsOperPencilOperInfo_Ntf, this::onPencilOpNtf);
        processorMap.put(Msg.DcsOperInsertPic_Ntf, this::onInsertPicNtf);
        processorMap.put(Msg.DcsOperPitchPicDrag_Ntf, this::onPitchPicNtf);
        processorMap.put(Msg.DcsOperPitchPicDel_Ntf, this::onDelPicNtf);
        processorMap.put(Msg.DcsOperEraseOperInfo_Ntf, this::onEraseOpNtf);
        processorMap.put(Msg.DcsOperFullScreen_Ntf, this::onFullScreenNtf);
        processorMap.put(Msg.DcsOperUndo_Ntf, this::onUndoNtf);
        processorMap.put(Msg.DcsOperRedo_Ntf, this::onRedoNtf);
        processorMap.put(Msg.DcsOperClearScreen_Ntf, this::onClearScreenNtf);
        processorMap.put(Msg.DcsElementOperFinal_Ntf, this::onOpEndNtf);

        return processorMap;
    }


    public void setPainter(IDCPainter painter){
        this.painter = painter;
    }

    public void login(String serverIp, int port, int terminalType, IResultListener resultListener){
        req(Msg.DCLogin, new MsgBeans.TDCSRegInfo(serverIp, port, convertTerminalType(terminalType)), resultListener);
    }

    public void createDcConf(IResultListener resultListener){
        req(Msg.DCSCreateConfReq, new MsgBeans.DCSCreateConf(), resultListener);
    }

    private void onLoginResponses(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.DCBuildLink4LoginRsp.equals(rspId)){
            MsgBeans.DcsLinkCreationResult linkCreationResult = (MsgBeans.DcsLinkCreationResult) rspContent;
            if (!linkCreationResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCLogin, listener);  // 后续不会有DcsLoginSrv_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(ResultCode.FAILED, null);
            }
        }else if (Msg.DCLoginRsp.equals(rspId)){
            MsgBeans.DcsLoginResult loginRes = (MsgBeans.DcsLoginResult) rspContent;
            if (null != listener){
                if (loginRes.bSucces) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
    }


    private void onCreateDcConfResponses(Msg rspId, Object rspContent, IResultListener listener){
        if (Msg.DcsConfResult_Ntf.equals(rspId)){
            MsgBeans.DcsConfResult dcsConfResult = (MsgBeans.DcsConfResult) rspContent;
            if (!dcsConfResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCSCreateConfReq, listener);  // 后续不会有DcsCreateConf_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(ResultCode.FAILED, null);
            }
        }else if (Msg.DcsCreateConf_Rsp.equals(rspId)){
            MsgBeans.TDCSCreateConfResult createConfResult = (MsgBeans.TDCSCreateConfResult) rspContent;
            if (null != listener){
                if (createConfResult.bSuccess) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
    }

    private void onCurrentWhiteBoardNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
    }

    private void onNewWhiteBoardNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onSwitchWhiteBoardNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onOpBeginNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onLineOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperLineOperInfo_Ntf lineOperInfo = (MsgBeans.DcsOperLineOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSWbLine line = lineOperInfo.AssParam.tLine;
            KLog.p("line{left=%s, top=%s, right=%s, bottom=%s}, paint{width=%s, rgb=%s}",
                    line.tBeginPt.nPosx, line.tBeginPt.nPosy, line.tEndPt.nPosx, line.tEndPt.nPosy, line.dwLineWidth, (int) line.dwRgb);
            painter.drawLine(line.tBeginPt.nPosx, line.tBeginPt.nPosy, line.tEndPt.nPosx, line.tEndPt.nPosy,
                    new DCPaintInfo(line.dwLineWidth, (int) line.dwRgb));
        }
    }

    private void onCircleOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperCircleOperInfo_Ntf opInfo = (MsgBeans.DcsOperCircleOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSWbCircle element = opInfo.AssParam.tCircle;
            KLog.p("line{left=%s, top=%s, right=%s, bottom=%s}, paint{width=%s, rgb=%s}",
                    element.tBeginPt.nPosx, element.tBeginPt.nPosy, element.tEndPt.nPosx, element.tEndPt.nPosy, element.dwLineWidth, (int) element.dwRgb);
            painter.drawOval(element.tBeginPt.nPosx, element.tBeginPt.nPosy, element.tEndPt.nPosx, element.tEndPt.nPosy,
                    new DCPaintInfo(element.dwLineWidth, (int) element.dwRgb));
        }
    }

    private void onRectangleOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperRectangleOperInfo_Ntf opInfo = (MsgBeans.DcsOperRectangleOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSWbRectangle element = opInfo.AssParam.tRectangle;
            KLog.p("line{left=%s, top=%s, right=%s, bottom=%s}, paint{width=%s, rgb=%s}",
                    element.tBeginPt.nPosx, element.tBeginPt.nPosy, element.tEndPt.nPosx, element.tEndPt.nPosy, element.dwLineWidth, (int) element.dwRgb);
            painter.drawRect(element.tBeginPt.nPosx, element.tBeginPt.nPosy, element.tEndPt.nPosx, element.tEndPt.nPosy,
                    new DCPaintInfo(element.dwLineWidth, (int) element.dwRgb));
        }
    }

    private void onPencilOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperPencilOperInfo_Ntf opInfo = (MsgBeans.DcsOperPencilOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSWbPencil element = opInfo.AssParam.tPencil;
            MsgBeans.TDCSWbPoint[] points = element.atPList;
            Path path = new Path();
            path.moveTo(points[0].nPosx, points[0].nPosy);
            for (MsgBeans.TDCSWbPoint point: points){
                KLog.p("path.point{%s, %s}", point.nPosx, point.nPosy);
                path.lineTo(point.nPosx, point.nPosy); // 未做起始点判断，多做了一次moveTo，但简化了逻辑
            }
            KLog.p("path.paint{width=%s, rgb=%s}", element.dwLineWidth, (int) element.dwRgb);
            painter.drawPath(path, new DCPaintInfo(element.dwLineWidth, (int) element.dwRgb));
        }
    }

    private void onInsertPicNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onPitchPicNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onDelPicNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onEraseOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onFullScreenNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onUndoNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onRedoNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }


    private void onClearScreenNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }


    private void onOpEndNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }









    private MsgConst.EmDcsType convertTerminalType(int terminal){
        switch (terminal){
            case Terminal_TrueLinkPc:
                return MsgConst.EmDcsType.emTypeTrueLink;
            case Terminal_TrueLinkIosPhone:
                return MsgConst.EmDcsType.emTypeTrueTouchPhoneIOS;
            case Terminal_TrueLinkIosPad:
                return MsgConst.EmDcsType.emTypeTrueTouchPadIOS;
            case Terminal_TrueLinkAndroidPhone:
                return MsgConst.EmDcsType.emTypeTrueTouchPhoneAndroid;
            case Terminal_TrueLinkAndroidPad:
                return MsgConst.EmDcsType.emTypeTrueTouchPadAndroid;
            case Terminal_TrueSens:
                return MsgConst.EmDcsType.emTypeTrueSens;
            case Terminal_Imix:
                return MsgConst.EmDcsType.emTypeIMIX;
            case Terminal_Other:
            default:
                return MsgConst.EmDcsType.emTypeThirdPartyTer;
        }
    }

    public void ejectNtfs(){
        eject(Msg.DcsCurrentWhiteBoard_Ntf);
        eject(Msg.DcsNewWhiteBoard_Ntf);
        eject(Msg.DcsSwitch_Ntf);
        eject(Msg.DcsElementOperBegin_Ntf);
        eject(Msg.DcsOperCircleOperInfo_Ntf);
        eject(Msg.DcsOperLineOperInfo_Ntf);
        eject(Msg.DcsOperRectangleOperInfo_Ntf);
        eject(Msg.DcsOperPencilOperInfo_Ntf);
        eject(Msg.DcsOperInsertPic_Ntf);
        eject(Msg.DcsOperPitchPicDrag_Ntf);
        eject(Msg.DcsOperPitchPicDel_Ntf);
        eject(Msg.DcsOperEraseOperInfo_Ntf);
        eject(Msg.DcsOperUndo_Ntf);
        eject(Msg.DcsOperRedo_Ntf);
        eject(Msg.DcsOperClearScreen_Ntf);
        eject(Msg.DcsElementOperFinal_Ntf);
    }

//    private DataCollaborateManager(){
////        Timer timer = new Timer();
////        timer.schedule(new TimerTask() {
////            @Override
////            public void run() {
////                DataCollaborateManager.this.ejectNtfs();
////            }
////        }, 5000, 5000);
//        new Handler().postDelayed(this::ejectNtfs, 5000);
//    }

}
