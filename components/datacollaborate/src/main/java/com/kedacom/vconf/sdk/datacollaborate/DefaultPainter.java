package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.PainterInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

class DefaultPainter implements IPainter {

    private int role = ROLE_COPIER;

    private Map<String, DefaultPaintBoard> paintBoards = new LinkedHashMap<>();

    private String curBoardId;

    private boolean bDirty = false;
    private boolean bPaused = false;
    private final Object renderLock = new Object();

    private HandlerThread handlerThread;
    private Handler handler;

    private PainterInfo painterInfo;

    public DefaultPainter(@NonNull Context context, @NonNull PainterInfo painterInfo, LifecycleOwner lifecycleOwner) {

        this.painterInfo = painterInfo;

        if (null != lifecycleOwner){
            lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver(){
                @Override
                public void onCreate(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s created", owner);
                    start();
                }

                @Override
                public void onResume(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s resumed", owner);
                    resume();
                }

                @Override
                public void onPause(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s to be paused", owner);
                    pause();
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    KLog.p("LifecycleOwner %s to be destroyed", owner);
                    destroy();
                }
            });
        }


        handlerThread = new HandlerThread("PainterAss", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void start() {
        if (!renderThread.isAlive()){
            renderThread.start();
        }
    }

    /**
     * 调用该方法暂停绘制
     * */
    @Override
    public void pause() {
        synchronized (renderLock) {
            bPaused = true;
        }
    }

    /**
     * 调用{@link #pause()}后再调用该方法恢复正常运行状态。
     * */
    @Override
    public void resume() {
        synchronized (renderLock) {
            bPaused = false;
        }
        refresh();
    }


    @Override
    public void destroy() {
        if (renderThread.isAlive()) {
            renderThread.interrupt();
        }
        handler.removeCallbacksAndMessages(null);
        handlerThread.quit();
        deleteAllPaintBoards();
    }


    /**
     * 刷新画板
     * */
    private void refresh(){
        handler.removeCallbacks(refreshRunnable);
        long timestamp = System.currentTimeMillis();
        if (timestamp-refreshTimestamp>20){
            refreshTimestamp = timestamp;
            doRefresh();
        }else{
            handler.postDelayed(refreshRunnable, 20);
        }
    }
    private long refreshTimestamp = System.currentTimeMillis();
    private Runnable refreshRunnable = this::doRefresh;
    /**
     * 刷新画板
     * */
    private void doRefresh(){
        synchronized (renderLock){
            bDirty = true;
            renderLock.notify();
        }
    }



    @Override
    public boolean addPaintBoard(@NonNull IPaintBoard paintBoard) {
        String boardId = paintBoard.getBoardId();
        if (paintBoards.containsKey(boardId)){
            KLog.p(KLog.ERROR,"board %s already exist!", boardId);
            return false;
        }
        DefaultPaintBoard defaultPaintBoard = (DefaultPaintBoard) paintBoard;
        defaultPaintBoard.setEnableManipulate(ROLE_AUTHOR==role);
        defaultPaintBoard.setOnStateChangedListener(onStateChangedListener);
        paintBoards.put(boardId, defaultPaintBoard);
        KLog.p(KLog.WARN,"board %s added", paintBoard.getBoardId());

        return true;
    }

    @Nullable
    @Override
    public IPaintBoard deletePaintBoard(@NonNull String boardId) {
        KLog.p(KLog.WARN,"delete board %s", boardId);
        DefaultPaintBoard board =  paintBoards.remove(boardId);
        if (null != board){
            if (board.getBoardId().equals(curBoardId)){
                curBoardId = null;
            }
            board.setEnableManipulate(false);
            board.setOnStateChangedListener(null);
        }
        return board;
    }

    @Override
    public void deleteAllPaintBoards() {
        KLog.p(KLog.WARN,"delete all boards");
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setEnableManipulate(false);
            board.setOnStateChangedListener(null);
        }
        paintBoards.clear();
        curBoardId = null;
    }


    @Nullable
    @Override
    public IPaintBoard switchPaintBoard(@NonNull String boardId) {
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no such board %s", boardId);
            return null;
        }
        KLog.p(KLog.WARN, "switched board from %s to %s", curBoardId, boardId);
        curBoardId = boardId;

