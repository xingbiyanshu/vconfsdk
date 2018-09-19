package com.sissi.annotation;

/**
 * 用于标记设置消息．
 *
 * 设置是同步的．
 *
 * Created by Sissi on 2018/9/14.
 */

public @interface Set {
    Class value(); // 传入参数对应的类
}