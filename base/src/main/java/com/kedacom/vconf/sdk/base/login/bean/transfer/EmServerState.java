package com.kedacom.vconf.sdk.base.login.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmServerState {
	emSrvIdle, // 空闲
	emSrvDnsQuerying, // 正在解析IP
	emSrvLogging_In, // 正在登录
	emSrvLogin_Succ, // 登录成功
	emSrvLogging_Out, // 正在登出
	emSrvDisconnected, // 断链（适用于保持心跳的服务器）
	emSrvLogin_Err
}