        return paintBoard;
    }

    @Nullable
    @Override
    public IPaintBoard getPaintBoard(@NonNull String boardId) {
        return paintBoards.get(boardId);
    }

    /**
     * 获取所有画板
     * @return 所有画板列表，顺序同添加时的顺序。
     * */
    @Override
    public List<IPaintBoard> getAllPaintBoards() {
        return new ArrayList<>(paintBoards.values());
    }

    @Nullable
    @Override
    public IPaintBoard getCurrentPaintBoard(){
        if (null == curBoardId) {
            KLog.p(KLog.WARN, "current board is null");
            return null;
        }
        return paintBoards.get(curBoardId);
    }

    @Override
    public int getPaintBoardCount() {
        return paintBoards.size();
    }


    @Override
    public void paint(@NonNull OpPaint op) {
        String boardId = op.getBoardId();
        DefaultPaintBoard paintBoard = paintBoards.get(boardId);
        if(null == paintBoard){
            KLog.p(KLog.ERROR,"no board %s for op %s", boardId, op);
            return;
        }

        paintBoard.onPaintOp(op);
    }


    @Override
    public void setRole(int role) {
        this.role = role;
        for (DefaultPaintBoard board : paintBoards.values()){
            board.setEnableManipulate(ROLE_AUTHOR==role);
        }
    }


    private IOnBoardStateChangedListener onBoardStateChangedListener;
    @Override
    public void setOnBoardStateChangedListener(IOnBoardStateChangedListener onBoardStateChangedListener) {
        this.onBoardStateChangedListener = onBoardStateChangedListener;
    }


    private DefaultPaintBoard.IOnStateChangedListener onStateChangedListener = new DefaultPaintBoard.IOnStateChangedListener() {
        @Override
        public void onDirty(String boardId) {
            if (boardId.equals(curBoardId)) {
                refresh();
            }
        }

        @Override
        public void onPaintOpGenerated(String boardId, OpPaint op, IResultListener publishResultListener) {
            op.setAuthorE164(painterInfo.getE164());
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onPaintOpGenerated(boardId, op, publishResultListener);
        }

        @Override
        public void onChanged(String boardId) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onChanged(boardId);
        }

        @Override
        public void onPictureCountChanged(String boardId, int count) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onPictureCountChanged(boardId, count);
        }

        @Override
        public void onZoomRateChanged(String boardId, int percentage) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onZoomRateChanged(boardId, percentage);
        }

        @Override
        public void onRepealableStateChanged(String boardId, int repealedOpsCount, int remnantOpsCount) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onRepealableStateChanged(boardId, repealedOpsCount, remnantOpsCount);
        }

        @Override
        public void onWcRevocableStateChanged(String boardId, int revocableOpsCount, int restorableOpsCount) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onWcRevocableStateChanged(boardId, revocableOpsCount, restorableOpsCount);
        }

        @Override
        public void onEmptyStateChanged(String boardId, boolean bEmptied) {
            if (null != onBoardStateChangedListener) onBoardStateChangedListener.onEmptyStateChanged(boardId, bEmptied);
        }
    };



    private final Thread renderThread = new Thread("DCRenderThr"){
        private boolean bNeedRender;

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (true){
                KLog.p(KLog.DEBUG, "start loop run");
                if (isInterrupted()){
                    KLog.p(KLog.WARN, "quit renderThread");
                    return;
                }

                // 判断当前是否有渲染任务
                synchronized (renderLock) {
                    bNeedRender = bDirty && !bPaused;
                    try {
                        if (!bNeedRender) {
                            do{
                                KLog.p(KLog.DEBUG, "waiting...");
                                renderLock.wait();
                                bNeedRender = bDirty && !bPaused;
                                KLog.p(KLog.DEBUG, "awaken! bNeedRender=%s", bNeedRender);
                            }while (!bNeedRender);
                        }
                    } catch (InterruptedException e) {
                        KLog.p(KLog.WARN, "quit renderThread");
                        return;
                    }

                    bDirty = false;

                }

                // 获取当前画板
                DefaultPaintBoard paintBoard = paintBoards.get(curBoardId);
                if (null != paintBoard){
                    paintBoard.paint();
                }

            }
        }


    };


}
