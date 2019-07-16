/**
 * 定制的Json反序列化适配器。
 *
 * 对于Class X{
 *     MainParam;
 *     AssParam;
 * }
 * 对于大多数X下层返回的json字符串是如下形式：
 * {MainParam: val1, AssParam: val2}
 * 如此可直接使用gson.fromJson解析得到A对象。
 * 但是对于有些X下层返回的json字符串除了上面那种形式外还有如下形式：
 * {val1}
 *
 * 此定制的适配器专门用于后一种X。
 *
 * */

package com.kedacom.vconf.sdk.base.tools;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kedacom.vconf.sdk.base.basement.JsonProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class MainAssParaJsonAdapter<T> implements JsonDeserializer<T> {

    private static final String MANKEY = "MainParam";
    private static final String ASSKEY = "AssParam";
    private Class<T> classT;

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (null == classT) {
            classT = (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }

        T instanceT = null;
        try {
            instanceT = classT.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        JsonObject jsonObject = json.getAsJsonObject();
        Gson gson = JsonProcessor.instance().obtainGson();
        try {
            if (jsonObject.has(MANKEY)){ // json 由两部分组成： "MainParam" : {} "AssParam" : {}
                Field fieldMain = classT.getDeclaredField(MANKEY);
                fieldMain.setAccessible(true);
                fieldMain.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(MANKEY), fieldMain.getType()));
                Field fieldAss = classT.getDeclaredField(ASSKEY);
                fieldAss.setAccessible(true);
                fieldAss.set(instanceT, gson.fromJson(jsonObject.getAsJsonObject(ASSKEY), fieldAss.getType()));
            }else{ // json 只包含MainParam部分（没有"MainParam" key）：{}
                Field fieldMain = classT.getDeclaredField(MANKEY);
                fieldMain.setAccessible(true);
                fieldMain.set(instanceT, gson.fromJson(jsonObject, fieldMain.getType()));
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return instanceT;
    }

}
