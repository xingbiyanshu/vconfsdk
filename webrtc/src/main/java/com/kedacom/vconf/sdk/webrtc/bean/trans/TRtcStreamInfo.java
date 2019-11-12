package com.kedacom.vconf.sdk.webrtc.bean.trans;

import com.kedacom.vconf.sdk.common.constant.EmMtResolution;

import java.util.List;

/**
 * Created by Sissi on 2019/10/24
 */
public final class TRtcStreamInfo {
    public String             achStreamId;  ///流标识
    public TMtId               tMtId;                               ///属于哪个终端
    public boolean             bAudio;                              ///音频还是视频
    public boolean             bAss;                                ///是不是辅流
    public int                 byMediaIndex;                        ///如果有多路主流， 多流里的索引，暂时主流只有1路，所以为0
    public List<EmMtResolution> aemSimcastRes;       ///流支持的分辨率
    public int                 byResCount;

    @Override
    public String toString() {
        return "TRtcStreamInfo{" +
                "achStreamId='" + achStreamId + '\'' +
                ", tMtId=" + tMtId +
                ", bAudio=" + bAudio +
                ", bAss=" + bAss +
                ", byMediaIndex=" + byMediaIndex +
                ", aemSimcastRes=" + aemSimcastRes +
                ", byResCount=" + byResCount +
                '}';
    }
}
