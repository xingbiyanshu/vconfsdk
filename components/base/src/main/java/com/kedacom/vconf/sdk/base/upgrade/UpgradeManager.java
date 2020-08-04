package com.kedacom.vconf.sdk.base.upgrade;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerState;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerType;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrState;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrStateList;
import com.kedacom.vconf.sdk.base.upgrade.bean.DownloadProgressInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.UpgradePkgInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TCheckUpgradeRsp;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTSUSAddr;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeClientInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeDeviceInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeDownloadInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeNetParam;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeVersionInfo;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.utils.file.FileHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.io.File;

import static com.kedacom.vconf.sdk.base.upgrade.UpgradeResultCode.ALREADY_NEWEST;
import static com.kedacom.vconf.sdk.base.upgrade.UpgradeResultCode.NO_UPGRADE_PACKAGE;
import static com.kedacom.vconf.sdk.base.upgrade.UpgradeResultCode.SERVER_DISCONNECTED;


public class UpgradeManager extends Caster<Msg> {
    private static UpgradeManager instance = null;
    private Context context;


    private UpgradeManager(Context ctx) {
        context = ctx;
    }

    public synchronized static UpgradeManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new UpgradeManager(ctx);
            instance.startService();
        }
        return instance;
    }

    // 启动业务组件升级服务
    private void startService(){
        String serviceName = "upgrade";
        req(Msg.StartMtService, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TSrvStartResult result = (TSrvStartResult) rspContent;
                boolean success = result.MainParam.basetype && result.AssParam.achSysalias.equals(serviceName);
                if (success){
                    KLog.p("start %s service success!", serviceName);
                }
            }
        }, null , serviceName);
    }


    /**
     * 检查更新
     * @param terminalType 终端类型
     * @param version 当前软件版本
     * @param e164 用户e164
     * @param resultListener onSuccess {@link UpgradePkgInfo}；
     *                       onFailed  {@link UpgradeResultCode#NO_UPGRADE_PACKAGE}
     *                                 {@link UpgradeResultCode#ALREADY_NEWEST}
     * */
    public void checkUpgrade(@NonNull TerminalType terminalType, @NonNull String version, @NonNull String e164, @NonNull IResultListener resultListener){
        TMTSUSAddr addr = (TMTSUSAddr) get(Msg.GetServerAddr);
        if (null == addr){
            reportFailed(-1, resultListener);
            return;
        }
        TMTUpgradeClientInfo checkUpgradePara = new TMTUpgradeClientInfo(
                new TMTUpgradeNetParam(addr.dwIP),
                new TMTUpgradeDeviceInfo(terminalType.getVal(), e164, version, addr.dwIP, "kedacom")
        );

        req(Msg.CheckUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                /*
                 * 检查更新完成后取消。
                 * 对于业务组件那边来说检查更新不是一个独立的短暂的操作，而是和其他操作如下载更新关联在一起的，是一条流水线上的不同环节，是一个长连接的发端。
                 * 检查更新对他们来说更准确的语义是“开始升级流程”，需要跟“结束（取消）升级流程”配对使用，这样才算一个完整的升级流程，他们才能维持正确的状态。
                 * 所以当直接使用业务组件接口时，如下调用序列将会失败：
                 * 检查更新->检查更新（不能重复调用检查更新接口因为不能重复开始升级流程，业务组件状态不对）；
                 * 下载更新（不能直接调用下载更新的接口，必须先调用检查更新）；
                 *
                 * 我们认为这样的接口语义和行为不一致，会给用户造成严重困惑，故我们封装以使接口的行为跟语义保持一致。下面是我们做的工作：
                 * 1、站在用户角度，检查升级我们认为应该独立于下载、取消升级，检查结果返回检查升级结束，不存在遗留状态，重复检查是可以的。
                 * 为了达成这个目标我们在业务组件的检查升级结果返回后取消升级，以重置业务组件的状态。
                 * 2、站在用户角度，下载升级包我们认为应该独立于检查升级，我知道该升级包的id便可直接下载它。
                 * 为了达成这个目标我们在下载前先调用业务组件的检查升级，然后才去下载升级包。
                 * */
                req(Msg.CancelUpgrade, null, null);

                TMTUpgradeVersionInfo[] remoteVersionList = ((TCheckUpgradeRsp)rspContent).AssParam.tVerList;
                if (null != remoteVersionList && remoteVersionList.length>0){
                    UpgradePkgInfo upgradePkgInfo = ToDoConverter.TMTUpgradeVersionInfo2UpgradePkgInfo(remoteVersionList[0]);
                    if (version.compareToIgnoreCase(upgradePkgInfo.versionNum) < 0) {
                        reportSuccess(upgradePkgInfo, resultListener);
                    }else{
                        reportFailed(ALREADY_NEWEST, resultListener);
                    }
                }else{
                    reportFailed(NO_UPGRADE_PACKAGE, resultListener);
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                req(Msg.CancelUpgrade, null, null);
            }
        }, resultListener, checkUpgradePara);
    }


    /**
     * 下载升级包
     * @param versionId 目标版本id（由checkUpgrade的返回结果中获取）。
     * @param saveDirPath 升级包存放目录
     * @param resultListener onProgress {@link DownloadProgressInfo}
     *                       onSuccess  downloaded package file full path
     *                       onFailed   {@link UpgradeResultCode#SERVER_DISCONNECTED}
     * */
    public void downloadUpgrade(int versionId, String saveDirPath, IResultListener resultListener){
        File saveDir = FileHelper.createDir(saveDirPath);
        if (saveDir == null){
            KLog.p(KLog.ERROR, "create save dir %s failed!", saveDirPath);
            reportFailed(-1, resultListener);
            return;
        }
        // 下载前先check以建链
        req(Msg.CheckUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMTUpgradeVersionInfo[] remoteVersionList = ((TCheckUpgradeRsp)rspContent).AssParam.tVerList;
                if (null != remoteVersionList && remoteVersionList.length>0){
                    UpgradePkgInfo upgradePkgInfo = ToDoConverter.TMTUpgradeVersionInfo2UpgradePkgInfo(remoteVersionList[0]);
                    if (upgradePkgInfo.versionId == versionId){
                        // 下载升级包
                        req(Msg.DownloadUpgrade, new SessionProcessor<Msg>() {
                            @Override
                            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                if (Msg.DownloadUpgradeRsp == rsp){
                                    TMTUpgradeDownloadInfo downloadInfo = (TMTUpgradeDownloadInfo) rspContent;
                                    if (downloadInfo.dwErrcode == 0) {
                                        // 上报下载进度
                                        reportProgress(new DownloadProgressInfo(downloadInfo.dwCurPercent), resultListener);
                                        if (downloadInfo.dwCurPercent == 100) {
                                            cancelReq(Msg.DownloadUpgrade, resultListener); // 已下载完毕，取消会话，否则会话会等待超时，因为没有结束消息。
                                            reportSuccess(new File(saveDir, downloadInfo.achCurFileName).getAbsolutePath(), resultListener);
                                        }
                                    } else {
                                        reportFailed(-1, resultListener);
                                    }
                                }else if (Msg.ServerDisconnected == rsp){
                                    reportFailed(SERVER_DISCONNECTED, resultListener);
                                }
                            }

                            @Override
                            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                req(Msg.CancelUpgrade, null, null);
                                isConsumed[0] = true;
                                reportTimeout(resultListener);
                            }
                        }, resultListener, saveDirPath, versionId);

                    }else{
                        reportFailed(NO_UPGRADE_PACKAGE, resultListener);
                    }
                }else{
                    reportFailed(NO_UPGRADE_PACKAGE, resultListener);
                }
            }
        }, resultListener);

    }


    /**
     * 取消下载升级包
     * @param resultListener onSuccess null
     * */
    public void cancelDownloadUpgrade(IResultListener resultListener){
        req(Msg.CancelUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtSvrState[] states = ((TMtSvrStateList) rspContent).arrSvrState;
                boolean got = false;
                for (TMtSvrState state : states){
                    if (EmServerType.emSUS == state.emSvrType
                            && (EmServerState.emSrvIdle == state.emSvrState || EmServerState.emSrvDisconnected == state.emSvrState)){
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

}
