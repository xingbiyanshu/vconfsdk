package com.kedacom.vconf.sdk.base.startup;


import com.kedacom.kdv.mt.mtapi.IMtcCallback;
import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.MtLoginMtParam;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.TMTLoginMtResult;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.TNetWorkInfo;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtModel;

import static com.kedacom.vconf.sdk.annotation.Request.*;

@Module(name = "SU")
enum Msg {

    /**设置业务组件工作空间。
     * 其创建的一些配置文件以及日志文件均在该路径下*/
    @Request(name = "SetSysWorkPathPrefix",
            owner = MtcLib,
            paras = StringBuffer.class,
            userParas = String.class  // 工作空间完整路径
    )
    SetMtWorkspace,

    /**
     * 设置是否启用业务组件的日志文件功能。
     * 若启用则业务组件会将日志以文件形式保存在SetMtWorkspace设置的路径下。
     * */
    @Request(name = "SetLogCfgCmd",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = boolean.class // true启用
    )
    MtLogToFile,

    /**启动业务组件基础模块
     * NOTE：调用所有其他业务组件接口前需先等该接口“完成”！
     * */
    @Request(name = "MtStart",
            owner = MtcLib,
            paras = {int.class,
                    StringBuffer.class,
                    StringBuffer.class,
            },
            userParas = {EmMtModel.class, // 终端型号
                    // （登录aps和升级检测也都传了“型号”参数，但那个型号仅用于登录和升级，
                    // 平台需要那样的参数（至于为什么没能做到各场景下统一用一个型号那是平台的问题），
                    // 我们平时所提及的“终端型号”指此处的）
                    String.class, // 终端型号名称（有型号为啥还要型号名称？）
                    String.class, // 终端软件版本号
            }
            )
    StartMtBase,


    /**启动业务组件sdk*/
    @Request(name = "Start",
            owner = MtcLib,
            paras = {boolean.class,
                    boolean.class,
                    StringBuffer.class
            },
            userParas = {boolean.class, // 是否为mtc终端（业务组件将jni层分为两种使用模式：本地和远程，其中远程模式即对应mtc），移动软终端目前始终为非mtc终端。
                    boolean.class, // 是否使用单独的日志文件。NOTE: 仅当终端为mtc模式时，该字段才有效，非mtc模式始终不使用独立日志，不论该字段如何设置。
                    MtLoginMtParam.class // 连接业务组件的access模块，用于跟service模块通信。（业务组件的内部细节）
            },
            rspSeq = "StartMtSdkRsp")
    StartMtSdk,

    /**启动业务组件sdk响应
     * */
    @Response(name = "MTCLoginRsp",
            clz = TMTLoginMtResult.class)
    StartMtSdkRsp,


    /**设置业务组件层回调*/
    @Request(name = "Setcallback",
            owner = MtcLib,
            paras = IMtcCallback.class,
            userParas = IMtcCallback.class // 回调接口
    )
    SetMtSdkCallback,


    /**启用/停用业务组件日志文件功能*/
    @Request(name = "SetLogCfgCmd",
            owner = ConfigCtrl,
            paras = boolean.class,
            userParas = boolean.class // true启用，false停用
    )
    ToggleMtFileLog,

    /**设置网络配置*/
    @Request(name = "SendUsedNetInfoNtf",
            owner = CommonCtrl,
            paras = StringBuffer.class,
            userParas = TNetWorkInfo.class
    )
    SetNetWorkCfg,

}
