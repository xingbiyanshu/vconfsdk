package com.sissi.vconfsdk.annotation;

/**
 * Created by Sissi on 2018/9/10.
 */

/**
 * 用来指明注解生成类的消费者,意义在于指导注解处理器生成正确的包名以便于消费者访问.
 *
 * 注意: 同一个注解的消费者只能是唯一的,多于一个消费者只有其中一个有效,其余被忽略. 比如:
 *  * @Consumer(Message.class)
 * ClassA{}
 *
 *  * @Consumer(Message.class)
 * ClassB{}
 * 则ClassB的被忽略.
 * */

public @interface Consumer {
    Class[] value(); // 可消费的注解列表
}