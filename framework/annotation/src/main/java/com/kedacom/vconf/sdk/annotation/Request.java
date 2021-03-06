package com.kedacom.vconf.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记一条请求.
 * 请求的结果往往通过响应{@link Response}反馈。
 * 请求也可能没有响应，如设置/获取本地配置。
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Request {

    /**
     * 请求名称(请求对应的native方法名称)
     * 如LoginManager.java中定义如下方法：
     * public static native void login(String jsonLoginPara);
     * 则该字段值为"login"
     * */
    String name();

    /**
     * native方法所属类
     * 如com.kedacom.kdv.mt.mtapi.LoginManager.java中定义如下方法：
     * public static native void login(String jsonLoginPara);
     * 则该字段值为"com.kedacom.kdv.mt.mtapi.LoginManager"
     * */
    String owner();

    /** native方法参数类型
     * 如LoginManager.java中定义如下方法：
     * public static native void login(String jsonLoginPara， String jsonLoginPara2, int para3);
     * 则该字段值为{String.class, String.class, int.class}
     * */
    Class[] paras() default {};

    int InvalidIndex = -1;
    int LastIndex = 987654321;

    /** 用户方法参数类型。
     不同于paras，paras为native方法的形参列表，目前大部分情形下是StringBuffer类型的json字符串，而userParas是面向用户（框架使用者）的方法的形参列表。
     例如native方法定义如下：
     public static native void login(StringBuffer jsonLoginPara1, StringBuffer jsonLoginPara2);
     而为了用户使用方便，面向用户的接口可能定义如下：
     public void login(LoginPara1 loginPara1, LoginPara2 loginPara2)
     则paras和userParas的赋值分别为paras={StringBuffer.class, StringBuffer.class}, userParas={LoginPara1.class, LoginPara2.class}
     框架在调用native方法前自动将LoginPara对象转为StringBuffer类型json字符串。

     有的native方法有传出参数，如获取本地配置。此时我们需要正确的声明{@link #outputParaIndex()}。
     如有如下native方法：
     public static native void DcsGetServerCfg(String serverId, StringBuffer outPara); // 最后一个参数为传出参数，native方法使用传出参数反馈请求结果。
     对应的用户方法我们定义为：
     public void getServerCfg(String serverId, ResultListener{ onResult(DCServerCfg cfg){}}); // 用户方法比native方法少一个传出参数，而通过监听器接受结果。
     我们声明outputParaIndex={@link #LastIndex}  // 最后一个参数为出参。
     则paras和userParas的赋值分别为
     paras={String.class, StringBuffer.class},
     userParas={String.class,
     DCServerCfg.class // NOTE: 用户并不需要传入该参数，此为请求的结果。
     }
     框架在反馈用户结果前根据outputParaIndex找到出参，并自动将StringBuffer类型json字符串outPara转为DCServerCfg对象。

     userPara到para转换规则按优先级从高到低如下：
     1、若userPara为基本类型包装类型，para为对应的基本类型，则将包装类型解包；
     2、若userPara为String，para为StringBuffer则将String转为StringBuffer；
     3、若para为String或StringBuffer，则将userPara转为String或StringBuffer类型的json字符串；
     4、若para为int且userPara为枚举则尝试使用已注册的json转换器将该枚举转为int；
     5、其余情形不做转换；
     */
    Class[] userParas() default {};

    /**
     * 出参index。
     * 有的native方法有传出参数，如获取配置。此字段用于标记该出参位置用于特殊处理。
     * 若值为{@link #InvalidIndex}则表示没有出参，值为{@link #LastIndex}则表示最后一个参数为出参。
     * */
    int outputParaIndex() default InvalidIndex;

    /**
     * 请求对应的响应序列。{@link Response}
     * NOTE：请求也可能没有响应，如设置/获取本地配置。
     * */
    String[] rspSeq() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq2() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq3() default {};
    /**
     * 请求对应的另一路可能的响应序列。
     * */
    String[] rspSeq4() default {};
    // 添加更多路响应序列很容易（只需在注解处理器中相应增加一行），
    // 但是一个请求有太多可能的响应序列这并不合理，
    // 往往暗示着设计有缺陷，需审视。

    /**
     * 贪婪模式标记。
     * 贪婪模式的使用场景：
     * 假设有req，接受的响应序列为：
     * rspA, rspA, ..., rspA。 // 接受不定次数的rspA。
     * rsp1, rsp1, ..., rsp1, rsp2, rsp2,..., rsp2； // 接受不定次数的rsp1，然后不定次数的rsp2。
     * rspX, rspX, ..., rspX, rspY。 // 接受不定次数rspX, 然后一个rspY
     * 可以看到响应序列中有的响应可以接受不定次数，或者数字庞大的次数，此种情形下，如何注册该响应序列呢？
     * 逐条注册行不通，此时则可以使用贪婪模式标记如下注册：
     * rspSeq={rspA, GREEDY},
     * rspSeq2={rsp1, GREEDY, rsp2, GREEDY},
     * rspSeq3={rspX, GREEDY, rspY},
     * GREEDY必须跟在一个响应后面，表示可以接受不定次数的该响应。
     * 注意：此例中的rspSeq，rspSeq2永远无法匹配结束，因为它始终在等无尽的“最后一条响应”，若不加干预会话最终将超时，
     * 这通常不是用户期望的，所以此种情形下用户需要根据接收到的响应内容判断是否需要手动结束会话以避免超时。
     * */
    String GREEDY = "...";

    /**
     * 请求对应的超时时长。单位：秒
     * */
    double timeout() default 5;


    /**
     * native方法所在类路径定义
     * */
    String PKG = "com.kedacom.kdv.mt.mtapi.";
    String KernalCtrl = PKG+"KernalCtrl";
    String MtcLib = PKG+"MtcLib";
    String LoginCtrl = PKG+"LoginCtrl";
    String MonitorCtrl = PKG+"MonitorCtrl";
    String CommonCtrl = PKG+"CommonCtrl";
    String ConfigCtrl = PKG+"ConfigCtrl";
    String MtServiceCfgCtrl = PKG+"MtServiceCfgCtrl";
    String MeetingCtrl = PKG+"MeetingCtrl";
    String MtEntityCtrl = PKG+"MtEntityCtrl";
    String RmtContactCtrl = PKG+"RmtContactCtrl";
    String ConfCtrl = PKG+"ConfCtrl";
    String AudioCtrl = PKG+"AudioCtrl";
    String DcsCtrl = PKG+"DcsCtrl";

}
