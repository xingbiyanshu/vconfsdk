package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.bean.transfer.TMtEntityStatus;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInstanceCreateConference;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMTEntityInfoList;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtId;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TMtRtcSvrAddr;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TQueryConfInfoResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcPlayParam;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcStreamInfoList;

/**
 * Created by Sissi on 2019/10/24
 */
@Module(
        name = "RTC"
)
enum Msg {

    /**
     * 启动业务组件服务
     * */
    @Request(name = "SYSStartService",
            owner = Atlas.MtServiceCfgCtrl,
            paras = StringBuffer.class,
            userParas = String.class
    )
    StartMtService,

    @Response(name = "SrvStartResultNtf",
            clz = TSrvStartResult.class)
    StartMtServiceRsp,

    /**获取Rtc服务器地址*/
    @Request(name = "GetRtcSvrCfg",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class,
            isGet = true)
    GetSvrAddr,

    /**
     * 登录Rtc服务器
     */
    @Request(name = "SetRtcSvrCfgCmd",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class, // 登录：TMtRtcSvrAddr.bUsedRtc==true，登出：=false
            timeout = 30,
            rspSeq = "LoginStateChanged")
    Login,

    /**
     * 注销Rtc服务器
     */
    @Request(name = "SetRtcSvrCfgCmd",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class, // 登录：TMtRtcSvrAddr.bUsedRtc==true，登出：=false
            rspSeq = "LoginStateChanged")
    Logout,

    /**
     * 登录状态变更
     */
    @Notification
    @Response(name = "RegResultNtf", clz = TRegResultNtf.class)
    LoginStateChanged,


    /**
     * 呼出
     * */
    @Request(name = "ConfMakeCallCmd",
            owner = Atlas.ConfCtrl,
            paras = {StringBuffer.class, int.class, int.class},
            userParas = {
                    String.class, // 对端e164（点对点）/ 会议号（多点）
                    int.class,  // 呼叫码率
                    EmConfProtocol.class   // 协议类型
            },
            timeout = 60,
            rspSeq = {"Calling", "MultipartyConfStarted"},
            rspSeq2 = {"Calling", "ConfCanceled"}
            )
    Call,

    /**
     * 正在呼出中
     * */
    @Response(clz = TMtCallLinkSate.class,
            name = "ConfCallingNtf")
    Calling,

    /**
     * 多方会议已开始
     * */
    @Response(clz = TMtCallLinkSate.class, name = "MulConfStartedNtf")
    MultipartyConfStarted,

    /**
     * 多方会议已结束
     * */
    @Notification
    @Response(clz = BaseTypeInt.class, // EmMtCallDisReason
            name = "MulConfEndedNtf")
    MultipartyConfEnded,

    /**
     * 会议已取消
     * */
    @Notification
    @Response(clz = BaseTypeInt.class, // EmMtCallDisReason
            name = "ConfCanceledNtf")
    ConfCanceled,

    /**
     * 呼入通知
     * */
    @Notification(clz = TMtCallLinkSate.class, name = "ConfInComingNtf")
    CallIncoming,


    /**
     * 创建会议
     * */
    @Request(name = "MGRestCreateConferenceReq",
            owner = Atlas.MeetingCtrl,
            paras = StringBuffer.class,
            userParas = TMTInstanceCreateConference.class,
            timeout = 60,
            rspSeq = {"CreateConfRsp", // 创会成功与否。创会成功后平台会拉终端入会
                    "MultipartyConfStarted",  // 终端（己端）被成功拉入会议
            },
            rspSeq2 = {"CreateConfRsp",
                    "ConfCanceled",
            }
    )
    CreateConf,

    /**
     * 创建会议响应
     * */
    @Response(clz = TCreateConfResult.class,
            name = "RestCreateConference_Rsp")
    CreateConfRsp,

    /**
     * 退出会议
     * */
    @Request(name = "ConfHangupConfCmd",
            owner = Atlas.ConfCtrl,
            paras = int.class,
            userParas = EmMtCallDisReason.class,
            rspSeq = "MultipartyConfEnded"
    )
    QuitConf,

    /**
     * 结束会议
     * */
    @Request(name = "ConfEndConfCmd",
            owner = Atlas.ConfCtrl,
            rspSeq = "MultipartyConfEnded"
    )
    EndConf,

    /**
     * 接受入会邀请
     * */
    @Request(name = "ConfAcceptCmd",
            owner = Atlas.ConfCtrl,
            timeout = 60,
            rspSeq = "MultipartyConfStarted"
    )
    AcceptInvitation,

    /**
     * 拒绝入会邀请
     * */
    @Request(name = "ConfRejectConfCmd",
            owner = Atlas.ConfCtrl,
            rspSeq = {} //TODO
    )
    DeclineInvitation,


    /**
     * 此与会方标识已分配
     * 入会后平台会为每个与会方分配标识。
     * */
    @Response(clz = TMtId.class,
            name = "TerLabelNtf")
    MyLabelAssigned,


    /**
     * 当前会议中已有与会成员列表通知
     * NOTE: 入会后会收到一次该通知，创会者也会收到这条消息。
     * */
    @Notification(clz = TMTEntityInfoList.class,
            name = "OnLineTerListNtf")
    CurrentConfereeList,

