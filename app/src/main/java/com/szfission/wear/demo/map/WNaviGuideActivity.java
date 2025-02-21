/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.szfission.wear.demo.map;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.model.BikeNaviLocationResult;
import com.baidu.mapapi.bikenavi.model.BikeRouteResult;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.adapter.IWTTSPlayer;
import com.baidu.mapapi.walknavi.model.IWRouteIconInfo;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.mapapi.walknavi.model.WalkNaviDisplayOption;
import com.baidu.mapapi.walknavi.model.WalkNaviLocationResult;
import com.baidu.mapapi.walknavi.model.WalkRouteResult;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.BdWatchMapGpsStatusChange;
import com.fission.wear.sdk.v2.bean.BdWatchMapRemainDistance;
import com.fission.wear.sdk.v2.bean.BdWatchMapRemainRoute;
import com.fission.wear.sdk.v2.bean.BdWatchMapRemainTime;
import com.fission.wear.sdk.v2.bean.BdWatchMapRoadGuideIcon;
import com.fission.wear.sdk.v2.bean.BdWatchMapRoadGuideText;
import com.fission.wear.sdk.v2.bean.BdWatchMapRoutePlanRecInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapRouteYawReminder;
import com.fission.wear.sdk.v2.bean.HiSiWatchReqTask;
import com.fission.wear.sdk.v2.callback.HiSiliconDataResultListener;
import com.fission.wear.sdk.v2.constant.BdWatchMsgType;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdHelper;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdID;
import com.fission.wear.sdk.v2.utils.HiSiTaskManage;
import com.fission.wear.sdk.v2.utils.SvgTaskManage;

import java.util.ArrayList;
import java.util.List;


public class WNaviGuideActivity extends Activity {

    private final static String TAG = WNaviGuideActivity.class.getSimpleName();

    private WalkNavigateHelper mNaviHelper;

    private long lastRemainTime;

    private long lastIconUpdate;

    private long lastTextUpdate;

    private long lastRemainDistance;

    private long lastFarAway;

    private long lastPlanYawing;

    private long lastArriveDest;

    private long lastGpsChange;

    private final static int CALLBACK_INTERVAL = 3000;

    private HiSiliconDataResultListener hiSiliconDataResultListener;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviHelper.quit();
        mNaviHelper.unInitNaviEngine();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNaviHelper.pause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNaviHelper = WalkNavigateHelper.getInstance();
        WalkNaviDisplayOption walkNaviDisplayOption = new WalkNaviDisplayOption()
                .showImageToAr(true) // 是否展示AR图片
                .showCalorieLayoutEnable(true) // 是否展示热量消耗布局
                .showLocationImage(true);  // 是否展示视角切换资源

        mNaviHelper.setWalkNaviDisplayOption(walkNaviDisplayOption);

