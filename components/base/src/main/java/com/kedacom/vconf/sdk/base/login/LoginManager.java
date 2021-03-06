package com.kedacom.vconf.sdk.base.login;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.INtfListener;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.login.bean.UserDetails;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerAddrType;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerState;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerType;
import com.kedacom.vconf.sdk.base.login.bean.transfer.MtXAPSvrCfg;
import com.kedacom.vconf.sdk.base.login.bean.transfer.MtXAPSvrListCfg;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TApsLoginResult;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TLoginPlatformRsp;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMTAccountManagerSystem;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMTApsLoginParam;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMTUserInfoFromAps;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMTWeiboLogin;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtPlatformApiAddr;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrState;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrStateList;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TQueryUserDetailsRsp;
import com.kedacom.vconf.sdk.common.type.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.type.transfer.TRestErrorInfo;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LoginManager extends Caster<Msg> {
    private static LoginManager instance = null;
    private Application context;

    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    private static ExecutorService executor = Executors.newCachedThreadPool();

    private boolean loggedIn;

    private LoginManager(Application ctx) {
        context = ctx;
    }

    public synchronized static LoginManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new LoginManager(ctx);
            instance.startService();
        }
        return instance;
    }

    // 启动业务组件接入服务
    private void startService(){
        // 启动服务过程中该模块其它请求禁止下发
        disableReq(true);

        String serviceName = "rest";
        req(false, true, Msg.StartMtService, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                // 取消禁令
                disableReq(false);

                TSrvStartResult result = (TSrvStartResult) rspContent;
                if (!result.AssParam.achSysalias.equals(serviceName)){
                    isConsumed[0] = false;
                    return;
                }
                boolean success = result.MainParam.basetype;
                if (success){
                    KLog.p("start service %s success!", serviceName);
                }
            }
        }, null , serviceName);

    }


    /**
     * 登录APS
     * NOTE: APS为接入服务器，登录APS后才能登录其他服务器如会议、IM、升级等。
     * @param apsAddr APS服务器地址。可以为domain或者IP。
     * @param username 用户名
     * @param password 密码
     * @param resultListener 结果监听器
     *                       成功返回null，
     *                       失败返回错误码。
     * */
    public void loginAps(@NonNull String apsAddr, @NonNull String username, @NonNull String password, IResultListener resultListener){
        if (loggedIn){
            reportSuccess(null, resultListener);
            return;
        }
        if (NetAddrHelper.isValidIp(apsAddr)){
            doLoginAps(apsAddr, username, password, resultListener);
        }else{
            // 尝试域名解析
            executor.execute(() -> {
                List<String> ips = NetAddrHelper.parseDomain(apsAddr);
                mainHandler.post(() -> {
                    if (!ips.isEmpty()){
                        doLoginAps(ips.get(0), username, password, resultListener);
                    }else{
                        reportFailed(-1, resultListener);
                    }
                });

            });
        }

    }


    private void doLoginAps(String apsIP, String username, @NonNull String password, IResultListener resultListener){
        long ipLong;
        try {
            ipLong = NetAddrHelper.ipStr2LongLittleEndian(apsIP);
        } catch (NetAddrHelper.InvalidIpv4Exception e) {
            e.printStackTrace();
            reportFailed(-1, resultListener);
            return;
        }
        MtXAPSvrCfg mtXAPSvrCfg = new MtXAPSvrCfg(
                EmServerAddrType.emSrvAddrTypeCustom.ordinal(),
                apsIP,
                "",
                ipLong,
                true,
                60090   // 端口暂时写死
        );

        // 配置Aps
        req(Msg.SetApsServerCfg, null, null, new MtXAPSvrListCfg(0, Collections.singletonList(mtXAPSvrCfg)));

        loggedIn = false;
        // 登录Aps
        req(Msg.LoginAps, new SessionProcessor<Msg>() {
                @Override
                public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                    TApsLoginResult apsLoginResult = (TApsLoginResult) rspContent;
                    if (apsLoginResult.MainParam.bSucess){
                        // 获取platform地址
                        req(Msg.GetPlatformAddr, new SessionProcessor<Msg>() {
                            @Override
                            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {
                                TMtPlatformApiAddr platformApiAddr = (TMtPlatformApiAddr) output;
                                if (null == platformApiAddr){
                                    reportFailed(LIResultCode.Failed, resultListener);
                                    return;
                                }
                                // 获取platform分配的token
                                req(Msg.QueryAccountToken, new SessionProcessor<Msg>() {
                                    @Override
                                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                        TRestErrorInfo restErrorInfo = (TRestErrorInfo) rspContent;
                                        if (restErrorInfo.dwErrorID == 1000){
                                            // 登录platform
                                            req(Msg.LoginPlatform, new SessionProcessor<Msg>() {
                                                @Override
                                                public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                                    TLoginPlatformRsp res = (TLoginPlatformRsp) rspContent;
                                                    if (res.MainParam.dwErrorID == 1000){
                                                        reportSuccess(null, resultListener);
                                                        loggedIn = true;
                                                    }else{
                                                        logoutAps(null);
                                                        reportFailed(-1, resultListener);
                                                    }
                                                }
                                                @Override
                                                public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                                    logoutAps(null);
                                                    reportFailed(-1, resultListener);
                                                }
                                            }, resultListener, new TMTWeiboLogin(username, password));
                                        }else{
                                            logoutAps(null);
                                            reportFailed(-1, resultListener);
                                        }
                                    }

                                    @Override
                                    public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                        logoutAps(null);
                                        reportFailed(-1, resultListener);
                                    }
                                }, resultListener, NetAddrHelper.ipLongLittleEndian2Str(platformApiAddr.dwIp));
                            }
                        }, resultListener);
                    }else{
                        reportFailed(LIResultCode.trans(rsp, apsLoginResult.MainParam.dwApsErroce), resultListener);
                    }
                }
            },
            resultListener, new TMTApsLoginParam(username, password, "", "Skywalker_Ali", "")
        );

    }


    /**
     * 注销APS
     * */
    public void logoutAps(IResultListener resultListener){
        loggedIn = false;
        req(Msg.LogoutAps, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtSvrState[] states = ((TMtSvrStateList) rspContent).arrSvrState;
                boolean got = false;
                for (TMtSvrState state : states){
                    if (EmServerType.emAPS == state.emSvrType
                            && EmServerState.emSrvIdle == state.emSvrState){
                        got = true;
                        break;
                    }
                }
                if (got) {
                    reportSuccess(null, resultListener);
                }else {
                    isConsumed[0] = false; // 该条消息不是我们期望的，继续等待后续消息
                }
            }
        }, resultListener);
    }


    /**
     * 查询用户详情
     * @param resultListener
     *          成功反馈：{@link UserDetails}
     *          失败反馈 errorcode
     * */
    public void queryUserDetails(@NonNull IResultListener resultListener){
        req(Msg.GetUserBrief, new SessionProcessor<Msg>() {
            @Override
            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {

                TMTUserInfoFromAps userBrief = (TMTUserInfoFromAps) output;
                if (null == userBrief){
                    reportFailed(-1, resultListener);
                    return;
                }
                String username = userBrief.achE164;
                req(Msg.QueryUserDetails, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        TQueryUserDetailsRsp detailsRsp = (TQueryUserDetailsRsp) rspContent;
                        if (1000 == detailsRsp.MainParam.dwErrorID){
                            UserDetails userDetails = ToDoConverter.fromTransferObj(detailsRsp.AssParam);
                            userDetails.aliroomId = userBrief.achVirtualRoomId;
                            reportSuccess(userDetails, resultListener);
                        }else{
                            reportFailed(-1, resultListener);
                        }
                    }
                }, resultListener, new TMTAccountManagerSystem(username));
            }
        }, resultListener);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected void onNtf(Msg ntf, Object ntfContent) {
        switch (ntf){
            case KickedOff:
                Stream.of(getNtfListeners(OnKickedOffListener.class)).forEach(OnKickedOffListener::onKickedOff);
                break;
        }
    }

    /**
     * 被抢登监听器
     * */
    public interface OnKickedOffListener extends INtfListener {
        void onKickedOff();
    }

}
