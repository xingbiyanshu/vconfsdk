package com.kedacom.vconf.sdk.amulet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.LinkedHashSet;
import java.util.Set;


final class SessionFairy implements IFairy.ISessionFairy{

    private static MagicBook magicBook = MagicBook.instance();

    private ICrystalBall crystalBall;

    // session集合。
    // 使用有序集合以保证会话的时序性。
    private Set<Session> sessions = new LinkedHashSet<>();
    private final Object sessionsLock = new Object();

    private static final int MSG_ID_FinDueToNoRsp = 200;
    private static final int MSG_ID_SESSION_TIMEOUT = 999;

    private static HandlerThread reqThread = new HandlerThread("reqThr", Process.THREAD_PRIORITY_BACKGROUND);
    static {
        reqThread.start();
    }

    private Handler reqHandler = new Handler(reqThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
        }
    };

    private Handler uiHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_ID_SESSION_TIMEOUT) {
                Session s = (Session) msg.obj;
                s.state = Session.END;
                sessions.remove(s);
                Log.d(TAG, String.format("%s <-~- timeout", s.id));
                s.listener.onTimeout(s.reqName, s.reqSn, s.reqPara);
            }else if (msg.what == MSG_ID_FinDueToNoRsp) {
                Session s = (Session) msg.obj;
                sessions.remove(s);
                Log.d(TAG, String.format("%s <-~- no response", s.id));
                s.listener.onFinDueToNoRsp(s.reqName, s.reqSn, s.reqPara);
            }
        }
    };


    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }


    @Override
    public boolean req(IListener listener, String reqName, int reqSn, Object... reqPara) {

        if (null == crystalBall){
            KLog.p(KLog.ERROR, "no crystalBall");
            return false;
        }
        if (null == listener){
            KLog.p(KLog.ERROR, "listener is null");
            return false;
        }
        if (null == reqPara){
            KLog.p(KLog.ERROR, "reqPara is null");
            return false;
        }

        if (!magicBook.isSession(reqName)){
            KLog.p(KLog.ERROR, "no such session request: %s", reqName);
            return false;
        }

        if (!magicBook.checkUserPara(reqName, reqPara)){
            KLog.p(KLog.ERROR,"checkUserPara not pass");
            return false;
        }

        Session s = new Session(listener, reqSn, reqName, reqPara,magicBook.getTimeout(reqName) * 1000, magicBook.getRspSeqs(reqName));
        sessions.add(s);

        // 用户参数转换为底层方法需要的参数
        Object[] paras = magicBook.userPara2MethodPara(s.reqPara, magicBook.getParaClasses(s.reqName));
        StringBuilder sb = new StringBuilder();
        for (Object para : paras) {
            sb.append(para).append(", ");
        }
        String methodName = magicBook.getMethod(s.reqName);

        Log.d(TAG, String.format("%s -~-> %s(%s) \nparas={%s}", s.id, s.reqName, methodName, sb));

        if (null == s.rspSeqs || 0 == s.rspSeqs.length) {
            s.state = Session.END;
            Message msg = Message.obtain();
            msg.what = MSG_ID_FinDueToNoRsp;
            msg.obj = s;
            uiHandler.sendMessage(msg);
        }else {
            s.state = Session.WAITING; // 请求已发出正在等待响应
            // 启动超时
            Message msg = Message.obtain();
            msg.what = MSG_ID_SESSION_TIMEOUT;
            msg.obj = s;
            uiHandler.sendMessageDelayed(msg, s.timeoutVal);
        }

        if (null != crystalBall) {
            reqHandler.post(() -> {
                if (null != crystalBall) {
                    // 调用底层业务组件接口
                    crystalBall.spell(magicBook.getMethodOwner(s.reqName),
                            methodName,
                            paras,
                            magicBook.getParaClasses(s.reqName));
                }
            });
        }

        return true;
    }


    @Override
    public boolean cancelReq(int reqSn) {
        for (Session s : sessions) {
            if (reqSn == s.reqSn) {
                s.state = Session.CANCELED;
                uiHandler.removeMessages(MSG_ID_SESSION_TIMEOUT, s);
                uiHandler.removeMessages(MSG_ID_FinDueToNoRsp, s);

                Log.d(TAG, String.format("%s -~-x %s", s.id, s.reqName));
                // 用户很有可能在onMsg回调中调用cancelReq（onMsg回调到上层的onRsp然后用户在onRsp中cancelReq），
                // 然而onMsg中我们正在遍历sessions，所以我们延迟删除
                uiHandler.post(() -> {
                    if (Session.CANCELED == s.state) {
                        s.state = Session.END;
                        sessions.remove(s);
                    }else{
                        KLog.p(KLog.ERROR, "canceled session %s gone? something must be wrong!", s.id);
                    }
                });
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean onMsg(String msgId, String msgContent) {
        String msgName = magicBook.getRspName(msgId);
        if (null == msgName){
            return false;
        }

        if (!magicBook.isResponse(msgName)){
            return false;
        }

        synchronized (sessionsLock) {
            for (final Session s : sessions) { // 查找期望该响应的会话
                if (!(Session.WAITING == s.state || Session.RECVING == s.state)) {
                    continue;
                }

                SparseIntArray candidates = new SparseIntArray();
                boolean gotLast = false; // 是否匹配到会话的最后一条响应

                for (int i = 0; i < s.candidates.size(); ++i) { // 在候选序列中查找所有能处理该响应的序列。
                    int rspSeqIndx = s.candidates.keyAt(i);
                    int rspIndx = s.candidates.get(rspSeqIndx);
                    String[] candidateRspSeq = s.rspSeqs[rspSeqIndx];
                    String candidateRsp = candidateRspSeq[rspIndx];
                    if (msgName.equals(candidateRsp)) { // 找到了一路匹配的序列
                        candidates.put(rspSeqIndx, rspIndx + 1); // 记录该序列下一个可匹配的响应
                        if (candidateRspSeq.length == rspIndx + 1) { // 该响应为该匹配序列中的最后一条响应
                            gotLast = true;
                        }
                    }

                    if (gotLast) { // 若该响应已匹配到某条响应序列的最后一条响应，则该序列即为最终响应序列，匹配过程结束，该会话结束。
                        break;
                    }
                }

                if (0 == candidates.size()) { // 候选响应序列中未找到该响应
                    continue; // 该响应不是该会话所期望的，继续寻找期望该响应的会话
                }

                @SuppressWarnings("ConstantConditions")
                boolean bConsumed = s.listener.onRsp(gotLast, msgName,
                        Kson.fromJson(msgContent, magicBook.getRspClazz(msgName)),
                        s.reqName, s.reqSn, s.reqPara);

                if (bConsumed) {
                    s.candidates = candidates; // 更新候选序列
                    if (gotLast) {
                        uiHandler.removeMessages(MSG_ID_SESSION_TIMEOUT, s); // 移除定时器
                        s.state = Session.END; // 已获取到所有期待的响应，该会话结束
                        sessions.remove(s);
                        Log.d(TAG, String.format("%s <-~-o %s(%s) \n%s", s.id, msgName, msgId, msgContent));
                    } else {
                        s.state = Session.RECVING; // 已收到响应，继续接收后续响应
                        Log.d(TAG, String.format("%s <-~- %s(%s) \n%s", s.id, msgName, msgId, msgContent));
                    }
                }

                return bConsumed;
            }
        }

        return false;
    }



    /**
     * 会话 */
    private static final class Session{
        private static int count = 0;
        private final int id;   // 会话ID
        private final IListener listener;// 会话监听器
        private final int reqSn;        // 请求序列号。上层用来唯一标识一次请求，会话不使用不处理该字段，上报响应时带回给请求者。
        private final String reqName;   // 请求名称。
        private final Object[] reqPara;   // 请求参数。
        private final int timeoutVal;   // 超时时限。单位：毫秒
        private final String[][] rspSeqs;  // 响应名称序列组。一条请求可能对应多条响应序列，如reqXX——{{rsp1, rsp2},{rsp1,rsp3}}，一次会话只能匹配其中一条序列。
        private SparseIntArray candidates; // 候选的响应序列记录。记录当前可被用来匹配的响应序列组及各响应序列中的下一条待匹配响应（的位置）。“键”对应响应序列组的行下标，“值”对应列下标。每收到一条响应后该记录会更新。

        private int state;  // 会话状态
        private static final int IDLE = 0;  // 空闲。初始状态
        private static final int WAITING = 3; // 等待。请求发送以后，收到第一条响应之前。
        private static final int RECVING = 4; // 接收。收到第一条响应后，收到最后一条响应之前。
        private static final int CANCELED = 6; // 被取消。会话结束前用户取消了该会话。
        private static final int END = 7;   // 结束。

        private Session(IListener listener, int reqSn, String reqName, Object[] reqPara, int timeoutVal, String[][] rspSeqs){
            this.id = count++;
            this.listener = listener;
            this.reqSn = reqSn;
            this.reqName = reqName;
            this.reqPara = reqPara;
            this.timeoutVal = timeoutVal>0 ? timeoutVal : 5*1000;
            this.rspSeqs = rspSeqs;

            candidates = new SparseIntArray(3);
            if (null != rspSeqs) {
                for (int i = 0; i < rspSeqs.length; ++i) {
                    candidates.put(i, 0);
                }
            }
            state = IDLE;
        }

    }

}
