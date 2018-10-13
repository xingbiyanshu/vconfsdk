package com.sissi.vconfsdk.base.amulet;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 通知处理器
 *
 * Created by Sissi on 1/9/2017.
 */
final class NotifiManager implements ISubscribeProcessor, INotificationProcessor, INotificationEmitter{

    private static final String TAG = NotifiManager.class.getSimpleName();

    private static NotifiManager instance;

    private NativeInteractor nativeInteractor;
    private MessageRegister messageRegister;
    private JsonProcessor jsonProcessor;

    private Map<String, Set<Handler>> subscribers;

    private NotifiManager(){
        nativeInteractor = NativeInteractor.instance();
        messageRegister = MessageRegister.instance();
        jsonProcessor = JsonProcessor.instance();

        subscribers = new HashMap<>();
    }

    synchronized static NotifiManager instance() {
        if (null == instance) {
            instance = new NotifiManager();
        }

        return instance;
    }



    @Override
    public synchronized boolean subscribe(Handler subscriber, String ntfId) {
        if (null == subscriber){
            return false;
        }

        if (!messageRegister.isNotification(ntfId)){
            Log.e(TAG, "Unknown notification "+ntfId);
            return false;
        }

        Set<Handler> subs = subscribers.get(ntfId);
        if (null == subs){
            subs = new HashSet<>();
            subscribers.put(ntfId, subs);
        }

        subs.add(subscriber);

        return true;
    }

    @Override
    public synchronized void unsubscribe(Handler subscriber, String ntfId) {

        if (null == subscriber){
            return;
        }

        if (!messageRegister.isNotification(ntfId)){
            Log.e(TAG, "Unknown notification "+ntfId);
            return;
        }

        Set<Handler> subs = subscribers.get(ntfId);
        if (null != subs){
            subs.remove(subscriber);
            if (subs.isEmpty()){
                subscribers.remove(ntfId);
            }
        }
    }


    @Override
    public synchronized boolean processNotification(String ntfName, String ntfBody) {
        if (!messageRegister.isNotification(ntfName)){
            return false;
        }
        Set<Handler> subs = subscribers.get(ntfName);
        if (null == subs || 0==subs.size()){
            return false;
        }

        Log.i(TAG, String.format("<-~- %s\n%s", ntfName, ntfBody));

        Object ntfContent = jsonProcessor.fromJson(ntfBody, messageRegister.getNtfClazz(ntfName));

        for (Handler sub : subs){
            Message msg = Message.obtain();
            msg.obj = new FeedbackBundle(ntfName, ntfContent, FeedbackBundle.NTF);
            sub.sendMessage(msg);
        }

        return true;
    }


    @Override
    public synchronized boolean emitNotification(String ntfName) {
        if (!messageRegister.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return false;
        }

        return nativeInteractor.emitNotification(ntfName);
    }

}