    /**
     * 与会方加入通知
     * 入会以后，会议中有其他与会方加入则会收到该通知。
     * */
    @Notification(clz = TMTEntityInfo.class,
            name = "TerJoin_Ntf")
    ConfereeJoined,

    /**
     * 与会方退出通知
     * 入会以后，会议中有其他与会方离会则会收到该通知
     * */
    @Notification(clz = TMtId.class,
            name = "TerLeft_Ntf")
    ConfereeLeft,



    /**
     * 当前会议中已有音视频流列表通知
     * NOTE:
     * 加入会议后会收到一次该通知
     * 创会者不会收到这条消息。
     * 平台过来的Stream概念上对应的是WebRTC里面的Track
     * */
    @Notification(clz = TRtcStreamInfoList.class,
            name = "RtcStreamList_Ntf")
    CurrentStreamList,

    /**
     * 流加入通知
     * 入会以后，会议中有其他与会方的流加入则会收到该通知。
     * NOTE: 己端不会收到自己的流joined的消息。
     * */
    @Notification(clz = TRtcStreamInfoList.class,
            name = "RtcStreamAdd_Ntf")
    StreamJoined,

    /**
     * 流退出通知
     * 入会以后，会议中有其他与会方的流退出则会收到该通知
     * NOTE:
     * 己端不会收到自己的流left的消息。
     * 对比{@link #ConfereeLeft}，StreamLeft只表示音/视频离会了，而ConfereeLeft表示与会方退会了（当然相应的音视频也退出了）。
     * 比如某个与会方关闭了摄像头停止了视频发布，则其他与会方会收到StreamLeft，但不会收到ConfereeLeft。
     * 如果某个与会方退会了则其他与会方会收到ConfereeLeft和StreamLeft。
     * */
    @Notification(clz = TRtcStreamInfoList.class,
            name = "RtcStreamLeft_Ntf")
    StreamLeft,

    /**
     * 选择想要订阅的视频码流
     */
    @Request(name = "SetRtcPlayCmd",
            owner = Atlas.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcPlayParam.class)
    SelectStream,



    /**获取流列表*/
    @Request(name = "GetRtcStreamList",
            owner = Atlas.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcStreamInfoList.class,
            isGet = true)
    GetStreamList,

    /**
     * 获取流数量
     */
    @Request(name = "GetRtcStreamListNum",
            owner = Atlas.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = int.class,
            isGet = true)
    GetStreamCount,



    /**
     * 静音
     */
    @Request(name = "AudQuiteLocalSpeakerCmd",
            owner = Atlas.AudioCtrl,
            paras = boolean.class,
            rspSeq = "SelfSilenceStateChanged")
    SetSilence,


    /**
     * 哑音
     */
    @Request(name = "AudMuteLocalMicCmd",
            owner = Atlas.AudioCtrl,
            paras = boolean.class,
            rspSeq = "SelfMuteStateChanged")
    SetMute,



    /**
     * 开启/关闭桌面共享（双流）
     * */
    @Request(name = "VideoAssStreamCmd",
            paras = boolean.class,
            owner = Atlas.MonitorCtrl,
            rspSeq = "ToggleScreenShareRsp"
    )
    ToggleScreenShare,

    /**
     * 开启/关闭桌面共享（双流）响应
     * */
    @Response(clz = TMtAssVidStatusList.class,
            name = "AssSndSreamStatusNtf")
    ToggleScreenShareRsp,



    /**
     * 查询会议详情
     * */
    @Request(name = "MGRestGetInstantConfInfoByIDReq",
            owner = Atlas.MeetingCtrl,
            paras = StringBuffer.class,  // 会议e164号
            userParas = String.class,
            timeout = 10,
            rspSeq = "QueryConfInfoRsp"
    )
    QueryConfInfo,

    /**
     * 查询会议详情响应
     * */
    @Response(clz = TQueryConfInfoResult.class,
            name = "RestGetInstantConfInfoByID_Rsp")
    QueryConfInfoRsp,

    /**
     * 此会议需要密码
     * */
    @Notification
    @Response(clz = Void.class, name = "McReqTerPwdNtf")
    ConfPasswordNeeded,

    /**
     * 验证会议密码
     * */
    @Request(name = "ConfVerifyConfPwdCmd",
            owner = Atlas.ConfCtrl,
            paras = StringBuffer.class,  // 会议密码
            userParas = String.class,
            timeout = 10,
            rspSeq = "MyLabelAssigned",     // 验证通过
            rspSeq2 = "ConfPasswordNeeded"  // 验证失败
    )
    VerifyConfPassword,

    /**
     * 关闭己端主流
     * */
    @Request(name = "MainVideoOff",
            owner = Atlas.ConfCtrl
    )
    CloseMyMainVideoChannel,

    /**
     * 己端静音状态变更
     * */
    @Notification
    @Response(name = "CodecQuietNtf",
            clz = BaseTypeBool.class // true已静音
    )
    SelfSilenceStateChanged,

    /**
     * 己端哑音状态变更
     * */
    @Notification
    @Response(name = "CodecMuteNtf",
            clz = BaseTypeBool.class // true已哑音
    )
    SelfMuteStateChanged,

    /**
     * 其他与会方状态变更
     * */
    @Notification(name = "GetTerStatusNtf", clz = TMtEntityStatus.class)
    OtherConfereeStateChanged,


    END;

}