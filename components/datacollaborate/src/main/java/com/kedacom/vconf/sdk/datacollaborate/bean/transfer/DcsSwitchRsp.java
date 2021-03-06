/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.json.LameStrategy;

//@JsonAdapter(DcsSwitchRsp.Adapter.class)
@LameStrategy(mainField = TDCSBoardResult.class)
public class DcsSwitchRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsSwitchRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

//    static {
//        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
//        Kson.registerAdapter(DcsSwitchRsp.class, new LameJsonAdapter<DcsSwitchRsp, TDCSBoardResult, TDCSBoardInfo>(){});
//    }
//    static final class Adapter extends LameJsonAdapter<DcsSwitchRsp, TDCSBoardResult, TDCSBoardInfo> { }
}
