package com.kedacom.vconf.sdk.base.security;


import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;

import static com.kedacom.vconf.sdk.annotation.Request.*;

@Module(name = "SEC")
enum Msg {

    /**设置是否开启交互式调试*/
    @Request(name = "SetUseOspTelnetCfgCmd",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true启用
            rspSeq = "SetEnableInteractiveDebugRsp"
    )
    SetEnableInteractiveDebug,

    @Response(name = "SetUseOspTelnetCfg_Ntf",
            clz = BaseTypeBool.class)
    SetEnableInteractiveDebugRsp,

    /**判断交互式调试是否已开启*/
    @Request(name = "GetUseOspTelnetCfg",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true已启用
            outputParaIndex = Request.LastIndex
    )
    HasEnabledInteractiveDebug,

}