        try {
            View view = mNaviHelper.onCreate(WNaviGuideActivity.this);
            if (view != null) {
                setContentView(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(hiSiliconDataResultListener == null){
            hiSiliconDataResultListener = new HiSiliconDataResultListener() {
                @Override
                public void sendSuccess(String cmdId) {

                }

                @Override
                public void sendFail(String cmdId) {

                }

                @Override
                public void onResultTimeout(String cmdId) {

                }

                @Override
                public void onResultError(String errorMsg) {

                }

                @Override
                public void destroyNaviByWatch() {
                    super.destroyNaviByWatch();
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_NAVI_DESTROY);
                    recInfo.setNavi_type(0);
                    StringBuffer sb = new StringBuffer();
                    sb.append("百度手表矢量地图结束步行导航:"+ GsonUtils.toJson(recInfo));
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onNaviDestroyRec(GsonUtils.toJson(recInfo));
                    ToastUtils.showLong(sb.toString());
                    finish();
                }
            };
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(hiSiliconDataResultListener);
        FissionSdkBleManage.getInstance().addCmdResultListener(hiSiliconDataResultListener);

        mNaviHelper.setWalkNaviStatusListener(new IWNaviStatusListener() {
            @Override
            public void onWalkNaviModeChange(int mode, WalkNaviModeSwitchListener listener) {
                Log.d(TAG, "onWalkNaviModeChange : " + mode);
                mNaviHelper.switchWalkNaviMode(WNaviGuideActivity.this, mode, listener);
            }

            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
                BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                recInfo.setMsg_type(BdWatchMsgType.ON_NAVI_DESTROY);
                recInfo.setNavi_type(0);
                StringBuffer sb = new StringBuffer();
                sb.append("百度手表矢量地图结束步行导航:"+ GsonUtils.toJson(recInfo));
                LogUtils.d("wl", sb.toString());
                FissionSdkBleManage.getInstance().onNaviDestroyRec(GsonUtils.toJson(recInfo));
                ToastUtils.showLong(sb.toString());
            }
        });

        mNaviHelper.setTTsPlayer(new IWTTSPlayer() {
            @Override
            public int playTTSText(final String s, boolean b) {
                Log.d(TAG, "tts: " + s);
                return 0;
            }
        });

        boolean startResult = mNaviHelper.startWalkNavi(WNaviGuideActivity.this);
        Log.e(TAG, "startWalkNavi result : " + startResult);

        mNaviHelper.setRouteGuidanceListener(this, new IWRouteGuidanceListener() {
            @Override
            public void onRouteGuideIconInfoUpdate(IWRouteIconInfo routeIconInfo) {
                if (routeIconInfo != null) {
                    Log.d(TAG, "onRoadGuideTextUpdate   Drawable=: " + routeIconInfo.getIconDrawable()
                            + " Name=: " + routeIconInfo.getIconName());
                }
            }

            @Override
            public void onRouteGuideIconUpdate(Drawable icon) {
                Log.d(TAG, "onRoadGuideTextUpdate   Drawable=: " + icon);
            }

            @Override
            public void onRouteGuideKind(RouteGuideKind routeGuideKind) {
                if(System.currentTimeMillis() - lastIconUpdate > CALLBACK_INTERVAL){
                    BdWatchMapRoadGuideIcon icon = new BdWatchMapRoadGuideIcon();
                    icon.setGuide_icon(routeGuideKind.ordinal());
                    icon.setMsg_type(BdWatchMsgType.ON_ROUTE_GUIDE_ICON_UPDATE);
                    icon.setNavi_type(0);

                    StringBuffer sb = new StringBuffer();
                    sb.append("步行导航中诱导图片回调:"+ GsonUtils.toJson(icon));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onRouteGuideIconUpdateRec(GsonUtils.toJson(icon));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROAD_GUIDE_ICON_UPDATE_REC), GsonUtils.toJson(icon));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb.toString());
                    lastIconUpdate = System.currentTimeMillis();
                }
            }

            @Override
            public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {
                if(System.currentTimeMillis() - lastTextUpdate > CALLBACK_INTERVAL){
                    BdWatchMapRoadGuideText text = new BdWatchMapRoadGuideText();
                    StringBuffer sb = new StringBuffer();
                    sb.append(charSequence);
                    sb.append("\n");
                    sb.append(charSequence1);
                    text.setGuide_info(sb.toString());
                    text.setMsg_type(BdWatchMsgType.ON_ROAD_GUIDE_TEXT_UPDATE);
                    text.setNavi_type(0);

                    StringBuffer sb2 = new StringBuffer();
                    sb2.append("步行导航中信息回调:"+ GsonUtils.toJson(text));
                    LogUtils.d("wl", sb2.toString());
//                FissionSdkBleManage.getInstance().onRoadGuideTextUpdateRec(GsonUtils.toJson(text));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROAD_GUIDE_TEXT_UPDATE_REC), GsonUtils.toJson(text));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb2.toString());

                    lastTextUpdate = System.currentTimeMillis();
                }
            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {
                if(System.currentTimeMillis() - lastRemainDistance > CALLBACK_INTERVAL){
                    BdWatchMapRemainDistance distance = new BdWatchMapRemainDistance();
                    distance.setRemain_distance((String) charSequence);
                    distance.setMsg_type(BdWatchMsgType.ON_REMAIN_DISTANCE_UPDATE);
                    distance.setNavi_type(0);

                    StringBuffer sb= new StringBuffer();
                    sb.append("步行导航中剩余里程回调:"+ GsonUtils.toJson(distance));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onRemainDistanceUpdateRec(GsonUtils.toJson(distance));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_DISTANCE_UPDATE_REC), GsonUtils.toJson(distance));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb.toString());

                    lastRemainDistance = System.currentTimeMillis();
                }
            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {
                if(System.currentTimeMillis() - lastRemainTime > CALLBACK_INTERVAL){
                    BdWatchMapRemainTime time = new BdWatchMapRemainTime();
                    time.setRemain_time((String) charSequence);
                    time.setMsg_type(BdWatchMsgType.ON_REMAIN_TIME_UPDATE);
                    time.setNavi_type(0);

                    StringBuffer sb= new StringBuffer();
                    sb.append("步行导航中剩余时间回调:"+ GsonUtils.toJson(time));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onRemainTimeUpdateRec(GsonUtils.toJson(time));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_TIME_UPDATE_REC), GsonUtils.toJson(time));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb.toString());

                    lastRemainTime = System.currentTimeMillis();
                }
            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onGpsStatusChange: charSequence = :" + charSequence);

            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
                if(System.currentTimeMillis() - lastFarAway > CALLBACK_INTERVAL){
                    BdWatchMapRouteYawReminder yawReminder = new BdWatchMapRouteYawReminder();
                    yawReminder.setMsg_type(BdWatchMsgType.ON_ROUTE_FAR_AWAY);
                    yawReminder.setNavi_type(0);
                    yawReminder.setYawing_msg((String) charSequence);

                    StringBuffer sb= new StringBuffer();
                    sb.append("步行导航开始偏航回调:"+ GsonUtils.toJson(yawReminder));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onRouteFarAwayRec(GsonUtils.toJson(yawReminder));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_FAR_AWAYE_REC), GsonUtils.toJson(yawReminder));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb.toString());

                    lastFarAway = System.currentTimeMillis();
                }
            }

            @Override
            public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {
                if(System.currentTimeMillis() - lastPlanYawing > CALLBACK_INTERVAL){
                    BdWatchMapRouteYawReminder yawReminder = new BdWatchMapRouteYawReminder();
                    yawReminder.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_YAWING);
                    yawReminder.setNavi_type(0);
                    yawReminder.setYawing_msg((String) charSequence);

                    StringBuffer sb= new StringBuffer();
                    sb.append("步行导航偏航规划中回调:"+ GsonUtils.toJson(yawReminder));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onRoutePlanYawingRec(GsonUtils.toJson(yawReminder));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_PLAN_YAWING_REC), GsonUtils.toJson(yawReminder));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb.toString());

                    lastPlanYawing = System.currentTimeMillis();
                }
            }

            @Override
            public void onReRouteComplete() {
                WalkRouteResult walkRouteResult = mNaviHelper.getWalkNaviRouteInfo();
                BdWatchMapRemainRoute remainRoute = new BdWatchMapRemainRoute();
                remainRoute.setMsg_type(BdWatchMsgType.ON_REROUTE_COMPLETE);
                remainRoute.setNavi_type(0);
                BdWatchMapRemainRoute.RouteLine routeLine = remainRoute.new RouteLine();
                routeLine.distance = walkRouteResult.getDistance();
                routeLine.duration = walkRouteResult.getDuration();
                List<BdWatchMapRemainRoute.Point> points = new ArrayList<>();
                ArrayList<LatLng> latLngs = walkRouteResult.getPositions();
                if(latLngs!=null && latLngs.size()>0){
                    for(LatLng latLng: latLngs){
                        BdWatchMapRemainRoute.Point point = remainRoute. new Point();
                        point.latitude = latLng.latitude;
                        point.longitude = latLng.longitude;
                        points.add(point);
                    }
                }
                routeLine.points = points;
                remainRoute.setLine(routeLine);

                StringBuffer sb= new StringBuffer();
                sb.append("百度手表步行导航重新算路成功回调："+GsonUtils.toJson(remainRoute));
                LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onReRouteCompleteRec(GsonUtils.toJson(remainRoute));

                SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REROUTE_COMPLETE_REC), GsonUtils.toJson(remainRoute)));
            }

            @Override
            public void onArriveDest() {
                if(System.currentTimeMillis() - lastArriveDest > CALLBACK_INTERVAL){
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ARRIVE_DEST);
                    recInfo.setNavi_type(0);
                    StringBuffer sb = new StringBuffer();
                    sb.append("步行导航到达目的地:"+ GsonUtils.toJson(recInfo));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onArriveDestRec(GsonUtils.toJson(recInfo));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ARRIVE_DEST_REC), GsonUtils.toJson(recInfo));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    ToastUtils.showLong(sb.toString());

                    lastArriveDest = System.currentTimeMillis();
                }
            }

            @Override
            public void onIndoorEnd(Message msg) {

            }

            @Override
            public void onFinalEnd(Message msg) {

            }

            @Override
            public void onVibrate() {

            }

            @Override
            public void onNaviLocationUpdate() {
                if(System.currentTimeMillis() - lastGpsChange > CALLBACK_INTERVAL){
                    WalkNaviLocationResult result = mNaviHelper.getWalkNaviLocationInfo();
                    BdWatchMapGpsStatusChange gpsStatusChange = new BdWatchMapGpsStatusChange();
                    gpsStatusChange.setMsg_type(BdWatchMsgType.ON_GPS_STATUS_CHANGE);
                    gpsStatusChange.setNavi_type(0);
                    gpsStatusChange.setLink_id(result.getCurRouteShapeIdx());
                    gpsStatusChange.setDirection(result.getPostDirection());

                    BdWatchMapGpsStatusChange.Point location = gpsStatusChange.new Point();
                    location.latitude = result.getPostLatitude();
                    location.longitude = result.getPostLongitude();
                    gpsStatusChange.setLocation(location);

                    BdWatchMapGpsStatusChange.Point originLocation = gpsStatusChange.new Point();
                    originLocation.latitude = result.getGpsLatitude();
                    originLocation.longitude = result.getGpsLongitude();
                    gpsStatusChange.setOrigin_location(originLocation);

                    StringBuffer sb= new StringBuffer();
                    sb.append("步行导航行程中位置回调："+GsonUtils.toJson(gpsStatusChange));
                    LogUtils.d("wl", sb.toString());
//                FissionSdkBleManage.getInstance().onGpsStatusChangeRec(GsonUtils.toJson(gpsStatusChange));
                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_GPS_STATUS_CHANGE_REC), GsonUtils.toJson(gpsStatusChange));
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);

                    lastGpsChange = System.currentTimeMillis();
                }

            }
        });


    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == ArCameraView.WALK_AR_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(WNaviGuideActivity.this, "没有相机权限,请打开后重试", Toast.LENGTH_SHORT).show();
//            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                mNaviHelper.startCameraAndSetMapView(WNaviGuideActivity.this);
//            }
//        }
//    }
}
