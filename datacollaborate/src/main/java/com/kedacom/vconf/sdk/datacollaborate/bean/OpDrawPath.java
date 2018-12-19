package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

public class OpDrawPath extends OpDraw {
    private List<PointF> points;
    private Path path;

    public OpDrawPath(List<PointF> points){
        if (null != points) {
            this.points = points;
            path = new Path();
            if (points.size()>=3){
                PointF pointF = points.get(0);
                path.moveTo(pointF.x, pointF.y);
                pointF = points.get(1);
//                path.lineTo(pointF.x, pointF.y);
                PointF prePoint = pointF;
                float midX, midY;
                int i=2;
                for (; i<points.size()-1; ++i){
                    pointF = points.get(i);
                    midX = (prePoint.x+pointF.x)/2;
                    midY = (prePoint.y+pointF.y)/2;
                    path.quadTo(prePoint.x, prePoint.y, midX, midY);
                    prePoint = pointF;
                }
                pointF = points.get(i);
                path.lineTo(pointF.x, pointF.y);
            }else if(!points.isEmpty()){
                path.moveTo(points.get(0).x, points.get(0).y);
                if (points.size()==2){
                    path.lineTo(points.get(1).x, points.get(1).y);
                }
            }
        }
        type = EOpType.DRAW_PATH;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+"path="+path+super.toString()+"}";
    }

    public List<PointF> getPoints() {
        return points;
    }

    public Path getPath() {
        return path;
    }
}
