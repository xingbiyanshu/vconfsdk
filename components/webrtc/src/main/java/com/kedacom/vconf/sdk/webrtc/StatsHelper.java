package com.kedacom.vconf.sdk.webrtc;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.log.KLog;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Sissi on 2019/12/19
 */
public final class StatsHelper {

    @SuppressWarnings({"ConstantConditions", "SimplifiableConditionalExpression"})
    static void resolveStats(RTCStatsReport rtcStatsReport, @NonNull Stats resolvedStats){

        resolvedStats.clear();

        KLog.p("resolveStats start===========");
        for (Map.Entry<String, RTCStats> rtcStatsEntry : rtcStatsReport.getStatsMap().entrySet()){
            RTCStats rtcStats = rtcStatsEntry.getValue();
            String type = rtcStats.getType();
            Map<String, Object> members = rtcStats.getMembers();

            System.out.println(String.format("statsEntry={key=%s, values={type=%s ",rtcStatsEntry.getKey(), type));
            for (Map.Entry<String, Object> member  : members.entrySet()){
                System.out.println(String.format("member={%s, %s, %s}",member.getKey(), member.getValue(), member.getValue().getClass()));
            }
            System.out.println("}");

            if (type.equals("media-source")) {
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bAudio){
                    AudioSource audioSource = new AudioSource();
                    audioSource.trackIdentifier = null != members.get("trackIdentifier") ? (String) members.get("trackIdentifier") : null;
                    audioSource.audioLevel = null != members.get("audioLevel") ? (double) members.get("audioLevel") : 0;
                    audioSource.totalAudioEnergy = null != members.get("totalAudioEnergy") ? (double) members.get("totalAudioEnergy") : 0;
                    audioSource.totalSamplesDuration = null != members.get("totalSamplesDuration") ? (double) members.get("totalSamplesDuration") : 0;
                    resolvedStats.audioSource = audioSource;
                }else{
                    VideoSource videoSource = new VideoSource();
                    videoSource.trackIdentifier = null != members.get("trackIdentifier") ? (String) members.get("trackIdentifier") : null;
                    videoSource.width = null != members.get("width") ? (long) members.get("width") : 0;
                    videoSource.height = null != members.get("height") ? (long) members.get("height") : 0;
                    videoSource.framesPerSecond = null != members.get("framesPerSecond") ? (long) members.get("framesPerSecond") : 0;
                    resolvedStats.videoSource = videoSource;
                }

            }else if (type.equals("codec")){
                Codec codec = new Codec();
                codec.id = rtcStats.getId();
                codec.payloadType = null != members.get("payloadType") ? (long) members.get("payloadType") : 0;
                codec.mimeType = null != members.get("mimeType") ? (String) members.get("mimeType") : null;
                codec.clockRate = null != members.get("clockRate") ? (long) members.get("clockRate") : 0;
                boolean isDecoder = codec.id.contains("Inbound");
                if (isDecoder){
                    resolvedStats.decoderList.add(codec);
                }else{
                    resolvedStats.encoderList.add(codec);
                }

            }else if (type.equals("track")){
                boolean bRecv = null != members.get("remoteSource") ? (boolean) members.get("remoteSource") : true;
                boolean bAudio = "audio".equals(members.get("kind"));
                String trackIdInRtp = rtcStatsEntry.getKey();
                if (bRecv){
                    if (bAudio){
                        RecvAudioTrack recvAudioTrack = new RecvAudioTrack();
                        recvAudioTrack.id = trackIdInRtp;
                        recvAudioTrack.trackIdentifier = null != members.get("trackIdentifier") ? (String) members.get("trackIdentifier") : null;
                        recvAudioTrack.ended = null != members.get("ended") ? (boolean) members.get("ended") : true;
                        recvAudioTrack.detached = null != members.get("detached") ? (boolean) members.get("detached") : true;
                        recvAudioTrack.jitterBufferDelay = null != members.get("jitterBufferDelay") ? (double) members.get("jitterBufferDelay") : 0;
                        recvAudioTrack.jitterBufferEmittedCount = null != members.get("jitterBufferEmittedCount") ? ((BigInteger) members.get("jitterBufferEmittedCount")).longValue() : 0;
                        recvAudioTrack.audioLevel = null != members.get("audioLevel") ? (double) members.get("audioLevel") : 0;
                        recvAudioTrack.totalAudioEnergy = null != members.get("totalAudioEnergy") ? (double) members.get("totalAudioEnergy") : 0;
                        recvAudioTrack.totalSamplesReceived = null != members.get("totalSamplesReceived") ? ((BigInteger) members.get("totalSamplesReceived")).longValue() : 0;
                        recvAudioTrack.totalSamplesDuration = null != members.get("totalSamplesDuration") ? (double) members.get("totalSamplesDuration") : 0;
                        recvAudioTrack.concealedSamples = null != members.get("concealedSamples") ? ((BigInteger) members.get("concealedSamples")).longValue() : 0;
                        recvAudioTrack.silentConcealedSamples = null != members.get("silentConcealedSamples") ? ((BigInteger) members.get("silentConcealedSamples")).longValue() : 0;
                        recvAudioTrack.concealmentEvents = null != members.get("concealmentEvents") ? ((BigInteger) members.get("concealmentEvents")).longValue() : 0;
                        recvAudioTrack.insertedSamplesForDeceleration = null != members.get("insertedSamplesForDeceleration") ? ((BigInteger) members.get("insertedSamplesForDeceleration")).longValue() : 0;
                        recvAudioTrack.removedSamplesForAcceleration = null != members.get("removedSamplesForAcceleration") ? ((BigInteger) members.get("removedSamplesForAcceleration")).longValue() : 0;
                        recvAudioTrack.jitterBufferFlushes = null != members.get("jitterBufferFlushes") ? ((BigInteger) members.get("jitterBufferFlushes")).longValue() : 0;
                        recvAudioTrack.delayedPacketOutageSamples = null != members.get("delayedPacketOutageSamples") ? ((BigInteger) members.get("delayedPacketOutageSamples")).longValue() : 0;
                        recvAudioTrack.relativePacketArrivalDelay = null != members.get("relativePacketArrivalDelay") ? (double) members.get("relativePacketArrivalDelay") : 0;
                        recvAudioTrack.interruptionCount = null != members.get("interruptionCount") ? (long) members.get("interruptionCount") : 0;
                        recvAudioTrack.totalInterruptionDuration = null != members.get("totalInterruptionDuration") ? (double) members.get("totalInterruptionDuration") : 0;
                        resolvedStats.recvAudioTrackList.add(recvAudioTrack);
                    }else{
                        RecvVideoTrack recvVideoTrack = new RecvVideoTrack();
                        recvVideoTrack.id = trackIdInRtp;
                        recvVideoTrack.trackIdentifier = null != members.get("trackIdentifier") ? (String) members.get("trackIdentifier") : null;
                        recvVideoTrack.ended = null != members.get("ended") ? (boolean) members.get("ended") : true;
                        recvVideoTrack.detached = null != members.get("detached") ? (boolean) members.get("detached") : true;
                        recvVideoTrack.jitterBufferDelay = null != members.get("jitterBufferDelay") ? (double) members.get("jitterBufferDelay") : 0;
                        recvVideoTrack.jitterBufferEmittedCount = null != members.get("jitterBufferEmittedCount") ? ((BigInteger) members.get("jitterBufferEmittedCount")).longValue() : 0;
                        recvVideoTrack.frameWidth = null != members.get("frameWidth") ? (long) members.get("frameWidth") : 0;
                        recvVideoTrack.frameHeight = null != members.get("frameHeight") ? (long) members.get("frameHeight") : 0;
                        recvVideoTrack.framesReceived = null != members.get("framesReceived") ? (long) members.get("framesReceived") : 0;
                        recvVideoTrack.framesDecoded = null != members.get("framesDecoded") ? (long) members.get("framesDecoded") : 0;
                        recvVideoTrack.framesDropped = null != members.get("framesDropped") ? (long) members.get("framesDropped") : 0;
                        recvVideoTrack.freezeCount = null != members.get("freezeCount") ? (long) members.get("freezeCount") : 0;
                        recvVideoTrack.pauseCount = null != members.get("pauseCount") ? (long) members.get("pauseCount") : 0;
                        recvVideoTrack.totalFreezesDuration = null != members.get("totalFreezesDuration") ? (double) members.get("totalFreezesDuration") : 0;
                        recvVideoTrack.totalPausesDuration = null != members.get("totalPausesDuration") ? (double) members.get("totalPausesDuration") : 0;
                        recvVideoTrack.totalFramesDuration = null != members.get("totalFramesDuration") ? (double) members.get("totalFramesDuration") : 0;
                        recvVideoTrack.sumOfSquaredFramesDuration = null != members.get("sumOfSquaredFramesDuration") ? (double) members.get("sumOfSquaredFramesDuration") : 0;
                        resolvedStats.recvVideoTrackList.add(recvVideoTrack);
                    }
                }else{
                    if (bAudio){
                        SendAudioTrack sendAudioTrack = new SendAudioTrack();
                        sendAudioTrack.id = trackIdInRtp;
                        sendAudioTrack.trackIdentifier = null != members.get("trackIdentifier") ? (String) members.get("trackIdentifier") : null;
                        sendAudioTrack.mediaSourceId = null != members.get("mediaSourceId") ? (String) members.get("mediaSourceId") : null;
                        sendAudioTrack.ended = null != members.get("ended") ? (boolean) members.get("ended") : true;
                        sendAudioTrack.detached =  null != members.get("detached") ? (boolean) members.get("detached") : true;
                        resolvedStats.sendAudioTrack = sendAudioTrack;
                    }else{
                        SendVideoTrack sendVideoTrack = new SendVideoTrack();
                        sendVideoTrack.id = trackIdInRtp;
                        sendVideoTrack.trackIdentifier = null != members.get("trackIdentifier") ? (String) members.get("trackIdentifier") : null;
                        sendVideoTrack.mediaSourceId = null != members.get("mediaSourceId") ? (String) members.get("mediaSourceId") : null;
                        sendVideoTrack.ended = null != members.get("ended") ? (boolean) members.get("ended") : true;
                        sendVideoTrack.detached =  null != members.get("detached") ? (boolean) members.get("detached") : true;
                        sendVideoTrack.frameWidth = null != members.get("frameWidth") ? (long) members.get("frameWidth") : 0;
                        sendVideoTrack.frameHeight = null != members.get("frameHeight") ? (long) members.get("frameHeight") : 0;
                        sendVideoTrack.framesSent = null != members.get("framesSent") ? (long) members.get("framesSent") : 0;
                        sendVideoTrack.hugeFramesSent = null != members.get("hugeFramesSent") ? (long) members.get("hugeFramesSent") : 0;
                        resolvedStats.sendVideoTrack = sendVideoTrack;
                    }
                }

            }else if (type.equals("outbound-rtp")){
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bAudio) {
                    AudioOutboundRtp audioOutboundRtp = new AudioOutboundRtp();
                    audioOutboundRtp.ssrc = null != members.get("ssrc") ? (long) members.get("ssrc") : 0;
                    audioOutboundRtp.trackId = null != members.get("trackId") ? (String) members.get("trackId") : null;
                    audioOutboundRtp.transportId = null != members.get("transportId") ? (String) members.get("transportId") : null;
                    audioOutboundRtp.codecId = null != members.get("codecId") ? (String) members.get("codecId") : null;
                    audioOutboundRtp.mediaSourceId = null != members.get("mediaSourceId") ? (String) members.get("mediaSourceId") : null;
                    audioOutboundRtp.packetsSent = null != members.get("packetsSent") ? (long) members.get("packetsSent") : 0;
                    audioOutboundRtp.retransmittedPacketsSent = null != members.get("retransmittedPacketsSent") ? ((BigInteger) members.get("retransmittedPacketsSent")).longValue() : 0;
                    audioOutboundRtp.bytesSent = null != members.get("bytesSent") ? ((BigInteger) members.get("bytesSent")).longValue() : 0;
                    audioOutboundRtp.headerBytesSent = null != members.get("headerBytesSent") ? ((BigInteger) members.get("headerBytesSent")).longValue() : 0;
                    audioOutboundRtp.retransmittedBytesSent = null != members.get("retransmittedBytesSent") ? ((BigInteger) members.get("retransmittedBytesSent")).longValue() : 0;
                    resolvedStats.audioOutboundRtp = audioOutboundRtp;
                }else{
                    VideoOutboundRtp videoOutboundRtp = new VideoOutboundRtp();
                    videoOutboundRtp.ssrc = null != members.get("ssrc") ? (long) members.get("ssrc") : 0;
                    videoOutboundRtp.trackId = null != members.get("trackId") ? (String) members.get("trackId") : null;
                    videoOutboundRtp.transportId = null != members.get("transportId") ? (String) members.get("transportId") : null;
                    videoOutboundRtp.codecId = null != members.get("codecId") ? (String) members.get("codecId") : null;
                    videoOutboundRtp.firCount = null != members.get("firCount") ? (long) members.get("firCount") : 0;
                    videoOutboundRtp.pliCount = null != members.get("pliCount") ? (long) members.get("pliCount") : 0;
                    videoOutboundRtp.nackCount = null != members.get("nackCount") ? (long) members.get("nackCount") : 0;
                    videoOutboundRtp.qpSum = null != members.get("qpSum") ? ((BigInteger) members.get("qpSum")).longValue() : 0;
                    videoOutboundRtp.mediaSourceId = null != members.get("mediaSourceId") ? (String) members.get("mediaSourceId") : null;
                    videoOutboundRtp.packetsSent = null != members.get("packetsSent") ? (long) members.get("packetsSent") : 0;
                    videoOutboundRtp.retransmittedPacketsSent = null != members.get("retransmittedPacketsSent") ? ((BigInteger) members.get("retransmittedPacketsSent")).longValue() : 0;
                    videoOutboundRtp.bytesSent = null != members.get("bytesSent") ? ((BigInteger) members.get("bytesSent")).longValue() : 0;
                    videoOutboundRtp.headerBytesSent = null != members.get("headerBytesSent") ? ((BigInteger) members.get("headerBytesSent")).longValue() : 0;
                    videoOutboundRtp.retransmittedBytesSent = null != members.get("retransmittedBytesSent") ? ((BigInteger) members.get("retransmittedBytesSent")).longValue() : 0;
                    videoOutboundRtp.framesEncoded = null != members.get("framesEncoded") ? (long) members.get("framesEncoded") : 0;
                    videoOutboundRtp.keyFramesEncoded = null != members.get("keyFramesEncoded") ? (long) members.get("keyFramesEncoded") : 0;
                    videoOutboundRtp.totalEncodeTime = null != members.get("totalEncodeTime") ? (double) members.get("totalEncodeTime") : 0;
                    videoOutboundRtp.totalEncodedBytesTarget = null != members.get("totalEncodedBytesTarget") ? ((BigInteger) members.get("totalEncodedBytesTarget")).longValue() : 0;
                    videoOutboundRtp.totalPacketSendDelay = null != members.get("totalPacketSendDelay") ? (double) members.get("totalPacketSendDelay") : 0;
                    videoOutboundRtp.qualityLimitationReason = null != members.get("qualityLimitationReason") ? (String) members.get("qualityLimitationReason") : null;
                    videoOutboundRtp.qualityLimitationResolutionChanges = null != members.get("qualityLimitationResolutionChanges") ? (long) members.get("qualityLimitationResolutionChanges") : 0;
                    videoOutboundRtp.encoderImplementation = null != members.get("encoderImplementation") ? (String) members.get("encoderImplementation") : null;
                    resolvedStats.videoOutboundRtp = videoOutboundRtp;
                }

            }else if (type.equals("inbound-rtp")){
                boolean bAudio = "audio".equals(members.get("kind"));
                if (bAudio) {
                    AudioInboundRtp audioInboundRtp = new AudioInboundRtp();
                    audioInboundRtp.ssrc = null != members.get("ssrc") ? (long) members.get("ssrc") : 0;
                    audioInboundRtp.trackId = null != members.get("trackId") ? (String) members.get("trackId") : null;
                    audioInboundRtp.transportId = null != members.get("transportId") ? (String) members.get("transportId") : null;
                    audioInboundRtp.codecId = null != members.get("codecId") ? (String) members.get("codecId") : null;
                    audioInboundRtp.packetsReceived = null != members.get("packetsReceived") ? (long) members.get("packetsReceived") : 0;
                    audioInboundRtp.bytesReceived = null != members.get("bytesReceived") ? ((BigInteger) members.get("bytesReceived")).longValue() : 0;
                    audioInboundRtp.headerBytesReceived = null != members.get("headerBytesReceived") ? ((BigInteger) members.get("headerBytesReceived")).longValue() : 0;
                    audioInboundRtp.packetsLost = null != members.get("packetsLost") ? (int) members.get("packetsLost") : 0;
                    audioInboundRtp.lastPacketReceivedTimestamp = null != members.get("lastPacketReceivedTimestamp") ? (double) members.get("lastPacketReceivedTimestamp") : 0;
                    audioInboundRtp.jitter = null != members.get("jitter") ? (double) members.get("jitter") : 0;
                    resolvedStats.audioInboundRtpList.add(audioInboundRtp);
                }else{
                    VideoInboundRtp videoInboundRtp = new VideoInboundRtp();
                    videoInboundRtp.ssrc = null != members.get("ssrc") ? (long) members.get("ssrc") : 0;
                    videoInboundRtp.trackId = null != members.get("trackId") ? (String) members.get("trackId") : null;
                    videoInboundRtp.transportId = null != members.get("transportId") ? (String) members.get("transportId") : null;
                    videoInboundRtp.codecId = null != members.get("codecId") ? (String) members.get("codecId") : null;
                    videoInboundRtp.firCount = null != members.get("firCount") ? (long) members.get("firCount") : 0;
                    videoInboundRtp.pliCount = null != members.get("pliCount") ? (long) members.get("pliCount") : 0;
                    videoInboundRtp.nackCount = null != members.get("nackCount") ? (long) members.get("nackCount") : 0;
                    videoInboundRtp.qpSum = null != members.get("qpSum") ? ((BigInteger) members.get("qpSum")).longValue() : 0;
                    videoInboundRtp.packetsReceived = null != members.get("packetsReceived") ? (long) members.get("packetsReceived") : 0;
                    videoInboundRtp.bytesReceived = null != members.get("bytesReceived") ? ((BigInteger) members.get("bytesReceived")).longValue() : 0;
                    videoInboundRtp.headerBytesReceived = null != members.get("headerBytesReceived") ? ((BigInteger) members.get("headerBytesReceived")).longValue() : 0;
                    videoInboundRtp.packetsLost = null != members.get("packetsLost") ? (int) members.get("packetsLost") : 0;
                    videoInboundRtp.lastPacketReceivedTimestamp = null != members.get("lastPacketReceivedTimestamp") ? (double) members.get("lastPacketReceivedTimestamp") : 0;
                    videoInboundRtp.framesDecoded = null != members.get("framesDecoded") ? (long) members.get("framesDecoded") : 0;
                    videoInboundRtp.keyFramesDecoded = null != members.get("keyFramesDecoded") ? (long) members.get("keyFramesDecoded") : 0;
                    videoInboundRtp.totalDecodeTime = null != members.get("totalDecodeTime") ? (double) members.get("totalDecodeTime") : 0;
                    videoInboundRtp.totalInterFrameDelay = null != members.get("totalInterFrameDelay") ? (double) members.get("totalInterFrameDelay") : 0;
                    videoInboundRtp.totalSquaredInterFrameDelay = null != members.get("totalSquaredInterFrameDelay") ? (double) members.get("totalSquaredInterFrameDelay") : 0;
                    videoInboundRtp.decoderImplementation = null != members.get("decoderImplementation") ? (String) members.get("decoderImplementation") : null;
                    resolvedStats.videoInboundRtpList.add(videoInboundRtp);
                }

            }

        }

        KLog.p("resolveStats end===========");
    }



    public static class Stats{
        // send
        public AudioSource audioSource;
        public VideoSource videoSource;
        public SendAudioTrack sendAudioTrack;
        public SendVideoTrack sendVideoTrack;
        public AudioOutboundRtp audioOutboundRtp;
        public VideoOutboundRtp videoOutboundRtp;
        public List<Codec> encoderList = new ArrayList<>();

        //receive
        public List<AudioInboundRtp> audioInboundRtpList = new ArrayList<>();
        public List<VideoInboundRtp> videoInboundRtpList = new ArrayList<>();
        public List<RecvAudioTrack> recvAudioTrackList = new ArrayList<>();
        public List<RecvVideoTrack> recvVideoTrackList = new ArrayList<>();
        public List<Codec> decoderList = new ArrayList<>();

        void clear(){
            audioSource = null;
            videoSource = null;
            sendAudioTrack = null;
            sendVideoTrack = null;
            audioOutboundRtp = null;
            videoOutboundRtp = null;
            audioInboundRtpList.clear();
            videoInboundRtpList.clear();
            recvAudioTrackList.clear();
            recvVideoTrackList.clear();
            encoderList.clear();
            decoderList.clear();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            Stats cloneStats = new Stats();
            cloneStats.audioSource = null != audioSource ? (AudioSource) audioSource.clone() : null;
            cloneStats.videoSource = null != videoSource ? (VideoSource) videoSource.clone() : null;
            cloneStats.sendAudioTrack = null != sendAudioTrack ? (SendAudioTrack) sendAudioTrack.clone() : null;
            cloneStats.sendVideoTrack = null != sendVideoTrack ? (SendVideoTrack) sendVideoTrack.clone() : null;
            cloneStats.audioOutboundRtp = null != audioOutboundRtp ? (AudioOutboundRtp) audioOutboundRtp.clone() : null;
            cloneStats.videoOutboundRtp = null != videoOutboundRtp ? (VideoOutboundRtp) videoOutboundRtp.clone() : null;
            cloneStats.audioInboundRtpList = null != audioInboundRtpList ? new ArrayList<>(audioInboundRtpList) : null;
            cloneStats.videoInboundRtpList = null != videoInboundRtpList ? new ArrayList<>(videoInboundRtpList) : null;
            cloneStats.recvAudioTrackList = null != recvAudioTrackList ? new ArrayList<>(recvAudioTrackList) : null;
            cloneStats.recvVideoTrackList = null != recvVideoTrackList ? new ArrayList<>(recvVideoTrackList) : null;
            cloneStats.encoderList = null != encoderList ? new ArrayList<>(encoderList) : null;
            cloneStats.decoderList = null != decoderList ? new ArrayList<>(decoderList) : null;

            return cloneStats;
        }

        Stats fillSend(Stats stats){
            audioSource = stats.audioSource;
            videoSource = stats.videoSource;
            sendAudioTrack = stats.sendAudioTrack;
            sendVideoTrack = stats.sendVideoTrack;
            audioOutboundRtp = stats.audioOutboundRtp;
            videoOutboundRtp = stats.videoOutboundRtp;
            encoderList = stats.encoderList;
            return this;
        }

        Stats fillRecv(Stats stats){
            audioInboundRtpList = stats.audioInboundRtpList;
            videoInboundRtpList = stats.videoInboundRtpList;
            recvAudioTrackList = stats.recvAudioTrackList;
            recvVideoTrackList = stats.recvVideoTrackList;
            decoderList = stats.decoderList;
            return this;
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "\naudioSource=" + audioSource +
                    ", \nvideoSource=" + videoSource +
                    ", \nsendAudioTrack=" + sendAudioTrack +
                    ", \nsendVideoTrack=" + sendVideoTrack +
                    ", \nrecvAudioTrackList=" + recvAudioTrackList +
                    ", \nrecvVideoTrackList=" + recvVideoTrackList +
                    ", \nencoderList=" + encoderList +
                    ", \ndecoderList=" + decoderList +
                    ", \naudioInboundRtpList=" + audioInboundRtpList +
                    ", \nvideoInboundRtpList=" + videoInboundRtpList +
                    ", \naudioOutboundRtp=" + audioOutboundRtp +
                    ", \nvideoOutboundRtp=" + videoOutboundRtp +
                    '}';
        }
    }


    public static class AudioSource{
        public String trackIdentifier; // 跟webrtc中track.id()方法获取到的一致
        public double audioLevel;
        public double totalAudioEnergy;
        public double totalSamplesDuration;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "AudioSource{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", audioLevel=" + audioLevel +
                    ", totalAudioEnergy=" + totalAudioEnergy +
                    ", totalSamplesDuration=" + totalSamplesDuration +
                    '}';
        }
    }

    public static class VideoSource{
        public String trackIdentifier;  // 跟webrtc中track.id()方法获取到的一致
        public long width;
        public long height;
        public long framesPerSecond;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "VideoSource{" +
                    "trackIdentifier='" + trackIdentifier + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", framesPerSecond=" + framesPerSecond +
                    '}';
        }
    }

    public static class Codec{
        public String id;
        public long payloadType;
        public String mimeType;
        public long clockRate;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "Codec{" +
                    "id='" + id + '\'' +
                    ", payloadType=" + payloadType +
                    ", mimeType='" + mimeType + '\'' +
                    ", clockRate=" + clockRate +
                    '}';
        }
    }

    public static class AudioInboundRtp{
        public long ssrc;
        public String trackId;         // 跟RecvAudioTrack中的id内容一致
        public String transportId;
        public String codecId;
        public long packetsReceived;
        public long bytesReceived;
        public long headerBytesReceived;
        public int packetsLost;
        public double lastPacketReceivedTimestamp;
        public double jitter;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "AudioInboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", packetsReceived=" + packetsReceived +
                    ", bytesReceived=" + bytesReceived +
                    ", headerBytesReceived=" + headerBytesReceived +
                    ", packetsLost=" + packetsLost +
                    ", lastPacketReceivedTimestamp=" + lastPacketReceivedTimestamp +
                    ", jitter=" + jitter +
                    '}';
        }
    }

    public static class VideoInboundRtp{
        public long ssrc;
        public String trackId;         // 跟RecvVideoTrack中的id内容一致
        public String transportId;
        public String codecId;
        public long firCount;
        public long pliCount;
        public long nackCount;
        public long qpSum;
        public long packetsReceived;
        public long bytesReceived;
        public long headerBytesReceived;
        public int packetsLost;
        public double lastPacketReceivedTimestamp;
        public long framesDecoded;
        public long keyFramesDecoded;
        public double totalDecodeTime;
        public double totalInterFrameDelay;
        public double totalSquaredInterFrameDelay;
        public String decoderImplementation;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "VideoInboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", firCount=" + firCount +
                    ", pliCount=" + pliCount +
                    ", nackCount=" + nackCount +
                    ", qpSum=" + qpSum +
                    ", packetsReceived=" + packetsReceived +
                    ", bytesReceived=" + bytesReceived +
                    ", headerBytesReceived=" + headerBytesReceived +
                    ", packetsLost=" + packetsLost +
                    ", lastPacketReceivedTimestamp=" + lastPacketReceivedTimestamp +
                    ", framesDecoded=" + framesDecoded +
                    ", keyFramesDecoded=" + keyFramesDecoded +
                    ", totalDecodeTime=" + totalDecodeTime +
                    ", totalInterFrameDelay=" + totalInterFrameDelay +
                    ", totalSquaredInterFrameDelay=" + totalSquaredInterFrameDelay +
                    ", decoderImplementation='" + decoderImplementation + '\'' +
                    '}';
        }
    }

    public static class AudioOutboundRtp{
        public long ssrc;
        public String trackId;     // 跟SendAudioTrack中的id内容一致
        public String transportId;
        public String codecId;
        public String mediaSourceId;
        public long packetsSent;
        public long retransmittedPacketsSent;
        public long bytesSent;
        public long headerBytesSent;
        public long retransmittedBytesSent;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "AudioOutboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", packetsSent=" + packetsSent +
                    ", retransmittedPacketsSent=" + retransmittedPacketsSent +
                    ", bytesSent=" + bytesSent +
                    ", headerBytesSent=" + headerBytesSent +
                    ", retransmittedBytesSent=" + retransmittedBytesSent +
                    '}';
        }
    }

    public static class VideoOutboundRtp{
        public long ssrc;
        public String trackId;         // 跟SendVideoTrack中的id内容一致
        public String transportId;
        public String codecId;
        public long firCount;
        public long pliCount;
        public long nackCount;
        public long qpSum;
        public String mediaSourceId;
        public long packetsSent;
        public long retransmittedPacketsSent;
        public long bytesSent;
        public long headerBytesSent;
        public long retransmittedBytesSent;
        public long framesEncoded;
        public long keyFramesEncoded;
        public double totalEncodeTime;
        public long totalEncodedBytesTarget;
        public double totalPacketSendDelay;
        public String qualityLimitationReason;
        public long qualityLimitationResolutionChanges;
        public String encoderImplementation;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "VideoOutboundRtp{" +
                    "ssrc=" + ssrc +
                    ", trackId='" + trackId + '\'' +
                    ", transportId='" + transportId + '\'' +
                    ", codecId='" + codecId + '\'' +
                    ", firCount=" + firCount +
                    ", pliCount=" + pliCount +
                    ", nackCount=" + nackCount +
                    ", qpSum=" + qpSum +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", packetsSent=" + packetsSent +
                    ", retransmittedPacketsSent=" + retransmittedPacketsSent +
                    ", bytesSent=" + bytesSent +
                    ", headerBytesSent=" + headerBytesSent +
                    ", retransmittedBytesSent=" + retransmittedBytesSent +
                    ", framesEncoded=" + framesEncoded +
                    ", keyFramesEncoded=" + keyFramesEncoded +
                    ", totalEncodeTime=" + totalEncodeTime +
                    ", totalEncodedBytesTarget=" + totalEncodedBytesTarget +
                    ", totalPacketSendDelay=" + totalPacketSendDelay +
                    ", qualityLimitationReason='" + qualityLimitationReason + '\'' +
                    ", qualityLimitationResolutionChanges=" + qualityLimitationResolutionChanges +
                    ", encoderImplementation='" + encoderImplementation + '\'' +
                    '}';
        }
    }

    public static class SendAudioTrack{
        public String id;          // 跟OutboundRtp中的trackId内容一致
        public String trackIdentifier; // 跟webrtc中track.id()方法获取到的一致
        public String mediaSourceId;
        public boolean ended;
        public boolean detached;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "SendAudioTrack{" +
                    "id='" + id + '\'' +
                    ", trackIdentifier='" + trackIdentifier + '\'' +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    '}';
        }
    }

    public static class SendVideoTrack{
        public String id;          // 跟OutboundRtp中的trackId内容一致
        public String trackIdentifier; // 跟webrtc中track.id()方法获取到的一致
        public String mediaSourceId;
        public boolean ended;
        public boolean detached;
        public long frameWidth;
        public long frameHeight;
        public long framesSent;
        public long hugeFramesSent;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "SendVideoTrack{" +
                    "id='" + id + '\'' +
                    ", trackIdentifier='" + trackIdentifier + '\'' +
                    ", mediaSourceId='" + mediaSourceId + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    ", frameWidth=" + frameWidth +
                    ", frameHeight=" + frameHeight +
                    ", framesSent=" + framesSent +
                    ", hugeFramesSent=" + hugeFramesSent +
                    '}';
        }
    }

    public static class RecvAudioTrack{
        public String id;              // 跟InboundRtp中的trackId内容一致
        public String trackIdentifier; // 跟webrtc中track.id()方法获取到的一致
        public boolean ended;
        public boolean detached;
        public double jitterBufferDelay;
        public long jitterBufferEmittedCount;
        public double audioLevel;
        public double totalAudioEnergy;
        public long totalSamplesReceived;
        public double totalSamplesDuration;
        public long concealedSamples;
        public long silentConcealedSamples;
        public long concealmentEvents;
        public long insertedSamplesForDeceleration;
        public long removedSamplesForAcceleration;
        public long jitterBufferFlushes;
        public long delayedPacketOutageSamples;
        public double relativePacketArrivalDelay;
        public long interruptionCount;
        public double totalInterruptionDuration;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "RecvAudioTrack{" +
                    "id='" + id + '\'' +
                    ", trackIdentifier='" + trackIdentifier + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    ", jitterBufferDelay=" + jitterBufferDelay +
                    ", jitterBufferEmittedCount=" + jitterBufferEmittedCount +
                    ", audioLevel=" + audioLevel +
                    ", totalAudioEnergy=" + totalAudioEnergy +
                    ", totalSamplesReceived=" + totalSamplesReceived +
                    ", totalSamplesDuration=" + totalSamplesDuration +
                    ", concealedSamples=" + concealedSamples +
                    ", silentConcealedSamples=" + silentConcealedSamples +
                    ", concealmentEvents=" + concealmentEvents +
                    ", insertedSamplesForDeceleration=" + insertedSamplesForDeceleration +
                    ", removedSamplesForAcceleration=" + removedSamplesForAcceleration +
                    ", jitterBufferFlushes=" + jitterBufferFlushes +
                    ", delayedPacketOutageSamples=" + delayedPacketOutageSamples +
                    ", relativePacketArrivalDelay=" + relativePacketArrivalDelay +
                    ", interruptionCount=" + interruptionCount +
                    ", totalInterruptionDuration=" + totalInterruptionDuration +
                    '}';
        }
    }

    public static class RecvVideoTrack{
        public String id;      //跟InboundRtp中的trackId内容一致
        public String trackIdentifier; // 跟webrtc中track.id()方法获取到的一致
        public boolean ended;
        public boolean detached;
        public double jitterBufferDelay;
        public long jitterBufferEmittedCount;
        public long frameWidth;
        public long frameHeight;
        public long framesReceived;
        public long framesDecoded;
        public long framesDropped;
        public long freezeCount;
        public long pauseCount;
        public double totalFreezesDuration;
        public double totalPausesDuration;
        public double totalFramesDuration;
        public double sumOfSquaredFramesDuration;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "RecvVideoTrack{" +
                    "id='" + id + '\'' +
                    ", trackIdentifier='" + trackIdentifier + '\'' +
                    ", ended=" + ended +
                    ", detached=" + detached +
                    ", jitterBufferDelay=" + jitterBufferDelay +
                    ", jitterBufferEmittedCount=" + jitterBufferEmittedCount +
                    ", frameWidth=" + frameWidth +
                    ", frameHeight=" + frameHeight +
                    ", framesReceived=" + framesReceived +
                    ", framesDecoded=" + framesDecoded +
                    ", framesDropped=" + framesDropped +
                    ", freezeCount=" + freezeCount +
                    ", pauseCount=" + pauseCount +
                    ", totalFreezesDuration=" + totalFreezesDuration +
                    ", totalPausesDuration=" + totalPausesDuration +
                    ", totalFramesDuration=" + totalFramesDuration +
                    ", sumOfSquaredFramesDuration=" + sumOfSquaredFramesDuration +
                    '}';
        }
    }

}
