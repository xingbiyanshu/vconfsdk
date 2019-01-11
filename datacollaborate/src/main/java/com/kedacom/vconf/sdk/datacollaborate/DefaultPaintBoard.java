package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDraw;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRectErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRedo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUndo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class DefaultPaintBoard extends FrameLayout implements IPaintBoard, Comparable<DefaultPaintBoard>{
    private Context context;

    // 画板matrix
    private Matrix boardMatrix = new Matrix();

    // 图形画布。用于图形绘制如画线、画圈、擦除等等
    private TextureView shapePaintView;
    // 调整中的图形操作。比如画线时，从手指按下到手指拿起之间的绘制都是“调整中”的。
    private OpPaint adjustingShapeOp;
    // 临时图形操作。手指拿起绘制完成，但并不表示此绘制已生效，需等到平台广播NTF后方能确认为生效的操作，在此之前的操作都作为临时操作保存在这里。
    private MyConcurrentLinkedDeque<OpPaint> tmpShapeOps = new MyConcurrentLinkedDeque<>();
    // 图形操作。已经平台NTF确认过的操作。
    private MyConcurrentLinkedDeque<OpPaint> shapeOps = new MyConcurrentLinkedDeque<>();
    // 被撤销的图形操作。撤销只针对已经平台NTF确认过的操作。
    private Stack<OpPaint> repealedShapeOps = new Stack<>();

    // 图片画布。用于绘制图片。
    private TextureView picPaintView;
    // 图片操作。
    private MyConcurrentLinkedDeque<OpPaint> picOps = new MyConcurrentLinkedDeque<>();

    // 图片编辑画布。
    private TextureView tmpPicPaintView;
    // 图片编辑画布缩放及位移
    private Matrix tmpPicViewMatrix = new Matrix();
    // 图片编辑操作
    private MyConcurrentLinkedDeque<PicEditStuff> picEditStuffs = new MyConcurrentLinkedDeque<>();
    // 删除图片按钮
    private Bitmap del_pic_icon;
    private Bitmap del_pic_active_icon;
    private float density = 1;

    // 相对于XHDPI的屏幕密度。
    private float relativeDensity;

    // 图层
    private int focusedLayer = LAYER_ALL;

    // 工具
    private int tool = TOOL_PENCIL;

    // 画笔粗细。单位：pixel
    private int paintStrokeWidth = 5;

    // 画笔颜色
    private long paintColor = 0xFFFFFFFFL;

    // 橡皮擦尺寸。单位：pixel
    private int eraserSize = 25;

    private static final int MIN_ZOOM = 25;
    private static final int MAX_ZOOM = 400;

    private IOnPaintOpGeneratedListener paintOpGeneratedListener;
    private IOnBoardStateChangedListener onBoardStateChangedListener;
    private IPublisher publisher;

    // 画板信息
    private BoardInfo boardInfo;

    private DefaultTouchListener boardViewTouchListener;
    private DefaultTouchListener shapeViewTouchListener;
    private DefaultTouchListener picViewTouchListener;
    private DefaultTouchListener tmpPicViewTouchListener;

    @Override
    public int compareTo(DefaultPaintBoard o) {
        if (null == o){
            return 1;
        }
        if (getBoardInfo().getAnonyId()<o.getBoardInfo().getAnonyId()){
            return -1;
        }else if (getBoardInfo().getAnonyId() == o.getBoardInfo().getAnonyId()){
            return 0;
        }else{
            return 1;
        }
    }


    public DefaultPaintBoard(@NonNull Context context, BoardInfo boardInfo) {
        super(context);
        this.context = context;
        density = context.getResources().getDisplayMetrics().density;
        relativeDensity = density/2;
//        shapeViewMatrixByDensity.postScale(relativeDensity, relativeDensity);
//        picViewMatrixByDensity.postScale(relativeDensity, relativeDensity);
        this.boardInfo = boardInfo;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View whiteBoard = layoutInflater.inflate(R.layout.default_whiteboard_layout, this);
        picPaintView = whiteBoard.findViewById(R.id.pb_pic_paint_view);
        picPaintView.setOpaque(false);
        shapePaintView = whiteBoard.findViewById(R.id.pb_shape_paint_view);
        shapePaintView.setOpaque(false);
        tmpPicPaintView = whiteBoard.findViewById(R.id.pb_tmp_paint_view);
        tmpPicPaintView.setOpaque(false);

        shapePaintView.setSurfaceTextureListener(surfaceTextureListener);
        picPaintView.setSurfaceTextureListener(surfaceTextureListener);
        tmpPicPaintView.setSurfaceTextureListener(surfaceTextureListener);

        shapeViewTouchListener = new DefaultTouchListener(context, shapeViewEventListener);
        picViewTouchListener = new DefaultTouchListener(context, picViewEventListener);
        tmpPicViewTouchListener = new DefaultTouchListener(context, tmpPicViewEventListener);
        boardViewTouchListener = new DefaultTouchListener(context, boardViewEventListener);
        picPaintView.setOnTouchListener( picViewTouchListener);
        shapePaintView.setOnTouchListener(shapeViewTouchListener);
        tmpPicPaintView.setOnTouchListener(tmpPicViewTouchListener);

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("del_pic.png");
            del_pic_icon = BitmapFactory.decodeStream(is);
            is.close();
            is = am.open("del_pic_active.png");
            del_pic_active_icon = BitmapFactory.decodeStream(is);
            is.close();
            Matrix matrix = new Matrix();
            matrix.postScale(density/2, density/2); // 切图是按hdpi给的故除2
            del_pic_icon = Bitmap.createBitmap(del_pic_icon, 0, 0,
                    del_pic_icon.getWidth(), del_pic_icon.getHeight(), matrix, true);
            del_pic_active_icon = Bitmap.createBitmap(del_pic_active_icon, 0, 0,
                    del_pic_active_icon.getWidth(), del_pic_active_icon.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setBackgroundColor(Color.DKGRAY);
    }

    public DefaultPaintBoard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            KLog.p("surface available");
            // 刷新
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            KLog.p("surface size changed");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            KLog.p("surface destroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    Matrix getBoardMatrix() {
        return boardMatrix;
    }

    void setBoardMatrix(Matrix boardMatrix) {
        this.boardMatrix.set(boardMatrix);
    }

    MyConcurrentLinkedDeque<OpPaint> getTmpShapeOps() {
        return tmpShapeOps;
    }

    MyConcurrentLinkedDeque<OpPaint> getShapeOps() {
        return shapeOps;
    }

    Stack<OpPaint> getRepealedShapeOps() {
        return repealedShapeOps;
    }


    MyConcurrentLinkedDeque<OpPaint> getPicOps() {
        return picOps;
    }

    public Matrix getTmpPicViewMatrix() {
        return tmpPicViewMatrix;
    }

    MyConcurrentLinkedDeque<PicEditStuff> getPicEditStuffs(){
        return picEditStuffs;
    }


    Canvas lockCanvas(int layer){
        // NOTE: TextureView.lockCanvas()获取的canvas没有硬件加速。
        if (LAYER_SHAPE == layer){
            return shapePaintView.lockCanvas();
        }else if (LAYER_PIC == layer){
            return picPaintView.lockCanvas();
        }else if (LAYER_PIC_TMP == layer){
            return tmpPicPaintView.lockCanvas();
        }
        return null;
    }

    void unlockCanvasAndPost(int layer, Canvas canvas){
        if (LAYER_SHAPE == layer){
            shapePaintView.unlockCanvasAndPost(canvas);
        }else if (LAYER_PIC == layer){
            picPaintView.unlockCanvasAndPost(canvas);
        }else if (LAYER_PIC_TMP == layer){
            tmpPicPaintView.unlockCanvasAndPost(canvas);
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (null == publisher){
            // 没有发布者不处理触屏事件。
            return true;
        }

        if (LAYER_ALL == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            boolean ret2 = picPaintView.dispatchTouchEvent(ev);
            boolean ret1 = shapePaintView.dispatchTouchEvent(ev);
            boolean ret3 = tmpPicPaintView.dispatchTouchEvent(ev);
            return ret1||ret2||ret3;
        } else if (LAYER_PIC_TMP == focusedLayer){
            return tmpPicPaintView.dispatchTouchEvent(ev);
        } else if (LAYER_NONE == focusedLayer){
            return true;
        }else if (LAYER_PIC == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            return picPaintView.dispatchTouchEvent(ev);
        }else if (LAYER_SHAPE == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            return shapePaintView.dispatchTouchEvent(ev);
        }else if (LAYER_PIC_AND_SHAPE == focusedLayer){
            boardViewTouchListener.onTouch(this, ev);
            boolean ret2 = picPaintView.dispatchTouchEvent(ev);
            boolean ret1 = shapePaintView.dispatchTouchEvent(ev);
            return ret1||ret2;
        }

        return false;
    }

    DefaultTouchListener.IOnEventListener boardViewEventListener = new DefaultTouchListener.IOnEventListener(){
        private float scaleCenterX, scaleCenterY;
        private final float scaleRateTopLimit = 3f;
        private final float scaleRateBottomLimit = 0.5f;

        @Override
        public void onMultiFingerDragBegin() {
            KLog.p("#######onMultiFingerDragBegin");
        }

        @Override
        public void onMultiFingerDrag(float dx, float dy) {
            KLog.p("~~> dx=%s, dy=%s", dx, dy);
            boardMatrix.postTranslate(dx, dy);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        }

        @Override
        public void onMultiFingerDragEnd() {
//            KLog.p("~~>");
            OpMatrix opMatrix = new OpMatrix(boardMatrix);
            assignBasicInfo(opMatrix);
            publisher.publish(opMatrix);
        }

        @Override
        public void onScaleBegin() {
            KLog.p("#######onScaleBegin");
            scaleCenterX = getWidth()/2;
            scaleCenterY = getHeight()/2;
        }

        @Override
        public void onScale(float factor) {
            KLog.p("~~> factor=%s", factor);
            boardMatrix.postScale(factor, factor, scaleCenterX, scaleCenterY);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
            zoomRateChanged();
        }

        @Override
        public void onScaleEnd() {
            KLog.p("#######onScaleEnd");
            OpMatrix opMatrix = new OpMatrix(boardMatrix);
            assignBasicInfo(opMatrix);
            publisher.publish(opMatrix);
        }
    };


    DefaultTouchListener.IOnEventListener shapeViewEventListener = new DefaultTouchListener.IOnEventListener(){

        @Override
        public void onDragBegin(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
            createShapeOp(x, y);
        }

        @Override
        public void onDrag(float x, float y) {
            KLog.p("~~> x=%s, y=%s", x, y);
            adjustShapeOp(x, y);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(adjustingShapeOp);
        }

        @Override
        public void onDragEnd() {
//            KLog.p("~~>");
            confirmShapeOp();
            KLog.p("new tmp op %s", adjustingShapeOp);
            tmpShapeOps.offerLast(adjustingShapeOp);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
            publisher.publish(adjustingShapeOp);
            adjustingShapeOp = null;
        }

    };


    DefaultTouchListener.IOnEventListener picViewEventListener = new DefaultTouchListener.IOnEventListener(){
        @Override
        public boolean onDown(float x, float y) {
            if (picOps.isEmpty()){
                KLog.p("pic layer is empty");
                return false; // 当前没有图片不用处理后续事件
            }
            return true;
        }


        @Override
        public void onLongPress(float x, float y) {
            KLog.p("onLongPress pic layer,  x=%s, y=%s", x, y);
            OpInsertPic opInsertPic = selectPic(x, y);
            if (null == opInsertPic){
                KLog.p("no pic selected(x=%s, y=%s)", x, y);
                return;
            }
            picOps.remove(opInsertPic);
            tmpPicViewMatrix.set(boardMatrix);
            editPic(opInsertPic);
        }
    };


    private float[] mapPoint= new float[2];
    private Matrix invertedBoardMatrix;
    private void createShapeOp(float startX, float startY){
        invertedBoardMatrix = MatrixHelper.invert(boardMatrix);
//        KLog.p("invert success?=%s, orgX=%s, orgY=%s", suc, x, y);
        mapPoint[0] = startX;
        mapPoint[1] = startY;
        invertedBoardMatrix.mapPoints(mapPoint);
        float x = mapPoint[0];
        float y = mapPoint[1];
//            KLog.p("startX=%s, startY=%s, shapeScaleX=%s, shapeScaleY=%s", startX, startY, shapeScaleX, shapeScaleY);
        switch (tool){
            case TOOL_PENCIL:
                OpDrawPath opDrawPath = new OpDrawPath(new ArrayList<>());
                opDrawPath.getPoints().add(new PointF(x, y));
                opDrawPath.getPath().moveTo(x, y);
                adjustingShapeOp = opDrawPath;
                break;
            case TOOL_LINE:
                OpDrawLine opDrawLine = new OpDrawLine();
                opDrawLine.setStartX(x);
                opDrawLine.setStartY(y);
                adjustingShapeOp = opDrawLine;
                break;
            case TOOL_RECT:
                OpDrawRect opDrawRect = new OpDrawRect();
                opDrawRect.setLeft(x);
                opDrawRect.setTop(y);
                adjustingShapeOp = opDrawRect;
                break;
            case TOOL_OVAL:
                OpDrawOval opDrawOval = new OpDrawOval();
                opDrawOval.setLeft(x);
                opDrawOval.setTop(y);
                adjustingShapeOp = opDrawOval;
                break;
            case TOOL_ERASER:
                OpErase opErase = new OpErase(eraserSize, eraserSize, new ArrayList<>());
                opErase.getPoints().add(new PointF(x, y));
                opErase.getPath().moveTo(x, y);
                adjustingShapeOp = opErase;
                break;
            case TOOL_RECT_ERASER:
                // 矩形擦除先绘制一个虚线矩形框选择擦除区域
                OpDrawRect opDrawRect1 = new OpDrawRect();
                opDrawRect1.setLeft(x);
                opDrawRect1.setTop(y);
                adjustingShapeOp = opDrawRect1;
                break;
            default:
                KLog.p(KLog.ERROR, "unknown TOOL %s", tool);
                return;
        }
        if (adjustingShapeOp instanceof OpDraw){
            OpDraw opDraw = (OpDraw) adjustingShapeOp;
            if (TOOL_ERASER == tool){
                opDraw.setStrokeWidth(eraserSize);
            }else if(TOOL_RECT_ERASER == tool){
                opDraw.setLineStyle(OpDraw.DASH);
                opDraw.setStrokeWidth(2);
                opDraw.setColor(0xFF08b1f2L);
            } else {
                opDraw.setStrokeWidth(paintStrokeWidth);
                opDraw.setColor(paintColor);
            }
        }
        assignBasicInfo(adjustingShapeOp);
    }

    private void adjustShapeOp(float adjustX, float adjustY){
        mapPoint[0] = adjustX;
        mapPoint[1] = adjustY;
        invertedBoardMatrix.mapPoints(mapPoint);
        float x = mapPoint[0];
        float y = mapPoint[1];
        switch (tool){
            case TOOL_PENCIL:
                OpDrawPath opDrawPath = (OpDrawPath) adjustingShapeOp;
                List<PointF> pointFS = opDrawPath.getPoints();
                float preX, preY, midX, midY;
                preX = pointFS.get(pointFS.size()-1).x;
                preY = pointFS.get(pointFS.size()-1).y;
                midX = (preX + x) / 2;
                midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                opDrawPath.getPath().quadTo(preX, preY, midX, midY);
                pointFS.add(new PointF(x, y));

                break;
            case TOOL_LINE:
                OpDrawLine opDrawLine = (OpDrawLine) adjustingShapeOp;
                opDrawLine.setStopX(x);
                opDrawLine.setStopY(y);
                break;
            case TOOL_RECT:
                OpDrawRect opDrawRect = (OpDrawRect) adjustingShapeOp;
                opDrawRect.setRight(x);
                opDrawRect.setBottom(y);
                break;
            case TOOL_OVAL:
                OpDrawOval opDrawOval = (OpDrawOval) adjustingShapeOp;
                opDrawOval.setRight(x);
                opDrawOval.setBottom(y);
                break;
            case TOOL_ERASER:
                OpErase opErase = (OpErase) adjustingShapeOp;
                pointFS = opErase.getPoints();
                preX = pointFS.get(pointFS.size()-1).x;
                preY = pointFS.get(pointFS.size()-1).y;
                midX = (preX + x) / 2;
                midY = (preY + y) / 2;
//                    KLog.p("=pathPreX=%s, pathPreY=%s, midX=%s, midY=%s", preX, preY, midX, midY);
                opErase.getPath().quadTo(preX, preY, midX, midY);
                pointFS.add(new PointF(x, y));

                break;
            case TOOL_RECT_ERASER:
                OpDrawRect opDrawRect1 = (OpDrawRect) adjustingShapeOp;
                opDrawRect1.setRight(x);
                opDrawRect1.setBottom(y);
                break;
            default:
                return;
        }


    }


    private void confirmShapeOp(){
        if (TOOL_RECT_ERASER == tool){
            OpDrawRect opDrawRect = (OpDrawRect) adjustingShapeOp;
            adjustingShapeOp = new OpRectErase(opDrawRect.getLeft(), opDrawRect.getTop(), opDrawRect.getRight(), opDrawRect.getBottom());
            assignBasicInfo(adjustingShapeOp);
        }else if (TOOL_PENCIL == tool){
            OpDrawPath opDrawPath = (OpDrawPath) adjustingShapeOp;
            List<PointF> points = opDrawPath.getPoints();
            PointF lastPoint = points.get(points.size()-1);
            opDrawPath.getPath().lineTo(lastPoint.x, lastPoint.y);
        }else if (TOOL_ERASER == tool){
            OpErase opErase = (OpErase) adjustingShapeOp;
            List<PointF> points = opErase.getPoints();
            PointF lastPoint = points.get(points.size()-1);
            opErase.getPath().lineTo(lastPoint.x, lastPoint.y);
        }
    }


    private void assignBasicInfo(OpPaint op){
        op.setConfE164(boardInfo.getConfE164());
        op.setBoardId(boardInfo.getId());
        op.setPageId(boardInfo.getPageId());
    }


    void clean(){
        publisher = null;
        paintOpGeneratedListener = null;
        onBoardStateChangedListener = null;
        handler.removeMessages(MSGID_FINISH_EDIT_PIC);
    }

    @Override
    public String getBoardId() {
        return null!=boardInfo ? boardInfo.getId() : null;
    }

    @Override
    public BoardInfo getBoardInfo(){
        return boardInfo;
    }

    @Override
    public View getBoardView() {
        return this;
    }


    @Override
    public void setTool(int style) {
        this.tool = style;
    }

    @Override
    public int getTool() {
        return tool;
    }

    @Override
    public void setPaintStrokeWidth(int width) {
        this.paintStrokeWidth = width;
    }

    @Override
    public int getPaintStrokeWidth() {
        return paintStrokeWidth;
    }

    @Override
    public void setPaintColor(long color) {
        this.paintColor = color;
    }

    @Override
    public long getPaintColor() {
        return paintColor;
    }

    @Override
    public void setEraserSize(int size) {
        eraserSize = size;
    }

    @Override
    public int getEraserSize() {
        return eraserSize;
    }

    @Override
    public void focusLayer(int layer) {
        focusedLayer = layer;
    }


    @Override
    public Bitmap snapshot(int layer) {
        KLog.p("layer=%s", layer);
        Bitmap shot = null;
        if (LAYER_ALL == layer) {
            Bitmap picBt = picPaintView.getBitmap();
            Bitmap shapeBt = shapePaintView.getBitmap();

            if (picBt==null || shapeBt==null){ // TODO 暂时规避，排查原因
                return null;
            }

            int picW = picBt.getWidth();
            int picH = picBt.getHeight();
            int shapeW = shapeBt.getWidth();
            int shapeH = shapeBt.getHeight();
            int maxW = picW>shapeW?picW:shapeW;
            int maxH = picH>shapeH?picH:shapeH;
            KLog.p("picW=%s, picH=%s, shapeW=%s, shapeH=%s", picW, picH, shapeW, shapeH);
            shot = Bitmap.createBitmap(maxW, maxH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            draw(canvas);
            canvas.drawBitmap(picBt, 0, 0, null);
            canvas.drawBitmap(shapeBt, 0, 0, null);
        }else if (LAYER_PIC_AND_SHAPE == layer){
            Bitmap picBt = picPaintView.getBitmap();
            Bitmap shapeBt = shapePaintView.getBitmap();
            int picW = picBt.getWidth();
            int picH = picBt.getHeight();
            int shapeW = shapeBt.getWidth();
            int shapeH = shapeBt.getHeight();
            int maxW = picW>shapeW?picW:shapeW;
            int maxH = picH>shapeH?picH:shapeH;
            KLog.p("picW=%s, picH=%s, shapeW=%s, shapeH=%s", picW, picH, shapeW, shapeH);
            shot = Bitmap.createBitmap(maxW, maxH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(shot);
            canvas.drawBitmap(picBt, 0, 0, null);
            canvas.drawBitmap(shapeBt, 0, 0, null);
        } else if (LAYER_SHAPE == layer){
            shot = shapePaintView.getBitmap();
        }else if (LAYER_PIC == layer){
            shot = picPaintView.getBitmap();
        }

        return shot;
    }

    private void dealSimpleOp(OpPaint op){
        if (null == publisher){
            KLog.p(KLog.ERROR,"publisher is null");
            return;
        }
        assignBasicInfo(op);
        publisher.publish(op);
    }

    @Override
    public void undo() {
        dealSimpleOp(new OpUndo());
    }

    @Override
    public void redo() {
        dealSimpleOp(new OpRedo());
    }

    @Override
    public void clearScreen() {
        dealSimpleOp(new OpClearScreen());
    }

    @Override
    public void zoom(int percentage) {
        int zoom = (MIN_ZOOM<=percentage && percentage<=MAX_ZOOM) ? percentage : (percentage<MIN_ZOOM ? MIN_ZOOM : MAX_ZOOM);
        KLog.p("zoom=%s, width=%s, height=%s", zoom, getWidth(), getHeight());
        OpMatrix opMatrix = new OpMatrix();
        opMatrix.getMatrix().setScale(zoom/100f, zoom/100f, getWidth()/2, getHeight()/2);
        dealSimpleOp(opMatrix);
    }

    float[] zoomVals = new float[9];
    @Override
    public int getZoom() {
        boardMatrix.getValues(zoomVals);
        return (int) (zoomVals[Matrix.MSCALE_X]*100);
    }

    private boolean bLastStateIsEmpty =true;
    @Override
    public boolean isEmpty() {
        // XXX 如果对端直接擦除、清屏一个空白画板会导致此接口返回false。TODO 在painter中过滤掉此种情形的擦除、清屏消息。
        // XXX 另外，使用“擦除”的方式清掉画板的内容并不会导致此接口返回true。
        return  (picOps.isEmpty() && (shapeOps.isEmpty() || shapeOps.peekLast() instanceof OpClearScreen));
    }


    @Override
    public IPaintBoard setPublisher(IPublisher publisher) {
        this.publisher = publisher;
        if (publisher instanceof LifecycleOwner){
            ((LifecycleOwner)publisher).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.publisher = null;
                    KLog.p("publisher destroyed");
                }
            });
        }

//        picPaintView.setOnTouchListener(null!=publisher ? picViewTouchListener : null);
//        shapePaintView.setOnTouchListener(null!=publisher ? shapeViewTouchListener : null);
//        tmpPicPaintView.setOnEventListener(null!=publisher ? tmpPicViewEventListener : null);

        return this;
    }


    @Override
    public int getRepealedOpsCount() {
        return repealedShapeOps.size();
    }

    @Override
    public int getShapeOpsCount() {
        return shapeOps.size();
    }

    @Override
    public int getPicCount() {
        int count = 0;
        for (OpPaint op : picOps){
            if (EOpType.INSERT_PICTURE == op.getType()){
                ++count;
            }
        }
        return count;
    }


    private void refreshEmptyState(){
        if (!bLastStateIsEmpty && isEmpty()){   // 之前是不为空的状态现在为空了
            onBoardStateChangedListener.onEmptyStateChanged(bLastStateIsEmpty=true);
        }else if (bLastStateIsEmpty && !isEmpty()){ // 之前是为空的状态现在不为空了
            onBoardStateChangedListener.onEmptyStateChanged(bLastStateIsEmpty=false);
        }
    }

    void repealableStateChanged(){
        if (null != onBoardStateChangedListener){
            onBoardStateChangedListener.onRepealableStateChanged(getRepealedOpsCount(), getShapeOpsCount());
            refreshEmptyState();
        }
    }

    void screenCleared(){
        if (null != onBoardStateChangedListener){
            refreshEmptyState();
        }
    }

    void picCountChanged(){
        if (null != onBoardStateChangedListener){
            onBoardStateChangedListener.onPictureCountChanged(getPicCount());
            refreshEmptyState();
        }
    }

    void zoomRateChanged(){
        if (null != onBoardStateChangedListener){
            onBoardStateChangedListener.onZoomRateChanged(getZoom());
        }
    }



    @Override
    public IPaintBoard setOnBoardStateChangedListener(IOnBoardStateChangedListener onBoardStateChangedListener) {
        this.onBoardStateChangedListener = onBoardStateChangedListener;
        if (onBoardStateChangedListener instanceof LifecycleOwner){
            ((LifecycleOwner)onBoardStateChangedListener).getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    DefaultPaintBoard.this.onBoardStateChangedListener = null;
                    KLog.p("onBoardStateChangedListener destroyed");
                }
            });
        }
        return this;
    }


    void setOnPaintOpGeneratedListener(IOnPaintOpGeneratedListener paintOpGeneratedListener) {
        this.paintOpGeneratedListener = paintOpGeneratedListener;
    }
    interface IOnPaintOpGeneratedListener{
        void onOp(OpPaint opPaint);
    }



    DefaultTouchListener.IOnEventListener tmpPicViewEventListener = new DefaultTouchListener.IOnEventListener(){
        private float preDragX, preDragY;
        private float scaleCenterX, scaleCenterY;
        private final float scaleRateTopLimit = 3f;
        private final float scaleRateBottomLimit = 0.5f;

        @Override
        public boolean onDown(float x, float y) {
            KLog.p("onDown tmp pic layer, x=%s. y=%s", x, y);
            if (picEditStuffs.isEmpty() && picOps.isEmpty()){
                return false; // 放弃处理后续事件
            }
            if (!picEditStuffs.isEmpty()){
                handler.removeMessages(MSGID_FINISH_EDIT_PIC);
                PicEditStuff picEditStuff = picEditStuffs.peekFirst(); // NOTE: 目前仅同时编辑一张图片
                if (picEditStuff.isInDelPicIcon(x, y)){
                    picEditStuff.delIcon.setPic(del_pic_active_icon);
                    if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
                }
            }
            return true;
        }


        @Override
        public void onUp(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                if (picEditStuff.isInDelPicIcon(x, y)){
                    handler.removeMessages(MSGID_FINISH_EDIT_PIC);
                    delPic(picEditStuffs.pollFirst());
                }else {
                    handler.sendEmptyMessageDelayed(MSGID_FINISH_EDIT_PIC, 3000);
                }
            }
        }

        @Override
        public void onSecondPointerDown(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                picEditStuff.delIcon.setPic(del_pic_icon);
                if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
            }
        }

        @Override
        public void onLastPointerLeft(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                if (picEditStuff.isInDelPicIcon(x, y)){
                    picEditStuff.delIcon.setPic(del_pic_active_icon);
                    if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
                }
            }
        }

        @Override
        public void onSingleTap(float x, float y) {
            if (!picEditStuffs.isEmpty()) {
                PicEditStuff picEditStuff = picEditStuffs.peekFirst();
                if (!picEditStuff.isInDashedRect(x, y)&&!picEditStuff.isInDelPicIcon(x,y)){
                    handler.removeMessages(MSGID_FINISH_EDIT_PIC);
                    finishEditPic(picEditStuffs.pollFirst());
                }
            }
        }

        @Override
        public void onDragBegin(float x, float y) {
            KLog.p("onDragBegin tmp pic layer, x=%s. y=%s", x, y);
            if (picEditStuffs.isEmpty()){
                return;
            }
            handler.removeMessages(MSGID_FINISH_EDIT_PIC);
            preDragX = x; preDragY = y;
        }

        @Override
        public void onDrag(float x, float y) {
            KLog.p("onDrag tmp pic layer, x=%s. y=%s", x, y);
            if (picEditStuffs.isEmpty()){
                return;
            }
            tmpPicViewMatrix.postTranslate(x-preDragX, y-preDragY);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
            preDragX = x; preDragY = y;
        }

        @Override
        public void onScaleBegin() {
            if (picEditStuffs.isEmpty()){
                return;
            }
            handler.removeMessages(MSGID_FINISH_EDIT_PIC);
            scaleCenterX = getWidth()/2;
            scaleCenterY = getHeight()/2;
        }

        @Override
        public void onScale(float factor) {
            KLog.p("onScale tmp pic layer, factor=%s", factor);
            if (picEditStuffs.isEmpty()){
                return;
            }
            tmpPicViewMatrix.postScale(factor, factor, scaleCenterX, scaleCenterY);
            if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        }

    };


    private boolean bInsertingPic = false;
    @Override
    public void insertPic(String path) {
        if (null == publisher){
            KLog.p(KLog.ERROR,"publisher is null");
            return;
        }

        handler.removeMessages(MSGID_FINISH_EDIT_PIC);
        if (!picEditStuffs.isEmpty()){
            finishEditPic(picEditStuffs.pollFirst());
        }

        bInsertingPic = true;

        Bitmap bt = BitmapFactory.decodeFile(path);
        int picW = bt.getWidth();
        int picH = bt.getHeight();
        float transX = (getWidth()-picW)/2f;
        float transY = (getHeight()-picH)/2f;
        Matrix matrix = new Matrix();
        matrix.setTranslate(transX, transY);
        OpInsertPic op = new OpInsertPic(path, new Matrix(), matrix);
        op.setPic(bt);
        assignBasicInfo(op);

        if (null != paintOpGeneratedListener) {
            editPic(op);
        }
    }


    private static int MSGID_FINISH_EDIT_PIC = 666;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (MSGID_FINISH_EDIT_PIC == msg.what){
                if (!picEditStuffs.isEmpty()) {
                    finishEditPic(picEditStuffs.pollFirst());
                }
            }
        }
    };


    private void finishEditPic(PicEditStuff picEditStuff){
        KLog.sp("picEditStuffs.size="+ picEditStuffs.size());

        OpInsertPic opInsertPic = picEditStuff.pic;
        Matrix increasedMatrix = new Matrix(tmpPicViewMatrix);
        increasedMatrix.postConcat(MatrixHelper.invert(boardMatrix));
        opInsertPic.getMatrix().postConcat(increasedMatrix);
        opInsertPic.setBoardMatrix(boardMatrix);

        picOps.offerLast(opInsertPic);

        if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);

        // 发布
        if (bInsertingPic) {
            // 正在插入图片
            publisher.publish(opInsertPic);
            bInsertingPic = false;
        } else {
            // 正在拖动放缩图片
            Matrix matrix = new Matrix(opInsertPic.getInitRelativeMatrix());
            matrix.postConcat(opInsertPic.getBoardMatrix());
            Map<String, Matrix> picMatrices = new HashMap<>();
            picMatrices.put(opInsertPic.getPicId(), matrix);
            OpDragPic opDragPic = new OpDragPic(picMatrices);
            assignBasicInfo(opDragPic);
            publisher.publish(opDragPic);
        }

        focusedLayer = savedLayerBeforeEditPic;

        // 清空tmpPaintView设置。
        tmpPicViewMatrix.reset();

        KLog.p("picEditStuffs.size=%s", picEditStuffs.size());
    }


    private void delPic(PicEditStuff picEditStuff){
        OpInsertPic opInsertPic = picEditStuff.pic;

        focusedLayer = savedLayerBeforeEditPic;
        tmpPicViewMatrix.reset();
        if (null != paintOpGeneratedListener) paintOpGeneratedListener.onOp(null);
        if (bInsertingPic) {
            // publisher.publish(opDelPic); 如果是正在插入中就删除就不用走发布
            bInsertingPic = false;
        }else{
            if (null == opInsertPic){
                KLog.p(KLog.ERROR,"null == opInsertPic");
                return;
            }
            OpDeletePic opDeletePic = new OpDeletePic(new String[]{opInsertPic.getPicId()});
            assignBasicInfo(opDeletePic);
            publisher.publish(opDeletePic);
        }
    }


    private OpInsertPic selectPic(float x, float y){
        RectF picBoundary = new RectF();
        Matrix matrix = new Matrix();
        Iterator<OpPaint> it = picOps.descendingIterator();
        while (it.hasNext()){
            OpPaint op = it.next();
            if (op instanceof OpInsertPic){
                OpInsertPic opInsertPic = (OpInsertPic) op;
                if (null == opInsertPic.getPic()){
                    // 图片操作有但图片可能还未获取到（如，协作方上传图片尚未完成）
                    continue;
                }
                picBoundary.set(0, 0, opInsertPic.getPicWidth(), opInsertPic.getPicHeight());
                matrix.set(opInsertPic.getMatrix());
                matrix.postConcat(boardMatrix);
                matrix.mapRect(picBoundary);
                KLog.p("x=%s, y=%s, mappedPicBoundary=%s, matrix=%s", x, y, picBoundary, opInsertPic.getPicWidth(), opInsertPic.getPicHeight(), matrix);
                if (picBoundary.contains(x, y)){
                    return opInsertPic;
                }
            }
        }
        return null;
    }

    private int savedLayerBeforeEditPic;
    private static final int DASH_RECT_PADDING = 5; // 图片编辑时的虚线矩形框和图片之间的间隙。单位：pixel
    private static final int DEL_ICON_TOP_PADDING = 8; // 图片编辑时的虚线矩形框和删除图标之间的间隙。单位：pixel
    private static final int DASH_RECT_STROKE_WIDTH = 2; // 图片编辑时的虚线矩形框粗细。单位：pixel
    private static final long DASH_RECT_COLOR = 0xFF08b1f2L; // 图片编辑时的虚线矩形框颜色。
    private void editPic(OpInsertPic opInsertPic){

        // 在图片外围绘制一个虚线矩形框
        OpDrawRect opDrawRect = new OpDrawRect();
        float[] rectVal = new float[4];
        rectVal[0] = -DASH_RECT_PADDING;
        rectVal[1] = -DASH_RECT_PADDING;
        rectVal[2] = opInsertPic.getPicWidth()+DASH_RECT_PADDING;
        rectVal[3] = opInsertPic.getPicHeight()+DASH_RECT_PADDING;
        opInsertPic.getMatrix().mapPoints(rectVal);
        opDrawRect.setValues(rectVal);
        opDrawRect.setLineStyle(OpDraw.DASH);
        opDrawRect.setStrokeWidth(DASH_RECT_STROKE_WIDTH);
        opDrawRect.setColor(DASH_RECT_COLOR);

        // 在虚线矩形框正下方绘制删除图标
        float transX = rectVal[0]+(rectVal[2]-rectVal[0]-del_pic_icon.getWidth())/2f;
        float transY = opDrawRect.getBottom()+DEL_ICON_TOP_PADDING;
        Matrix matrix = new Matrix();
        matrix.postTranslate(transX, transY);
        float scale = MatrixHelper.getScale(tmpPicViewMatrix);
        matrix.postScale(1/scale, 1/scale, (rectVal[0]+rectVal[2])/2, rectVal[3]); // 使图标以正常尺寸展示，不至于因画板缩小/放大而过小/过大
        OpInsertPic delPicIcon = new OpInsertPic();
        delPicIcon.setPic(del_pic_icon);
        delPicIcon.setMatrix(matrix);

        PicEditStuff picEditStuff = new PicEditStuff(opInsertPic, delPicIcon, opDrawRect);

        picEditStuffs.offerLast(picEditStuff);

        paintOpGeneratedListener.onOp(null);

        savedLayerBeforeEditPic = focusedLayer;
        focusedLayer = LAYER_PIC_TMP;
        // 3秒过后画到图片画板上并清除临时画板
        handler.sendEmptyMessageDelayed(MSGID_FINISH_EDIT_PIC, 3000);
    }



    static int editedPicCount=0;
    class PicEditStuff{
        int id;
        OpInsertPic pic;
        OpInsertPic delIcon;
        OpDrawRect dashedRect;

        PicEditStuff(OpInsertPic pic, OpInsertPic delIcon, OpDrawRect dashedRect) {
            id = editedPicCount++;
            this.pic = pic;
            this.delIcon = delIcon;
            this.dashedRect = dashedRect;
        }

        boolean isInDashedRect(float x, float y){
            RectF rectF = new RectF(dashedRect.getLeft(), dashedRect.getTop(), dashedRect.getRight(), dashedRect.getBottom());
            tmpPicViewMatrix.mapRect(rectF);
            KLog.p("x=%s, y=%s, rect=%s, tmpPicViewMatrix=%s", x, y, rectF, tmpPicViewMatrix);
            return rectF.contains(x, y);
        }

        boolean isInDelPicIcon(float x, float y){
            RectF rectF = new RectF(0, 0, delIcon.getPicWidth(), delIcon.getPicHeight());
            delIcon.getMatrix().mapRect(rectF);
            tmpPicViewMatrix.mapRect(rectF);
            KLog.p("x=%s, y=%s, rect=%s, tmpPicViewMatrix=%s", x, y, rectF, tmpPicViewMatrix);
            return rectF.contains(x, y);
        }

    }



}
