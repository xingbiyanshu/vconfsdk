package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Bitmap;
import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

/**
 * 画板。
 * 负责：
 * 提供画板界面；
 * 提供工具栏相关接口如设置画直线画圆，设置颜色画笔粗细，插入图片等；
 * 处理用户触屏事件结合工具栏设置生成相应绘制操作并上报给用户；
 * */
public interface IPaintBoard {

    /**
     * 获取画板ID。
     * 画板信息中也包含该ID，此方法为便捷方法。
     * */
    String getBoardId();

    /**
     * 获取画板信息
     * */
    BoardInfo getBoardInfo();

    /**
     * 获取画板View。
     * 用户需要将该view添加到自己的view树中后方可正常展示画板及在画板上绘制。
     * 用户亦可通过为此view设置背景来给画板设置背景。
     * @return 画板对应的View
     * */
    View getBoardView();

    // 工具
    int TOOL_PENCIL = 1;
    int TOOL_LINE = 2;
    int TOOL_RECT = 3;
    int TOOL_OVAL = 4;
    int TOOL_RECT_ERASER = 5;
    int TOOL_ERASER = 6;
    /**
     * 设置画板工具。
     * 在进行相关绘制操作前需先设置好对应的工具。
     * 如画任意曲线需设置TOOL_PENCIL, 画直线需设置TOOL_LINE。
     * */
    void setTool(int style);

    /**
     * 获取当前画板工具
     * */
    int getTool();

    /**
     * 设置画笔粗细。
     * @param width 粗细。单位：pixel
     * */
    void setPaintStrokeWidth(int width);

    /**
     * 获取画笔粗细
     * */
    int getPaintStrokeWidth();

    /**
     * 设置画笔颜色。
     * @param color 颜色值。NOTE:必须为正整数，如果是字面值注意加后缀"L"，如0xFFFFFFFFL。
     * */
    void setPaintColor(long color);

    /**
     * 获取画笔颜色
     * */
    long getPaintColor();

    /**
     * 设置橡皮擦尺寸（size*size）。
     * @param size 橡皮擦尺寸。单位：pixel
     * */
    void setEraserSize(int size);

    /**
     * 获取橡皮擦尺寸
     * */
    int getEraserSize();

    /**
     * 插入图片
     * @param picPath 图片绝对路径
     * */
    void insertPic(String picPath);


    /**
     * 所有区域，包括画板窗口内和窗口外。
     * */
    int AREA_ALL = 0;
    /**
     * 仅画板窗口内
     * */
    int AREA_WINDOW = 1;
//    /**
//     * 所有区域的图形
//     * */
//    int AREA_ALL_SHAPE = 2;
//    /**
//     * 所有区域的图片
//     * */
//    int AREA_ALL_PIC = 3;

    /**
     * 快照。
     * @param area 区域{@link #AREA_ALL},{@link #AREA_WINDOW}。
     * @param outputWidth 生成的图片的宽
     * @param outputHeight 生成的图片的高
     * @return 快照。
     * */
    Bitmap snapshot(int area, int outputWidth, int outputHeight);


    /**
     * 从上次保存以来内容是否有变更。
     * 可用来决定是否需要再次保存。
     * @return 返回true如果从上次保存以来内容有变更，否则返回false。
     * */
    boolean changedSinceLastSave(); // TODO hasContentChangedSinceLastSnapshot

    /**
     * 撤销。
     * */
    void undo();

    /**
     * 恢复被撤销的操作。
     * */
    void redo();

    /**
     * 清屏
     * */
    void clearScreen();

    /**
     * 放缩
     * @param percentage 百分数。如50代表50%。
     * */
    void zoom(int percentage);

    /**
     * 获取放缩百分数
     * @return 百分数。如50代表50%。
     * */
    int getZoom();

    /**
     * 设置最小缩放率
     * @param rate 缩放率。如50代表50%。
     * */
    void setMinZoomRate(int rate);

    /**
     * 获取最小缩放率
     * @return  缩放率。如50代表50%。
     * */
    int getMinZoomRate();

    /**
     * 设置最大缩放率
     * @param rate 缩放率。如300代表300%。
     * */
    void setMaxZoomRate(int rate);

    /**
     * 获取最大缩放率
     * @@return  缩放率。如300代表300%。
     * */
    int getMaxZoomRate();

    /**
     * 获取被撤销操作数量
     * */
    int getRepealedOpsCount();

    /**
     * 获取图形操作数量
     * */
    int getShapeOpsCount();

    /**
     * 获取图片数量
     * */
    int getPicCount();

    /**
     * 画板内容是否为空。
     * 判断标准跟{@link IOnBoardStateChangedListener#onEmptyStateChanged(boolean)}一致。
     * */
    boolean isEmpty();

    /**
     * 画板状态变化监听器
     * */
    interface IOnBoardStateChangedListener{
        /**图片数量变化
         * @param count  当前图片数量*/
        default void onPictureCountChanged(int count){}
        /**缩放比例变化
         * @param percentage  当前屏幕缩放比率百分数。如50代表50%。*/
        default void onZoomRateChanged(int percentage){}
        /**
         * 可撤销状态变化。
         * 触发该方法的场景：
         * 1、新画板画了第一笔；
         * 2、执行了撤销操作；
         * 3、执行了恢复操作；
         * @param repealedOpsCount 已被撤销操作数量
         * @param remnantOpsCount 剩下的可撤销操作数量。如画了3条线撤销了1条则repealedOpsCount=1，remnantOpsCount=2。
         *                        NOTE: 此处的可撤销数量是具体业务无关的，“可撤销”指示的是操作类型，如画线画圆等操作是可撤销的而插入图片放缩等是不可撤销的。
         * */
        default void onRepealableStateChanged(int repealedOpsCount, int remnantOpsCount){}
        /**
         * 画板内容为空状态变化（即画板内容从有到无或从无到有）。
         * 画板内容包括图形和图片。
         * 该方法触发的场景包括：
         * 1、最后一笔图形被撤销且没有图片，bEmptied=true；
         * 2、最后一张图片被删除且没有图形，bEmptied=true；
         * 3、清屏且没有图片，bEmptied=true；
         * 4、上述123或画板刚创建情形下，第一笔图形绘制或第一张图片插入，bEmptied=false；
         * NOTE:
         * 1、新建的画板为空（{@link IPaintBoard#isEmpty()}返回true），但不会触发该方法；
         * 2、使用“擦除”功能，包括黑板擦擦除矩形擦除，将画板内容清掉的情形不会触发此方法，且{@link IPaintBoard#isEmpty()}返回false；
         *
         * @param bEmptied 内容是否空了，true表示画板内容从有到无，false表示画板内容从无到有。
         * */
        default void onEmptyStateChanged(boolean bEmptied){}

    }
    IPaintBoard setOnBoardStateChangedListener(IOnBoardStateChangedListener onBoardStateChangedListener);


    /**
     * 发布者。
     * 己端作为“创作者”角色完成创作后通过发布者将内容（绘制操作）发布出去。
     * */
    interface IPublisher{
        /**
         * 产生了绘制操作可供发布
         * @param Op 绘制操作*/
        void publish(OpPaint Op);
    }
    /**
     * 设置发布者。
     * NOTE: 只有设置了发布者才可以在画板上主动绘制
     * */
    IPaintBoard setPublisher(IPublisher publisher);

}
