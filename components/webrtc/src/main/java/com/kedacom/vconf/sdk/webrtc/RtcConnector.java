
package com.kedacom.vconf.sdk.webrtc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.kedacom.kdv.mt.ospconnector.Connector;
import com.kedacom.kdv.mt.ospconnector.IRcvMsgCallback;
import com.kedacom.mt.netmanage.protobuf.BasePB;
import com.kedacom.mt.netmanage.protobuf.EnumPB;
import com.kedacom.mt.netmanage.protobuf.StructCommonPB;
import com.kedacom.mt.netmanage.protobuf.StructConfPB;
import com.kedacom.osp.BodyItem;
import com.kedacom.osp.EmMtOspMsgSys;
import com.kedacom.osp.MtMsg;
import com.kedacom.vconf.sdk.common.constant.EmAudFormat;
import com.kedacom.vconf.sdk.common.constant.EmVidFormat;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.webrtc.bean.Statistics;

import org.webrtc.RtpParameters;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class RtcConnector implements IRcvMsgCallback{

	private static final String TAG = "RtcConnector";
	private static final String WEBRTC_NAME = "WEBRTC_NAME";
	private static final short WEBRTC_ID = 144;
	private static final short MTDISPATCH_ID = 107;
	private static final short MTRTCSERVICE_ID = 145;
	private static final short GUARD_ID = 109;
	private final Map<String, ICbMsgHandler> cbMsgHandlerMap = new HashMap<>();

	private final long myId =Connector.MAKEIID(WEBRTC_ID, (short)1 );
    private final long myNode = 0;
    private final long dispatchId = Connector.MAKEIID(MTDISPATCH_ID, (short)1 );
	private final long dispatchNode = 0;

	private final long mtrtcserviceId = Connector.MAKEIID(MTRTCSERVICE_ID, (short)1 );
	private final long mtrtcserviceNode = 0;

	private final long guardId = Connector.MAKEIID(GUARD_ID, (short)1 );
	private final long guardNode = 0;


	private Handler handler = new Handler(Looper.getMainLooper());

	// 因受osp单次可发送数据长度的限制，sdp可能需要分段发送，需要先缓存下来后拼接，拼接完成后decode。
	private ByteArrayOutputStream offerBuf;
	private ByteArrayOutputStream answerBuf;
	private static final int SegLengthLimit = 50000; // 每段的长度上限。

	private Listener listener;
	void setSignalingEventsCallback(Listener listener){
		this.listener = listener;
	}

	RtcConnector( ) {
		// 注册消息处理方法
		cbMsgHandlerMap.put("Ev_MT_GetOffer_Cmd", this::onGetOfferCmd);
		cbMsgHandlerMap.put("Ev_MT_SetOffer_Cmd", this::onSetOfferCmd);
		cbMsgHandlerMap.put("Ev_MT_SetAnswer_Cmd", this::onSetAnswerCmd);
		cbMsgHandlerMap.put("Ev_MT_SetIceCandidate_Cmd", this::onSetIceCandidateCmd);
		cbMsgHandlerMap.put("Ev_MT_GetFingerPrint_Cmd", this::onGetFingerPrintCmd);
		cbMsgHandlerMap.put("Ev_MT_UnPub_Cmd", this::onUnPubCmd);
		cbMsgHandlerMap.put("Ev_MT_CodecQuiet_Cmd", this::onCodecQuietCmd);
		cbMsgHandlerMap.put("Ev_MT_CodecMute_Cmd", this::onCodecMuteCmd);
		cbMsgHandlerMap.put("Ev_MT_Agent_RtcCodecStatistic_Req", this::onAgentRtcCodecStatisticReq);

		if (!CreateOspObject()){
			throw new RuntimeException("CreateOspObject failed!");
		}

		// 向业务组件订阅消息
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_Subscribe_Cmd");
		StructCommonPB.TSubsMsgID.Builder subsMsgBuilder = StructCommonPB.TSubsMsgID.newBuilder();
		subsMsgBuilder.addAllMsgid(cbMsgHandlerMap.keySet());
		msg.addMsg(subsMsgBuilder.build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			throw new RuntimeException("Register msg failed!");
		}

	}

	void destroy(){
		Connector.RelaseOspConnector();
	}


	private boolean CreateOspObject() {
		boolean bOspInited = Connector.IsOspConnectorInitd();

		if ( !bOspInited )
		{
			boolean bInited = Connector.OspConnectorInit( true , (short) 25000, "webrtc" );
			if (!bInited){
				KLog.p(KLog.ERROR, "ospinit failed");
				return false;
			}
		}

		boolean bSuccess = Connector.CreateOspConnector(
				WEBRTC_NAME, // app名
				WEBRTC_ID, // appID
				(byte)80, // 优先级
				(short)300, // 消息队列大小
				2000, // 堆栈大小
				0 // 保留参数
		);
		if (!bSuccess){
			KLog.p(KLog.ERROR, "CreateOspConnector failed");
			return false;
		}

		Connector.Setcallback(this);

		return true;
	}


	@Override
	public void Callback( long nEvent, byte[] abyContent, short wLen, long nDstIId, long nDstNode, long nSrcIId, long nSrcNode ) {

		KLog.p("mtrtcCallback event=%s", nEvent);

		MtMsg mtMsg = new MtMsg();
		
		if ( nEvent == EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal() )
		{
			// PB消息处理
			boolean bRet = mtMsg.Decode( abyContent );
			if ( !bRet ) {
				KLog.p(KLog.ERROR, " mtmsg decode failed" );
				return;
			}
		}
		else if ( nEvent == EmMtOspMsgSys.EV_MtOsp_OSP_DISCONNECT.getnVal() )
		{
			// OSP断链检测消息处理
			KLog.p(KLog.WARN, " mtdispatch disconnected" );
			mtMsg.SetMsgId( EmMtOspMsgSys.EV_MtOsp_OSP_DISCONNECT.name() );
		}
		else
		{
			KLog.p(TAG, " dismiss osp message + eventid: " + nEvent);
			return;
		}

		ICbMsgHandler cbMsgHandler = cbMsgHandlerMap.get(mtMsg.GetMsgId());
		if (null == cbMsgHandler){
			KLog.p(KLog.ERROR, "no handler for msg %s", mtMsg.GetMsgId());
			return;
		}

//		KLog.p("msg=%s, srcId=%s, srcNode=%s", mtMsg, nSrcIId, nSrcNode);
		cbMsgHandler.onMsg(mtMsg, nSrcIId, nSrcNode);
	}


	private interface ICbMsgHandler{
		void onMsg(MtMsg mtMsg, long nSrcId, long nSrcNode);
	}


	/**
	 * 收到平台“发起signaling”的指示，主动发起signaling流程
	 * */
	private void onGetOfferCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		int connType;
		int mediaType;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			mediaType = BasePB.TU32.parseFrom(item1.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		Log.i(TAG, String.format("[PUB]<=#= onGetOfferCmd, connType=%s, mediaType=%s", connType, mediaType));

		handler.post(() -> {
			if (null != listener) {
				listener.onGetOfferCmd(connType, mediaType);
			}
		});

	}


	/**
	 * 收到对端的offer，被动开始signaling流程
	 * */
	private void onSetOfferCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		int connType;
		String offer;
		StructConfPB.TRtcMedialist rtcMedialist;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			offer = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			rtcMedialist = StructConfPB.TRtcMedialist.parseFrom(item2.getAbyContent());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		Log.i(TAG, String.format("[sub]<=#= onSetOfferCmd, connType=%s, offer=", connType));
		KLog.p(offer);

		List<TRtcMedia> rtcMedias = new ArrayList<>();
		for (StructConfPB.TRtcMedia tRtcMedia : rtcMedialist.getMediaList()){
			rtcMedias.add(rtcMediaFromPB(tRtcMedia));
		}
		handler.post(() -> {
			if (null != listener) {
				listener.onSetOfferCmd(connType, offer, rtcMedias);
			}
		});

	}


	/**
	 * 收到对端的answer
	 * */
	private void onSetAnswerCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		int connType;
		String answer;
		StructConfPB.TRtcMedialist rtcMedialist;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			answer = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			rtcMedialist = StructConfPB.TRtcMedialist.parseFrom(item2.getAbyContent());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		Log.i(TAG, String.format("[PUB]<=#= onSetAnswerCmd, connType=%s, answer=", connType));
		KLog.p(answer);

		List<TRtcMedia> rtcMedias = new ArrayList<>();
		for (StructConfPB.TRtcMedia tRtcMedia : rtcMedialist.getMediaList()){
			rtcMedias.add(rtcMediaFromPB(tRtcMedia));
		}

		handler.post(() -> {
			if (null != listener) {
				listener.onSetAnswerCmd(connType, answer, rtcMedias);
			}
		});

	}


	/**
	 * 收到对端的candidate
	 * */
	private void onSetIceCandidateCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		BodyItem item2 = mtMsg.GetMsgBody(2);
		BodyItem item3 = mtMsg.GetMsgBody(3);
		int connType;
		String mid;
		int index;
		String candidate;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			mid = BasePB.TString.parseFrom(item1.getAbyContent()).getValue();
			index = BasePB.TU32.parseFrom(item2.getAbyContent()).getValue();
			candidate = BasePB.TString.parseFrom(item3.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		Log.i(TAG, String.format("<=#= connType=%s, mid=%s, index=%s, candidate=", connType, mid, index));
		KLog.p(candidate);
		handler.post(() -> {
			if (null != listener) {
				listener.onSetIceCandidateCmd(connType, mid, index, candidate);
			}
		});
	}


	/**
	 * 获取指纹。
	 * 业务组件定的流程：
	 * 组件先向界面获取FingerPrint，然后向界面推送StreamListNtf，
	 * 界面setPlayPara，然后组件推onSetOfferCmd给界面开始订阅
	 * */
	private void onGetFingerPrintCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		int connType;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("[sub]<=#= connType=%s", connType);
		handler.post(() -> {
			if (null != listener) {
				listener.onGetFingerPrintCmd(connType);
			}
		});
	}


	/**
	 * 静音
	 * */
	private void onCodecQuietCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		boolean bQuiet;
		try {
			bQuiet = BasePB.TBOOL32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("bQuiet=%s", bQuiet);
		handler.post(() -> {
			if (null != listener) {
				listener.onCodecQuietCmd(bQuiet);
			}
		});
	}


	/**
	 * 哑音
	 * */
	private void onCodecMuteCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		boolean bMute;
		try {
			bMute = BasePB.TBOOL32.parseFrom(item0.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		KLog.p("bMute=%s", bMute);
		handler.post(() -> {
			if (null != listener) {
				listener.onCodecMuteCmd(bMute);
			}
		});
	}


	/**
	 * 取消发布
	 * */
	private void onUnPubCmd(MtMsg mtMsg, long nSrcId, long nSrcNode){
		BodyItem item0 = mtMsg.GetMsgBody(0);
		BodyItem item1 = mtMsg.GetMsgBody(1);
		int connType;
		int mediaType;
		try {
			connType = BasePB.TU32.parseFrom(item0.getAbyContent()).getValue();
			mediaType = BasePB.TU32.parseFrom(item1.getAbyContent()).getValue();
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return;
		}

		Log.i(TAG, String.format("[UNPUB]<=#= onUnPubCmd, connType=%s, mediaType=%s", connType, mediaType));

		handler.post(() -> {
			if (null != listener) {
				listener.onUnPubCmd(connType, mediaType);
			}
		});
	}


	/**
	 * 请求上报编解码信息
	 * */
	private void onAgentRtcCodecStatisticReq(MtMsg mtMsg, long nSrcId, long nSrcNode){
		Log.i(TAG, "<=#= onAgentRtcCodecStatisticReq");
		handler.post(() -> {
			if (null != listener) {
				listener.onAgentRtcCodecStatisticReq();
			}
		});
	}


	private EnumPB.EmMtResolution convertRes(double scale){
		if (scale < 0.5){
			return EnumPB.EmMtResolution.emMt320x180;
		}else if (0.5 <= scale && scale < 0.75){
			return EnumPB.EmMtResolution.emMt640x360;
		}else if (0.75 <= scale && scale < 1){
			return EnumPB.EmMtResolution.emMt960x540;
		}else{
			return EnumPB.EmMtResolution.emMt1280x720;
		}
	}

	private TRtcMedia rtcMediaFromPB(StructConfPB.TRtcMedia pbRtcMedia){
		List<RtpParameters.Encoding> encodings = new ArrayList<>();
		for (StructConfPB.TRtcRid tRtcRid : pbRtcMedia.getRidlistList()){
			String rid = tRtcRid.getRid();
//			encodings.add(PeerConnectionClient.createEncoding(rid,
//					1 // encodings不用处理
//			));
		}
		return new TRtcMedia(pbRtcMedia.getStreamid(), pbRtcMedia.getMid());
	}

	private StructConfPB.TRtcMedia rtcMediaToPB(TRtcMedia rtcMedia){
		StructConfPB.TRtcMedia.Builder rtcMediaBuilder = StructConfPB.TRtcMedia.newBuilder();
		if (null != rtcMedia.encodings){
			for (RtpParameters.Encoding encoding : rtcMedia.encodings){
				StructConfPB.TRtcRid.Builder ridBuider = StructConfPB.TRtcRid.newBuilder();
				ridBuider
//						.setRid(encoding.rid)
						.setEmres(convertRes(encoding.scaleResolutionDownBy))
						.setBitrate(encoding.maxBitrateBps);
				rtcMediaBuilder.addRidlist(ridBuider.build());
			}
		}
		rtcMediaBuilder.setMid(rtcMedia.mid);
		rtcMediaBuilder.setStreamid(rtcMedia.streamid);
		return rtcMediaBuilder.build();
	}

	private StructConfPB.TAgentCodecStatistic statistics2PB(Statistics statistics){
		StructConfPB.TAgentCodecStatistic.Builder builder = StructConfPB.TAgentCodecStatistic.newBuilder();
		if (statistics.common != null){
			Statistics.AudioInput audioInput = statistics.common.mixedAudio;
			StructConfPB.TAgentAudDecStatistic.Builder audDecBuilder = StructConfPB.TAgentAudDecStatistic.newBuilder();
			audDecBuilder.setBitrate(audioInput.bitrate);
			audDecBuilder.setFormat(audEncodeFormat2PB(audioInput.encodeFormat));
			audDecBuilder.setPktsLose((int) audioInput.packetsLost);
			audDecBuilder.setPktsLoserate((int) (100 * audioInput.packetsLost/(audioInput.packetsReceived+audioInput.packetsLost)));
			audDecBuilder.setDecStart(true);
			audDecBuilder.setIndex(0);
			builder.addAuddecStatics(audDecBuilder.build());
		}
		for (Statistics.ConfereeRelated confereeRelated : statistics.confereeRelated) {
			if (confereeRelated.audioOutput!=null){
				StructConfPB.TAgentAudEncStatistic.Builder audEncBuilder = StructConfPB.TAgentAudEncStatistic.newBuilder();
				audEncBuilder.setBitrate(confereeRelated.audioOutput.bitrate);
				audEncBuilder.setFormat(audEncodeFormat2PB(confereeRelated.audioOutput.encodeFormat));
				audEncBuilder.setEncStart(true);
				audEncBuilder.setIndex(0);
				builder.addAudencStatics(audEncBuilder.build());
			}

			if (confereeRelated.audioInput !=null){
				StructConfPB.TAgentAudDecStatistic.Builder audDecBuilder = StructConfPB.TAgentAudDecStatistic.newBuilder();
				Statistics.AudioInput audioInput = confereeRelated.audioInput;
				audDecBuilder.setBitrate(audioInput.bitrate);
				audDecBuilder.setFormat(audEncodeFormat2PB(audioInput.encodeFormat));
				audDecBuilder.setPktsLose((int) audioInput.packetsLost);
				audDecBuilder.setPktsLoserate((int) (100 * audioInput.packetsLost/(audioInput.packetsReceived+audioInput.packetsLost)));
				audDecBuilder.setDecStart(true);
				audDecBuilder.setIndex(0);
				builder.addAuddecStatics(audDecBuilder.build());
			}

			if (confereeRelated.videoOutput != null){
				StructConfPB.TAgentVidEncStatistic.Builder vidEncBuilder = StructConfPB.TAgentVidEncStatistic.newBuilder();
				vidEncBuilder.setBitrate(confereeRelated.videoOutput.bitrate);
				vidEncBuilder.setFormat(vidEncodeFormat2PB(confereeRelated.videoOutput.encodeFormat));
				vidEncBuilder.setFramerate(confereeRelated.videoOutput.framerate);
				vidEncBuilder.setVidWidth(confereeRelated.videoOutput.width);
				vidEncBuilder.setVidHeight(confereeRelated.videoOutput.height);
				vidEncBuilder.setHwEncStatus(true);
				vidEncBuilder.setEncStart(true);
				vidEncBuilder.setIndex(0);
				if (confereeRelated.confereeId.endsWith(WebRtcManager.Conferee.ConfereeType.AssStream.name())) {
					builder.addAssVidencStatics(vidEncBuilder.build());
				}else {
					builder.addPriVidencStatics(vidEncBuilder.build());
				}
			}

			if (confereeRelated.videoInput != null){
				StructConfPB.TAgentVidDecStatistic.Builder vidDecBuilder = StructConfPB.TAgentVidDecStatistic.newBuilder();
				Statistics.VideoInput videoInput = confereeRelated.videoInput;
				vidDecBuilder.setBitrate(videoInput.bitrate);
				vidDecBuilder.setFormat(vidEncodeFormat2PB(videoInput.encodeFormat));
				vidDecBuilder.setFramerate(videoInput.framerate);
				vidDecBuilder.setVidWidth(videoInput.width);
				vidDecBuilder.setVidHeight(videoInput.height);
				vidDecBuilder.setPktsLose((int) videoInput.packetsLost);
				vidDecBuilder.setPktsLoserate((int) (100 * videoInput.packetsLost/(videoInput.packetsReceived+videoInput.packetsLost)));
				vidDecBuilder.setHwDecStatus(true);
				vidDecBuilder.setDecStart(true);
				vidDecBuilder.setIndex(0);
				if (confereeRelated.confereeId.endsWith(WebRtcManager.Conferee.ConfereeType.AssStream.name())) {
					builder.addAssViddecStatics(vidDecBuilder.build());
				}else {
					builder.addPriViddecStatics(vidDecBuilder.build());
				}
			}
		}

		return builder.build();
	}

	private int vidEncodeFormat2PB(String format){
		switch (format){
			case Statistics.H264:
				return EmVidFormat.emVH264.ordinal();
			default:
				return EmVidFormat.emVEnd.ordinal();
		}
	}

	private int audEncodeFormat2PB(String format){
		switch (format){
			case Statistics.OPUS:
				return EmAudFormat.emAOpus.ordinal();
			case Statistics.G722:
				return EmAudFormat.emAG722.ordinal();
			default:
				return EmAudFormat.emAudEnd.ordinal();
		}
	}

	void sendOfferSdp(int connType, @NonNull String offerSdp, TRtcMedia... rtcMediaList) {
        StructConfPB.TRtcMedialist.Builder builder = StructConfPB.TRtcMedialist.newBuilder();
        for (TRtcMedia rtcMedia : rtcMediaList){
            builder.addMedia(rtcMediaToPB(rtcMedia));
        }
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_GetOffer_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(offerSdp).build());
		msg.addMsg(builder.build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		Log.i(TAG, String.format("[PUB]=#=> sendOfferSdp, connType=%s, offerSdp=", connType));
		KLog.p(offerSdp);
	}

	void sendAnswerSdp(int connType, @NonNull String answerSdp, List<TRtcMedia> rtcMediaList) {
		StructConfPB.TRtcMedialist.Builder builder = StructConfPB.TRtcMedialist.newBuilder();
		for (TRtcMedia rtcMedia : rtcMediaList){
			builder.addMedia(rtcMediaToPB(rtcMedia));
		}
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_GetAnswer_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(answerSdp).build());
		msg.addMsg(builder.build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );

		Log.i(TAG, String.format("[sub]=#=> sendAnswerSdp, connType=%s, answerSdp=", connType));
		KLog.p(answerSdp);
        if (0 != ret){
            KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
        }

	}


	// 发送IceCandidate给对端
	void sendIceCandidate(int connType, @NonNull String sdpMid, int sdpMLineIndex, @NonNull String sdp) {
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_IceCandidate_Ntf");
		msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(sdpMid).build());
		msg.addMsg(BasePB.TU32.newBuilder().setValue(sdpMLineIndex).build());
		msg.addMsg(BasePB.TString.newBuilder().setValue(sdp).build());
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				dispatchId, dispatchNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		Log.i(TAG, String.format("=#=> sendIceCandidate, sdpmid=%s, sdpline=%s, candidate=", sdpMid, sdpMLineIndex));
		KLog.p(sdp);
	}


	void sendFingerPrint(int connType, @NonNull String fingerPrint){
        MtMsg msg = new MtMsg();
        msg.SetMsgId("Ev_MT_FingerPrint_Ntf");
        msg.addMsg(BasePB.TU32.newBuilder().setValue(connType).build());
        msg.addMsg(BasePB.TString.newBuilder().setValue(fingerPrint).build());
        byte[] abyContent = msg.Encode();
        int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
                dispatchId, dispatchNode, myId, myNode, 5000 );
        if (0 != ret){
            KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
        }

        KLog.p("[sub]=#=> sendFingerPrint connType=%s, fingerPrint=%s", connType, fingerPrint);
	}


	void sendStatistics(@NonNull Statistics stats){
		MtMsg msg = new MtMsg();
		msg.SetMsgId("Ev_MT_Agent_RtcCodecStatistic_Rsp");
		msg.addMsg(statistics2PB(stats));
		byte[] abyContent = msg.Encode();
		int ret = Connector.PostOspMsg( EmMtOspMsgSys.Ev_MtOsp_ProtoBufMsg.getnVal(), abyContent, abyContent.length,
				guardId, guardNode, myId, myNode, 5000 );
		if (0 != ret){
			KLog.p(KLog.ERROR, "PostOspMsg %s failed", msg.GetMsgId());
		}

		KLog.p("=#=> sendStatistics %s", stats);
	}

	static class TRtcMedia {
		String streamid="";
		String mid;
		List<RtpParameters.Encoding> encodings;

		TRtcMedia(String streamid, String mid, List<RtpParameters.Encoding> encodings) {
			this.streamid = streamid;
			this.mid = mid;
			this.encodings = encodings;
		}
		TRtcMedia(String streamid, String mid) {
			this.streamid = streamid;
			this.mid = mid;
		}
		TRtcMedia(String mid) {
			this.mid = mid;
		}
		TRtcMedia(String mid, List<RtpParameters.Encoding> encodings) {
			this.mid = mid;
			this.encodings = encodings;
		}
	}

	/**
	 * 事件监听器。
	 * 所有回调均从主线程
	 * */
	interface Listener {

		void onGetOfferCmd(int connType, int mediaType);

		void onSetOfferCmd(int connType, String offerSdp, List<TRtcMedia> rtcMediaList);

		void onSetAnswerCmd(int connType, String answerSdp, List<TRtcMedia> rtcMediaList);

		void onSetIceCandidateCmd(int connType, String sdpMid, int sdpMLineIndex, String sdp);

		void onGetFingerPrintCmd(int connType);

		default void onCodecQuietCmd(boolean bQuiet){}

		default void onCodecMuteCmd(boolean bMute){}

		void onUnPubCmd(int connType, int mediaType);

		void onAgentRtcCodecStatisticReq();
	}


}

 