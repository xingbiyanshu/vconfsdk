package com.kedacom.vconf.sdk.common.type.transfer;

import java.util.List;

public class TMTEntityInfo {

	public TNetAddr tMtAddr; // IP地址
	public EmMtModel emModel; // 型号
	public TMultMtAlias tMtAlias; // 别名，可以多个alias, e164
	public EmMtType emMtType; // 与会终端的类型

	public boolean bAudOnly; // 与会终端是否为为只发送音频码流 0-不只发送音频，1-只发送音频
	public int dwMcuId; // mcu ID
	public int dwTerId; // mt ID
	public List<TMtLoc> atLoc;
	public int byLocCount;
	public long dwAudSsrc; // 音频ssrc
}