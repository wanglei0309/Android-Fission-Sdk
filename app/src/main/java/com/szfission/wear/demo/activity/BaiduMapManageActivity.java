package com.szfission.wear.demo.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.ISVGLicenseListener;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.SVGLicenseManager;
import com.baidu.mapapi.SVGLicenseOption;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.model.BikeRouteResult;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeInfo;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiChildrenInfo;
import com.baidu.mapapi.search.core.PoiDetailInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.core.VehicleInfo;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.IntegralRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.search.svg.OnGetSVGTileBatchSearchResultListener;
import com.baidu.mapapi.search.svg.OnGetSVGTileSearchResultListener;
import com.baidu.mapapi.search.svg.SVGTileResult;
import com.baidu.mapapi.search.svg.SVGTileSearch;
import com.baidu.mapapi.search.svg.SVGTileSearchOption;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.model.WalkRouteResult;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;
import com.blankj.utilcode.util.CacheDoubleUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ZipUtils;
import com.fission.wear.sdk.v2.FissionSdkBleManage;
import com.fission.wear.sdk.v2.bean.BaseBdWatchJsonData;
import com.fission.wear.sdk.v2.bean.BdCoord;
import com.fission.wear.sdk.v2.bean.BdWatchGeocodeSearchRec;
import com.fission.wear.sdk.v2.bean.BdWatchGeocodeSearchReq;
import com.fission.wear.sdk.v2.bean.BdWatchGeolocationReq;
import com.fission.wear.sdk.v2.bean.BdWatchLocation;
import com.fission.wear.sdk.v2.bean.BdWatchMapAuthLicense;
import com.fission.wear.sdk.v2.bean.BdWatchMapBiKingResult;
import com.fission.wear.sdk.v2.bean.BdWatchMapBusResult;
import com.fission.wear.sdk.v2.bean.BdWatchMapGeolocation;
import com.fission.wear.sdk.v2.bean.BdWatchMapNaviInitReqInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapPoi;
import com.fission.wear.sdk.v2.bean.BdWatchMapPoiReq;
import com.fission.wear.sdk.v2.bean.BdWatchMapRemainRoute;
import com.fission.wear.sdk.v2.bean.BdWatchMapRoutePlanRecInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapSugSearchRecInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapSugSearchReqInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapSvgRecInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapSvgReqInfo;
import com.fission.wear.sdk.v2.bean.BdWatchMapWalkResult;
import com.fission.wear.sdk.v2.bean.BdWatchReGeocodeSearchRec;
import com.fission.wear.sdk.v2.bean.BdWatchReGeocodeSearchReq;
import com.fission.wear.sdk.v2.bean.BdWatchRouteRidingSearch;
import com.fission.wear.sdk.v2.bean.BdWatchRouteSearch;
import com.fission.wear.sdk.v2.bean.BdWatchRouteTransitSearch;
import com.fission.wear.sdk.v2.bean.HiSiWatchReqTask;
import com.fission.wear.sdk.v2.bean.JsAiJsonResult;
import com.fission.wear.sdk.v2.bean.JsAiVoiceJsonResult;
import com.fission.wear.sdk.v2.bean.SvgTaskInfo;
import com.fission.wear.sdk.v2.bean.TransferNotify;
import com.fission.wear.sdk.v2.callback.HiSiliconDataResultListener;
import com.fission.wear.sdk.v2.constant.BdWatchMsgType;
import com.fission.wear.sdk.v2.constant.FissionConstant;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdHelper;
import com.fission.wear.sdk.v2.parse.HiSiliconSppCmdID;
import com.fission.wear.sdk.v2.utils.BaiDuAiUtils;
import com.fission.wear.sdk.v2.utils.BdMapFileManage;
import com.fission.wear.sdk.v2.utils.FissionLogUtils;
import com.fission.wear.sdk.v2.utils.HiSiTaskManage;
import com.fission.wear.sdk.v2.utils.HiSiliconFileTransferUtils;
import com.fission.wear.sdk.v2.utils.SvgTaskManage;
import com.szfission.wear.demo.R;
import com.szfission.wear.demo.map.BNaviGuideActivity;
import com.szfission.wear.demo.map.WNaviGuideActivity;
import com.szfission.wear.demo.util.BMFTime;
import com.szfission.wear.demo.util.ParseErrorCodeUtils;
import com.szfission.wear.sdk.util.RxTimerUtil;

import org.json.JSONException;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BaiduMapManageActivity extends BaseActivity {

    TextView tv_log;

    private PoiSearch mPoiSearch;

    private RoutePlanSearch mRoutePlanSearch;

    public StringBuffer sb;

    private int dataBlockNum = 50;

    private int dataBlockNumCount; //数据总包数

    private int dataBlockSize = 965;


    private int fileSize = 0;

    private String fileName;

    byte[] resultData = null;

    private int framesCount =0; //文件数据分帧

    private int curFrames = 0; //当前帧数

    private List<String> fileDataHexList;

    private long startTime;

    private String curCmdId;

    private BdWatchMapSvgRecInfo bdWatchMapSvgRecInfo;

    private SuggestionSearch mSuggestionSearch;

    private int naviType;

    private BikeNaviLaunchParam mBikeNaviLaunchParam;

    private WalkNaviLaunchParam mWalkNaviLaunchParam;

    private int errorCode =0;

    private HiSiliconDataResultListener hiSiliconDataResultListener;

    private BdMapFileManage.BdMapFileTransportListener bdMapFileTransportListener;

    private HiSiTaskManage.HiSiWatchReqTaskListener hiSiWatchReqTaskListener;

    private SvgTaskManage.HiSiWatchReqTaskListener svgReqTaskListener;

    private RxTimerUtil rxTimerUtil;

    private final static int MAX_PRELOADING_NUM = 10; //预制请求svg数量 30*30

    private int preloadSvgNum =0;

    private long svgTaskStartTime = 0;

    private String lastJsonData;

    private int tryNum = 0;

    private final static int MAX_TRY_NUM =3;

    private LocationClient mLocationClient = null;
    private BDAbstractLocationListener myListener = new MyLocationListener();

    private GeoCoder mCoder;

    private boolean isFirstBatch = false;

    private boolean isSendSvgNotice = false;

    private Button btn_open_map, btn_send_svg_list, btn_route_plan_start, btn_start_navigation_riding_init, btn_start_navigation_step_init, btn_sug_search,
            btn_license, btn_send_svg, btn_send_dst_list, btn_push_navigation, btn_search_navigation_riding, btn_search_navigation_step, btn_start_navigation_bus, btn_send_poi,
            btn_start_navigation_riding, btn_start_navigation_step, btn_end_navigation, btn_location_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baidu_map);
        String[] testArray = getResources().getStringArray(R.array.haisi_test_array);
        setTitle(testArray[1]);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        sb = new StringBuffer();

        tv_log = findViewById(R.id.tv_log);

        init();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openMap() {
    }

    private void sendSvgFileList() {

        sendPreLoadingSvgFileList();
    }

    private void routePlanStart() {
//起终点位置
        LatLng startPt = new LatLng(40.047416,116.312143);
        LatLng endPt = new LatLng(40.048424, 116.313513);
//构造BikeNaviLaunchParam
//.vehicle(0)默认的普通骑行导航
        if(naviType == 0){
            WalkRouteNodeInfo startNodeInfo = new WalkRouteNodeInfo();
            startNodeInfo.setLocation(startPt);
            WalkRouteNodeInfo endNodeInfo = new WalkRouteNodeInfo();
            endNodeInfo.setLocation(endPt);
            mWalkNaviLaunchParam = new WalkNaviLaunchParam().startNodeInfo(startNodeInfo).endNodeInfo(endNodeInfo);
            //发起算路
            WalkNavigateHelper.getInstance().routePlanWithRouteNode(mWalkNaviLaunchParam, new IWRoutePlanListener() {
                @Override
                public void onRoutePlanStart() {
                    //开始算路的回调
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_START);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路开始："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanStartRec(GsonUtils.toJson(recInfo));
                }

                @Override
                public void onRoutePlanSuccess() {
                    //算路成功
                    //跳转至诱导页面
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_SUCCESS);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路成功："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanSuccessRec(GsonUtils.toJson(recInfo));
                }

                @Override
                public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {
                    //算路失败的回调
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_FAIL);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路失败："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanFailRec(GsonUtils.toJson(recInfo));
                }
            });
        }else if(naviType == 1){
            BikeRouteNodeInfo startNodeInfo = new BikeRouteNodeInfo();
            startNodeInfo.setLocation(startPt);
            BikeRouteNodeInfo endNodeInfo = new BikeRouteNodeInfo();
            endNodeInfo.setLocation(endPt);
            mBikeNaviLaunchParam = new BikeNaviLaunchParam().startNodeInfo(startNodeInfo).endNodeInfo(endNodeInfo).vehicle(0);
            //发起算路
            BikeNavigateHelper.getInstance().routePlanWithRouteNode(mBikeNaviLaunchParam, new IBRoutePlanListener() {
                @Override
                public void onRoutePlanStart() {
                    //执行算路开始的逻辑
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_START);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路开始："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanStartRec(GsonUtils.toJson(recInfo));
                }

                @Override
                public void onRoutePlanSuccess() {
                    //算路成功
                    //跳转至诱导页面
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_SUCCESS);
                    recInfo.setNavi_type(naviType);

                    sb.append("\n");
                    sb.append("百度手表导航算路成功："+GsonUtils.toJson(recInfo));
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanSuccessRec(GsonUtils.toJson(recInfo));

                    BikeRouteResult bikeRouteResult = BikeNavigateHelper.getInstance().getBikeNaviRouteInfo();
                    if(bikeRouteResult == null){
                        return;
                    }
                    BdWatchMapRemainRoute remainRoute = new BdWatchMapRemainRoute();
                    remainRoute.setMsg_type(BdWatchMsgType.ON_REMAIN_ROUTE_UPDATE);
                    remainRoute.setNavi_type(1);
                    BdWatchMapRemainRoute.RouteLine routeLine = remainRoute.new RouteLine();
                    routeLine.distance = bikeRouteResult.getDistance();
                    routeLine.duration = bikeRouteResult.getDuration();
                    List<BdWatchMapRemainRoute.Point> points = new ArrayList<>();
                    ArrayList<LatLng> latLngs = bikeRouteResult.getPositions();
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

                    sb.append("\n");
                    sb.append("百度手表导航路线回调："+GsonUtils.toJson(remainRoute));
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRemainRouteUpdateRec(GsonUtils.toJson(remainRoute));
                }

                @Override
                public void onRoutePlanFail(BikeRoutePlanError bikeRoutePlanError) {
                    //执行算路失败的逻辑
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_FAIL);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路失败："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanFailRec(GsonUtils.toJson(recInfo));
                }
            });
        }else{

        }
    }

    private void ridingNaviInit() {
        // 获取导航控制类
// 引擎初始化
        BikeNavigateHelper.getInstance().initNaviEngine(this, new IBEngineInitListener() {
            @Override
            public void engineInitSuccess() {
                naviType =1;
                //骑行导航初始化成功之后的回调
                sb.append("\n");
                sb.append("骑行导航初始化成功");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", "骑行导航初始化成功");
                FissionSdkBleManage.getInstance().onNaviInitRec("0");

            }

            @Override
            public void engineInitFail() {
                //骑行导航初始化失败之后的回调
                sb.append("\n");
                sb.append("骑行导航初始化失败");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", "骑行导航初始化失败");
                FissionSdkBleManage.getInstance().onNaviInitRec("1");
            }
        });
    }

    private void walkingNaviInit() {
        // 获取导航控制类
// 引擎初始化
        WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {

            @Override
            public void engineInitSuccess() {
                //引擎初始化成功的回调
                naviType =0;
                //骑行导航初始化成功之后的回调
                sb.append("\n");
                sb.append("步行导航初始化成功");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", "骑行导航初始化成功");
                FissionSdkBleManage.getInstance().onNaviInitRec("0");
            }

            @Override
            public void engineInitFail() {
                //引擎初始化失败的回调
                sb.append("\n");
                sb.append("步行导航初始化失败");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", "骑行导航初始化失败");
                FissionSdkBleManage.getInstance().onNaviInitRec("1");
            }
        });
    }

    private void sugSearch() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(sugListener);
        /**
         * 在您的项目中，keyword为随您的输入变化的值
         */
        mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                .city("北京")
                .keyword("肯"));
    }

    private void svgLicense() {
        //        // svg瓦片license校验
        SVGLicenseOption svgLicenseOption = new SVGLicenseOption();
        svgLicenseOption.setAkCipher("3Zags80N8pqu0kxMTDokwZhmAhMKKiaD");
        svgLicenseOption.setDeviceIDCipher("device_id");
        svgLicenseOption.setTime("1686240000");
        svgLicenseOption.setSign("aaa");
        svgLicenseOption.setMode("huawei");
        svgLicenseOption.setOSVersion("1.0.0");
        svgLicenseOption.setAppVersion("1.0.0");
        svgLicenseOption.setCuid("cuid");

        SVGLicenseManager svgLicenseManager = SVGLicenseManager.getInstance();
        svgLicenseManager.setSVGLicenseListener(new ISVGLicenseListener() {
            @Override
            public void auth(int type, int code) {
                sb.append("百度手表矢量地图鉴权返回code："+code);
                sb.append("\n");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", sb.toString());
                BaseBdWatchJsonData jsonData = new BaseBdWatchJsonData();
                jsonData.setError_code(code);
                FissionSdkBleManage.getInstance().onAuthLicenseRec(GsonUtils.toJson(jsonData));
            }
        });
        svgLicenseManager.loadSVGLicense(this, svgLicenseOption);
    }

    private void sendSvg() {
        curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SVG;
        // svg瓦片下载
        SVGTileSearchOption svgTileSearchOption = new SVGTileSearchOption();
        svgTileSearchOption.akCipher("3Zags80N8pqu0kxMTDokwZhmAhMKKiaD");
        svgTileSearchOption.deviceIDCipher("device_id");
        svgTileSearchOption.coordCipher("x,y,z");
        svgTileSearchOption.sign("aa");
        svgTileSearchOption.time("1686240000");
        svgTileSearchOption.x(215893);
        svgTileSearchOption.y(99299);
        svgTileSearchOption.z(18);

        SVGTileSearch svgTileSearch = SVGTileSearch.newInstance();
        svgTileSearch.setOnGetSVGTileSearchResultListener(new OnGetSVGTileSearchResultListener() {
            @Override
            public void onGetSVGTileResult(SVGTileResult result) {
                bdWatchMapSvgRecInfo = new BdWatchMapSvgRecInfo();
                errorCode = ParseErrorCodeUtils.getInstance().getErrorCode(result.error);
                String svgPath = result.getTilePath();
                bdWatchMapSvgRecInfo = new BdWatchMapSvgRecInfo();
                bdWatchMapSvgRecInfo.setCoord(svgTileSearchOption.mCoordCipher);
                bdWatchMapSvgRecInfo.setTime(svgTileSearchOption.mTime);
                LogUtils.d("wl", "百度地图SDK下载svg图片地址："+svgPath);
                fileName = svgTileSearchOption.mTime+".bin";
                BdMapFileManage.getInstance().sendSvgFile(svgPath, fileName, curCmdId);
//                svgPath = "/user/test/"+svgTileSearchOption.mTime+".bin";
//                bdWatchMapSvgRecInfo.setSvg(svgPath);
//                BaseBdWatchJsonData<BdWatchMapSvgRecInfo> jsonData = new BaseBdWatchJsonData<>();
//                jsonData.setError_code(errorCode);
//                jsonData.setResult(bdWatchMapSvgRecInfo);
//                FissionSdkBleManage.getInstance().sendSvg(GsonUtils.toJson(jsonData)+"\0");
//                HiSiliconSppCmdHelper.sendSvg(GsonUtils.toJson(jsonData)+"\0");
            }
        });
        svgTileSearch.requestSVGTile(svgTileSearchOption);
    }

    private void sendDstList() {
        mPoiSearch = PoiSearch.newInstance();

        mPoiSearch.setOnGetPoiSearchResultListener(onGetPoiSearchResultListener);

        mPoiSearch.searchInCity(new PoiCitySearchOption()
                .city("北京") //必填
                .keyword("美食") //必填
                .pageNum(0));
    }

    private void pushNavigation() {
    }

    private void searchNavigationStep() {
        if(mRoutePlanSearch == null){
            mRoutePlanSearch = RoutePlanSearch.newInstance();
            mRoutePlanSearch.setOnGetRoutePlanResultListener(listener);
        }

        PlanNode stNode = PlanNode.withCityNameAndPlaceName("北京", "西二旗地铁站");
        PlanNode enNode = PlanNode.withCityNameAndPlaceName("北京", "百度大厦");
        mRoutePlanSearch.walkingSearch((new WalkingRoutePlanOption())
                .from(stNode)
                .to(enNode));
    }

    private void searchNavigationRiding() {
        if(mRoutePlanSearch == null){
            mRoutePlanSearch = RoutePlanSearch.newInstance();
            mRoutePlanSearch.setOnGetRoutePlanResultListener(listener);
        }

        PlanNode stNode = PlanNode.withCityNameAndPlaceName("北京", "西二旗地铁站");
        PlanNode enNode = PlanNode.withCityNameAndPlaceName("北京", "百度大厦");

        mRoutePlanSearch.bikingSearch((new BikingRoutePlanOption())
                .from(stNode)
                .to(enNode)
                // ridingType  0 普通骑行，1 电动车骑行
                // 默认普通骑行
                .ridingType(0));
    }

    private void startNavigationBus() {
        if(mRoutePlanSearch == null){
            mRoutePlanSearch = RoutePlanSearch.newInstance();
            mRoutePlanSearch.setOnGetRoutePlanResultListener(listener);
        }

        PlanNode stNode = PlanNode.withCityNameAndPlaceName("北京", "西二旗地铁站");
        PlanNode enNode = PlanNode.withCityNameAndPlaceName("北京", "百度大厦");
        mRoutePlanSearch.transitSearch((new TransitRoutePlanOption())
                .from(stNode)
                .to(enNode)
                .city("北京"));
    }

    private void sendPoi(View v) {
    }

    private void startNavigationRiding() {
        startBikingNavi();
    }

    private void startNavigationStep() {
        startWalkingNavi();
    }

    private void endNavigation() {
    }

    private void startLocation() {
        BdWatchMapGeolocation geolocation = new BdWatchMapGeolocation();
        geolocation.setCoord_type(0);
        geolocation.setTime(String.valueOf(System.currentTimeMillis()));
        geolocation.setDirection(0);
        geolocation.setLatitude(22.012333);
        geolocation.setLongitude(152.09876);
        geolocation.setSpeed(10f);
        BaseBdWatchJsonData jsonData = new BaseBdWatchJsonData();
        jsonData.setError_code(0);
        jsonData.setResult(geolocation);
        FissionSdkBleManage.getInstance().onGeolocationRec(GsonUtils.toJson(jsonData));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPoiSearch!=null){
            mPoiSearch.destroy();
        }
        if(mRoutePlanSearch!=null){
            mRoutePlanSearch.destroy();
        }
        if(mSuggestionSearch!=null){
            mSuggestionSearch.destroy();
        }

        if(mCoder!=null){
            mCoder.destroy();
        }

        if(mLocationClient!=null){
            mLocationClient.stop();
        }

        FissionSdkBleManage.getInstance().removeCmdResultListener(hiSiliconDataResultListener);

        hiSiliconDataResultListener = null;

        hiSiWatchReqTaskListener = null;

        bdMapFileTransportListener = null;

        BdMapFileManage.getInstance().onDestroy();

        isFirstBatch = false;

        isSendSvgNotice = false;
    }

    // 天安门坐标
    double mLat1 = 39.915291;
    double mLon1 = 116.403857;
    // 百度大厦坐标
    double mLat2 = 40.056858;
    double mLon2 = 116.308194;
    public void startBikingNavi() {
        BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
        recInfo.setMsg_type(BdWatchMsgType.ON_NAVI_START);
        recInfo.setNavi_type(naviType);
        sb.append("\n");
        sb.append("百度手表矢量地图开始骑行导航:"+GsonUtils.toJson(recInfo));
        sb.append("------------------------------------------");
        tv_log.setText(sb.toString());
        LogUtils.d("wl", sb.toString());
        FissionSdkBleManage.getInstance().onNaviStartRec(GsonUtils.toJson(recInfo)+"\0");
        Intent intent = new Intent(BaiduMapManageActivity.this, BNaviGuideActivity.class);
        startActivity(intent);
    }

    public void startWalkingNavi() {
        BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
        recInfo.setMsg_type(BdWatchMsgType.ON_NAVI_START);
        recInfo.setNavi_type(naviType);
        sb.append("\n");
        sb.append("百度手表矢量地图开始步行导航:"+GsonUtils.toJson(recInfo));
        sb.append("------------------------------------------");
        tv_log.setText(sb.toString());
        LogUtils.d("wl", sb.toString());
        FissionSdkBleManage.getInstance().onNaviStartRec(GsonUtils.toJson(recInfo)+"\0");
        Intent intent = new Intent(BaiduMapManageActivity.this, WNaviGuideActivity.class);
        startActivity(intent);
    }

    OnGetRoutePlanResultListener listener = new OnGetRoutePlanResultListener() {

        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
            //创建WalkingRouteOverlay实例
            BdWatchMapWalkResult result = new BdWatchMapWalkResult();
            List<BdWatchMapWalkResult.Route> routes = new ArrayList<>();
            List<WalkingRouteLine> walkingRouteLines = walkingRouteResult.getRouteLines();
            if(walkingRouteLines == null){
                LogUtils.d("wl", "----onGetWalkingRouteResult--错误码-"+walkingRouteResult.error);
                return;
            }
            for(WalkingRouteLine walkingRouteLine: walkingRouteLines){
                BdWatchMapWalkResult.Route route = result.new Route();
                route.distance = walkingRouteLine.getDistance();

                BdWatchMapWalkResult.Duration duration = result.new Duration();
                BMFTime bmfTime = new BMFTime(walkingRouteLine.getDuration());
                duration.dates = bmfTime.dates;
                duration.hours = bmfTime.hours;
                duration.minutes = bmfTime.minutes;
                duration.seconds = bmfTime.seconds;
                route.duration = duration;

                BdWatchMapWalkResult.Point starting = result.new Point();
                starting.uid = walkingRouteLine.getStarting().getUid();
                starting.title = walkingRouteLine.getStarting().getTitle();
                BdWatchMapWalkResult.Location location = result.new Location();
                location.latitude = walkingRouteLine.getStarting().getLocation().latitude;
                location.longitude = walkingRouteLine.getStarting().getLocation().longitude;
                starting.location = location;
                route.starting = starting;

                BdWatchMapWalkResult.Point terminal = result.new Point();
                starting.uid = walkingRouteLine.getTerminal().getUid();
                starting.title = walkingRouteLine.getTerminal().getTitle();
                BdWatchMapWalkResult.Location location2 = result.new Location();
                location2.latitude = walkingRouteLine.getTerminal().getLocation().latitude;
                location2.longitude = walkingRouteLine.getTerminal().getLocation().longitude;
                terminal.location = location2;
                route.terminal = terminal;

                route.title = walkingRouteLine.getTitle();

                List<BdWatchMapWalkResult.Step> steps = new ArrayList<>();
                List<WalkingRouteLine.WalkingStep> walkingSteps = walkingRouteLine.getAllStep();
                for(WalkingRouteLine.WalkingStep walkingStep : walkingSteps){
                    BdWatchMapWalkResult.Step step = result.new Step();
                    step.distance = walkingStep.getDistance();
                    step.duration = walkingStep.getDuration();
                    List<BdWatchMapWalkResult.Location> points = new ArrayList<>();
                    List<LatLng> latLngs = walkingStep.getWayPoints();
                    for(LatLng latLng : latLngs){
                        BdWatchMapWalkResult.Location stepPoint = result.new Location();
                        stepPoint.latitude = latLng.latitude;
                        stepPoint.longitude = latLng.longitude;
                        points.add(stepPoint);
                    }
                    step.points = points;
                    step.name = walkingStep.getName();

                    step.direction = walkingStep.getDirection();

                    BdWatchMapWalkResult.Point entrace = result.new Point();
                    entrace.uid = walkingStep.getEntrance().getUid();
                    entrace.title = walkingStep.getEntrance().getTitle();
                    BdWatchMapWalkResult.Location entracePoint = result.new Location();
                    entracePoint.latitude = walkingStep.getEntrance().getLocation().latitude;
                    entracePoint.longitude = walkingStep.getEntrance().getLocation().longitude;
                    entrace.location = entracePoint;
                    step.entrace = entrace;

                    step.entraceInstruction = walkingStep.getEntranceInstructions();

                    BdWatchMapWalkResult.Point exit = result.new Point();
                    exit.uid = walkingStep.getEntrance().getUid();
                    exit.title = walkingStep.getEntrance().getTitle();
                    BdWatchMapWalkResult.Location exitPoint = result.new Location();
                    exitPoint.latitude = walkingStep.getExit().getLocation().latitude;
                    exitPoint.longitude = walkingStep.getExit().getLocation().longitude;
                    exit.location = exitPoint;
                    step.exit = exit;

                    step.exitInstruction = walkingStep.getExitInstructions();
                    step.instruction = walkingStep.getInstructions();

                    steps.add(step);
                }
                route.steps = steps;
                routes.add(route);
                break;
            }
            result.setRoutes(routes);

            BaseBdWatchJsonData<BdWatchMapWalkResult> jsonData = new BaseBdWatchJsonData<>();
            jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(walkingRouteResult.error));
            jsonData.setResult(result);
            sb.append(GsonUtils.toJson(jsonData));
            sb.append("\n");
            sb.append("------------------------------------------");
            tv_log.setText(sb.toString());
            LogUtils.d("wl", "步行导航转换为手表数据："+ GsonUtils.toJson(jsonData));

            curCmdId = HiSiliconSppCmdID.COMMAND_ID_ROUTE_WALKING_SEARCH_REC;
            SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_HIGH,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_WALKING_SEARCH_REQ), GsonUtils.toJson(jsonData)));
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
            BdWatchMapBusResult result = new BdWatchMapBusResult();
            List<BdWatchMapBusResult.Route> routes = new ArrayList<>();
            List<TransitRouteLine> transitRouteLines = transitRouteResult.getRouteLines();
            if(transitRouteLines == null){
                LogUtils.d("wl", "----onGetTransitRouteResult--错误码-"+transitRouteResult.error);
                return;
            }
            for(TransitRouteLine transitRouteLine: transitRouteLines){
                BdWatchMapBusResult.Route route = result.new Route();
                route.distance = transitRouteLine.getDistance();

                BdWatchMapBusResult.Duration duration = result.new Duration();
                BMFTime bmfTime = new BMFTime(transitRouteLine.getDuration());
                duration.dates = bmfTime.dates;
                duration.hours = bmfTime.hours;
                duration.minutes = bmfTime.minutes;
                duration.seconds = bmfTime.seconds;
                route.duration = duration;

                BdWatchMapBusResult.Point starting = result.new Point();
                starting.uid = transitRouteLine.getStarting().getUid();
                starting.title = transitRouteLine.getStarting().getTitle();
                BdWatchMapBusResult.Location location = result.new Location();
                location.latitude = transitRouteLine.getStarting().getLocation().latitude;
                location.longitude = transitRouteLine.getStarting().getLocation().longitude;
                starting.location = location;
                route.starting = starting;

                BdWatchMapBusResult.Point terminal = result.new Point();
                starting.uid = transitRouteLine.getTerminal().getUid();
                starting.title = transitRouteLine.getTerminal().getTitle();
                BdWatchMapBusResult.Location location2 = result.new Location();
                location2.latitude = transitRouteLine.getTerminal().getLocation().latitude;
                location2.longitude = transitRouteLine.getTerminal().getLocation().longitude;
                terminal.location = location2;
                route.terminal = terminal;

                route.title = transitRouteLine.getTitle();

                List<BdWatchMapBusResult.Step> steps = new ArrayList<>();
                List<TransitRouteLine.TransitStep> transitSteps = transitRouteLine.getAllStep();
                for(TransitRouteLine.TransitStep transitStep : transitSteps){
                    BdWatchMapBusResult.Step step = result.new Step();
                    step.distance = transitStep.getDistance();
                    step.duration = transitStep.getDuration();
                    List<BdWatchMapBusResult.Location> points = new ArrayList<>();
                    List<LatLng> latLngs = transitStep.getWayPoints();
                    for(LatLng latLng : latLngs){
                        BdWatchMapBusResult.Location stepPoint = result.new Location();
                        stepPoint.latitude = latLng.latitude;
                        stepPoint.longitude = latLng.longitude;
                        points.add(stepPoint);
                    }
                    step.points = points;
                    step.name = transitStep.getName();

                    BdWatchMapBusResult.Point entrace = result.new Point();
                    entrace.uid = transitStep.getEntrance().getUid();
                    entrace.title = transitStep.getEntrance().getTitle();
                    BdWatchMapBusResult.Location entracePoint = result.new Location();
                    entracePoint.latitude = transitStep.getEntrance().getLocation().latitude;
                    entracePoint.longitude = transitStep.getEntrance().getLocation().longitude;
                    entrace.location = entracePoint;
                    step.entrace = entrace;

                    BdWatchMapBusResult.Point exit = result.new Point();
                    exit.uid = transitStep.getEntrance().getUid();
                    exit.title = transitStep.getEntrance().getTitle();
                    BdWatchMapBusResult.Location exitPoint = result.new Location();
                    exitPoint.latitude = transitStep.getExit().getLocation().latitude;
                    exitPoint.longitude = transitStep.getExit().getLocation().longitude;
                    exit.location = exitPoint;
                    step.exit = exit;

                    step.instruction = transitStep.getInstructions();
                    step.stepType = transitStep.getStepType().ordinal();

                    VehicleInfo vehicleInfo = transitStep.getVehicleInfo();
                    if(vehicleInfo !=null){
                        BdWatchMapBusResult.VehicleInfo vehicleInfoWatch = result.new VehicleInfo();
                        vehicleInfoWatch.uid = vehicleInfo.getUid();
                        vehicleInfoWatch.title = vehicleInfo.getTitle();
                        vehicleInfoWatch.passStationNum = vehicleInfo.getPassStationNum();
                        vehicleInfoWatch.totalPrice = vehicleInfo.getTotalPrice();
                        vehicleInfoWatch.zonePrice = vehicleInfo.getZonePrice();
                        step.vehicleInfo = vehicleInfoWatch;
                    }

                    steps.add(step);
                }
                route.steps = steps;
                routes.add(route);
                break;
            }
            result.setRoutes(routes);

            BaseBdWatchJsonData<BdWatchMapBusResult> jsonData = new BaseBdWatchJsonData<>();
            jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(transitRouteResult.error));
            jsonData.setResult(result);
            sb.append(GsonUtils.toJson(jsonData));
            sb.append("\n");
            sb.append("------------------------------------------");
            tv_log.setText(sb.toString());
            LogUtils.d("wl", "公交导航转换为手表数据："+ GsonUtils.toJson(jsonData));

            curCmdId = HiSiliconSppCmdID.COMMAND_ID_ROUTE_TRANSIT_SEARCH_REC;
            SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_HIGH,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_TRANSIT_SEARCH_REQ), GsonUtils.toJson(jsonData)));
        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
            BdWatchMapBiKingResult result = new BdWatchMapBiKingResult();
            List<BdWatchMapBiKingResult.Route> routes = new ArrayList<>();
            List<BikingRouteLine> bikingRouteLines = bikingRouteResult.getRouteLines();
            if(bikingRouteLines == null){
                LogUtils.d("wl", "----onGetBikingRouteResult--错误码-"+bikingRouteResult.error);
                return;
            }
            for(BikingRouteLine bikingRouteLine: bikingRouteLines){
                BdWatchMapBiKingResult.Route route = result.new Route();
                route.distance = bikingRouteLine.getDistance();

                BdWatchMapBiKingResult.Duration duration = result.new Duration();
                BMFTime bmfTime = new BMFTime(bikingRouteLine.getDuration());
                duration.dates = bmfTime.dates;
                duration.hours = bmfTime.hours;
                duration.minutes = bmfTime.minutes;
                duration.seconds = bmfTime.seconds;
                route.duration = duration;

                BdWatchMapBiKingResult.Point starting = result.new Point();
                starting.uid = bikingRouteLine.getStarting().getUid();
                starting.title = bikingRouteLine.getStarting().getTitle();
                BdWatchMapBiKingResult.Location location = result.new Location();
                location.latitude = bikingRouteLine.getStarting().getLocation().latitude;
                location.longitude = bikingRouteLine.getStarting().getLocation().longitude;
                starting.location = location;
                route.starting = starting;

                BdWatchMapBiKingResult.Point terminal = result.new Point();
                starting.uid = bikingRouteLine.getTerminal().getUid();
                starting.title = bikingRouteLine.getTerminal().getTitle();
                BdWatchMapBiKingResult.Location location2 = result.new Location();
                location2.latitude = bikingRouteLine.getTerminal().getLocation().latitude;
                location2.longitude = bikingRouteLine.getTerminal().getLocation().longitude;
                terminal.location = location2;
                route.terminal = terminal;

                route.title = bikingRouteLine.getTitle();

                List<BdWatchMapBiKingResult.Step> steps = new ArrayList<>();
                List<BikingRouteLine.BikingStep> bikingSteps = bikingRouteLine.getAllStep();
                for(BikingRouteLine.BikingStep bikingStep : bikingSteps){
                    BdWatchMapBiKingResult.Step step = result.new Step();
                    step.distance = bikingStep.getDistance();
                    step.duration = bikingStep.getDuration();
                    List<BdWatchMapBiKingResult.Location> points = new ArrayList<>();
                    List<LatLng> latLngs = bikingStep.getWayPoints();
                    for(LatLng latLng : latLngs){
                        BdWatchMapBiKingResult.Location stepPoint = result.new Location();
                        stepPoint.latitude = latLng.latitude;
                        stepPoint.longitude = latLng.longitude;
                        points.add(stepPoint);
                    }
                    step.points = points;
                    step.name = bikingStep.getName();

                    step.direction = bikingStep.getDirection();

                    BdWatchMapBiKingResult.Point entrace = result.new Point();
                    entrace.uid = bikingStep.getEntrance().getUid();
                    entrace.title = bikingStep.getEntrance().getTitle();
                    BdWatchMapBiKingResult.Location entracePoint = result.new Location();
                    entracePoint.latitude = bikingStep.getEntrance().getLocation().latitude;
                    entracePoint.longitude = bikingStep.getEntrance().getLocation().longitude;
                    entrace.location = entracePoint;
                    step.entrace = entrace;

                    step.entraceInstruction = bikingStep.getEntranceInstructions();

                    BdWatchMapBiKingResult.Point exit = result.new Point();
                    exit.uid = bikingStep.getEntrance().getUid();
                    exit.title = bikingStep.getEntrance().getTitle();
                    BdWatchMapBiKingResult.Location exitPoint = result.new Location();
                    exitPoint.latitude = bikingStep.getExit().getLocation().latitude;
                    exitPoint.longitude = bikingStep.getExit().getLocation().longitude;
                    exit.location = exitPoint;
                    step.exit = exit;

                    step.exitInstruction = bikingStep.getExitInstructions();
                    step.instruction = bikingStep.getInstructions();
                    step.turnType = bikingStep.getTurnType();
                    step.restrictionsStatus = bikingStep.getRestrictionsStatus();
                    step.restrictionsInfo = bikingStep.getRestrictionsInfo();

                    steps.add(step);
                }
                route.steps = steps;
                routes.add(route);
                break;
            }
            result.setRoutes(routes);

            BaseBdWatchJsonData<BdWatchMapBiKingResult> jsonData = new BaseBdWatchJsonData<>();
            jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(bikingRouteResult.error));
            jsonData.setResult(result);
            sb.append(GsonUtils.toJson(jsonData));
            sb.append("\n");
            sb.append("------------------------------------------");
            tv_log.setText(sb.toString());
            LogUtils.d("wl", "骑行导航转换为手表数据："+ GsonUtils.toJson(jsonData));

            curCmdId = HiSiliconSppCmdID.COMMAND_ID_ROUTE_RIDING_SEARCH_REC;
            SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_HIGH,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_RIDING_SEARCH_REQ), GsonUtils.toJson(jsonData)));
        }

        @Override
        public void onGetIntegralRouteResult(IntegralRouteResult integralRouteResult) {

        }
    };


//    private void sendFile(String json, String name) throws JSONException {
//        startTime = System.currentTimeMillis();
//
//        resultData =  ConvertUtils.jsonObject2Bytes(new JSONObject(json));
//        fileSize = resultData.length;
//        fileName = name;
//        fileDataHexList = new ArrayList<>();
//        if(fileSize % (dataBlockNum * dataBlockSize)!=0){
//            framesCount = fileSize / (dataBlockNum * dataBlockSize) +1;
//        }else{
//            framesCount = fileSize/ (dataBlockNum * dataBlockSize);
//        }
//        String dataHex = ConvertUtils.bytes2HexString(resultData);
//        if(fileSize % dataBlockSize!=0){
//            dataBlockNumCount = fileSize / dataBlockSize + 1;
//            for(int i =0; i< dataBlockNumCount; i++){
//                String data;
//                if(i == dataBlockNumCount-1){
//                    data = dataHex.substring(i*dataBlockSize*2);
//                }else{
//                    data = dataHex.substring(i*dataBlockSize*2, (i+1)*dataBlockSize*2);
//                }
//                fileDataHexList.add(data);
//            }
//        }else{
//            dataBlockNumCount = fileSize / dataBlockSize;
//            for(int i =0; i< dataBlockNumCount; i++){
//                String data = dataHex.substring(i*dataBlockSize*2, (i+1)*dataBlockSize*2);
//                fileDataHexList.add(data);
//            }
//        }
//
//        TransferParameterRequest request = new TransferParameterRequest();
//        request.setTransmitId(2);
//        request.setTransferParameterInfo(TransferParameterRequest.UPLOAD_FILE);
//        request.setTotalSize(fileSize);
//        request.setDataBlockNum(dataBlockNum);
//        request.setDataBlockSize(dataBlockSize);
//        request.setFileName("/user/test/"+fileName+"\0");
//        FissionSdkBleManage.getInstance().parameterNegotiation(request);
//        LogUtils.d("wl", "当前上传文件信息："+"， 数据总帧数："+framesCount+"，数据总包数："+dataBlockNumCount);
//    }
//
//    private void sendSvgFile(String filePath, String name){
//        clear();
//        startTime = System.currentTimeMillis();
//        resultData =  FileIOUtils.readFile2BytesByStream(filePath);
//        fileSize = resultData.length;
//        fileName = name;
//        fileDataHexList = new ArrayList<>();
//        if(fileSize % (dataBlockNum * dataBlockSize)!=0){
//            framesCount = fileSize / (dataBlockNum * dataBlockSize) +1;
//        }else{
//            framesCount = fileSize/ (dataBlockNum * dataBlockSize);
//        }
//        String dataHex = ConvertUtils.bytes2HexString(resultData);
//        if(fileSize % dataBlockSize!=0){
//            dataBlockNumCount = fileSize / dataBlockSize + 1;
//            for(int i =0; i< dataBlockNumCount; i++){
//                String data;
//                if(i == dataBlockNumCount-1){
//                    data = dataHex.substring(i*dataBlockSize*2);
//                }else{
//                    data = dataHex.substring(i*dataBlockSize*2, (i+1)*dataBlockSize*2);
//                }
//                fileDataHexList.add(data);
//            }
//        }else{
//            dataBlockNumCount = fileSize / dataBlockSize;
//            for(int i =0; i< dataBlockNumCount; i++){
//                String data = dataHex.substring(i*dataBlockSize*2, (i+1)*dataBlockSize*2);
//                fileDataHexList.add(data);
//            }
//        }
//
//        TransferParameterRequest request = new TransferParameterRequest();
//        request.setTransmitId(2);
//        request.setTransferParameterInfo(TransferParameterRequest.UPLOAD_FILE);
//        request.setTotalSize(fileSize);
//        request.setDataBlockNum(dataBlockNum);
//        request.setDataBlockSize(dataBlockSize);
//        request.setFileName("/user/test/"+fileName+"\0");
//        FissionSdkBleManage.getInstance().parameterNegotiation(request);
//        LogUtils.d("wl", "当前上传文件信息："+"， 数据总帧数："+framesCount+"，数据总包数："+dataBlockNumCount);
//    }

    private void initLocation(){
        PermissionUtils.permission(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION).callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                Toast.makeText(BaiduMapManageActivity.this,"没有权限,请检查权限",Toast.LENGTH_SHORT).show();
            }
        }).request();

        // 初始化定位客户端
        try {
            mLocationClient = new LocationClient(getApplicationContext());

            // 注册定位监听器
            mLocationClient.registerLocationListener(myListener);

            LocationClientOption option = new LocationClientOption();

            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
//可选，设置定位模式，默认高精度
//LocationMode.Hight_Accuracy：高精度；
//LocationMode. Battery_Saving：低功耗；
//LocationMode. ：仅使用设备；
//LocationMode.Fuzzy_Locating, 模糊定位模式；v9.2.8版本开始支持，可以降低API的调用频率，但同时也会降低定位精度；

            option.setCoorType("bd09ll");
//可选，设置返回经纬度坐标类型，默认gcj02
//gcj02：国测局坐标；
//bd09ll：百度经纬度坐标；
//bd09：百度墨卡托坐标；
//海外地区定位，无需设置坐标类型，统一返回wgs84类型坐标

            option.setFirstLocType(LocationClientOption.FirstLocType.ACCURACY_IN_FIRST_LOC);
//可选，首次定位时可以选择定位的返回是准确性优先还是速度优先，默认为速度优先
//可以搭配setOnceLocation(Boolean isOnceLocation)单次定位接口使用，当设置为单次定位时，setFirstLocType接口中设置的类型即为单次定位使用的类型
//FirstLocType.SPEED_IN_FIRST_LOC:速度优先，首次定位时会降低定位准确性，提升定位速度；
//FirstLocType.ACCUARACY_IN_FIRST_LOC:准确性优先，首次定位时会降低速度，提升定位准确性；

            option.setScanSpan(10000);
//可选，设置发起定位请求的间隔，int类型，单位ms
//如果设置为0，则代表单次定位，即仅定位一次，默认为0
//如果设置非0，需设置1000ms以上才有效

            option.setOpenGnss(true);
//可选，设置是否使用卫星定位，默认false
//使用高精度和仅用设备两种定位模式的，参数必须设置为true

            option.setLocationNotify(false);
//可选，设置是否当卫星定位有效时按照1S/1次频率输出卫星定位结果，默认false

            option.setIgnoreKillProcess(true);
//可选，定位SDK内部是一个service，并放到了独立进程。
//设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)

            option.SetIgnoreCacheException(false);
//可选，设置是否收集Crash信息，默认收集，即参数为false

            option.setWifiCacheTimeOut(5*60*1000);
//可选，V7.2版本新增能力
//如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位

            option.setEnableSimulateGnss(false);
//可选，设置是否需要过滤卫星定位仿真结果，默认需要，即参数为false

            option.setNeedNewVersionRgc(true);
//可选，设置是否需要最新版本的地址信息。默认需要，即参数为true

            mLocationClient.setLocOption(option);
//mLocationClient为第二步初始化过的LocationClient对象
//需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
//更多LocationClientOption的配置，请参照类参考中LocationClientOption类的详细说明

            // 开始定位
//            mLocationClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init(){
        btn_open_map = findViewById(R.id.btn_open_map);
        btn_send_svg_list = findViewById(R.id.btn_send_svg_list);
        btn_route_plan_start = findViewById(R.id.btn_route_plan_start);
        btn_start_navigation_riding_init = findViewById(R.id.btn_start_navigation_riding_init);
        btn_start_navigation_step_init = findViewById(R.id.btn_start_navigation_step_init);
        btn_sug_search = findViewById(R.id.btn_sug_search);
        btn_license = findViewById(R.id.btn_license);
        btn_send_svg = findViewById(R.id.btn_send_svg);
        btn_send_dst_list = findViewById(R.id.btn_send_dst_list);
        btn_push_navigation = findViewById(R.id.btn_push_navigation);

        btn_search_navigation_riding = findViewById(R.id.btn_search_navigation_riding);
        btn_search_navigation_step = findViewById(R.id.btn_search_navigation_step);
        btn_start_navigation_bus = findViewById(R.id.btn_start_navigation_bus);
        btn_start_navigation_riding = findViewById(R.id.btn_start_navigation_riding);
        btn_start_navigation_step = findViewById(R.id.btn_start_navigation_step);
        btn_end_navigation = findViewById(R.id.btn_end_navigation);
        btn_location_start = findViewById(R.id.btn_location_start);

        btn_open_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        btn_send_svg_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendSvg();
            }
        });

        btn_route_plan_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                routePlanStart();
            }
        });

        btn_start_navigation_riding_init.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ridingNaviInit();
            }
        });

        btn_start_navigation_step_init.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                walkingNaviInit();
            }
        });

        btn_sug_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sugSearch();
            }
        });

        btn_license.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                svgLicense();
            }
        });

        btn_send_dst_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendDstList();
            }
        });

        btn_push_navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pushNavigation();
            }
        });

        btn_send_svg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendSvg();
            }
        });

        btn_search_navigation_riding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                searchNavigationRiding();
            }
        });

        btn_search_navigation_step.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                searchNavigationStep();
            }
        });

        btn_start_navigation_bus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startNavigationBus();
            }
        });

        btn_start_navigation_riding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startNavigationRiding();
            }
        });

        btn_start_navigation_step.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startNavigationStep();
            }
        });

        btn_end_navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                endNavigation();
            }
        });

        btn_location_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startLocation();
            }
        });

        initLocation();

        initVoiceSearch();

        BdMapFileManage.getInstance().init();

        if(bdMapFileTransportListener == null){
            bdMapFileTransportListener = new BdMapFileManage.BdMapFileTransportListener() {
                @Override
                public void sendSvgFileComplete() {
                    if(bdWatchMapSvgRecInfo !=null){
                        String svgPath = BdMapFileManage.DEFAULT_FILE_PATH+fileName;
                        bdWatchMapSvgRecInfo.setSvg(svgPath);
                        BaseBdWatchJsonData<BdWatchMapSvgRecInfo> jsonData = new BaseBdWatchJsonData<>();
                        jsonData.setError_code(errorCode);
                        jsonData.setResult(bdWatchMapSvgRecInfo);
                        FissionSdkBleManage.getInstance().sendSvg(GsonUtils.toJson(jsonData)+"\0");
                        lastJsonData = GsonUtils.toJson(jsonData)+"\0";

                        tryNum = 0;
                        initSendCmdTimeOutListener();
                        LogUtils.d("svg111", "---sendSvgFileComplete---"+GsonUtils.toJson(jsonData));
                    }
                }

                @Override
                public void sendSvgFileComplete(String jsonData) {
                    if(isSendSvgNotice){
                        FissionSdkBleManage.getInstance().sendSvg(jsonData);
                        lastJsonData = jsonData;
                        tryNum = 0;
                        initSendCmdTimeOutListener();
                        LogUtils.d("svg111", "---sendSvgFileComplete222---"+GsonUtils.toJson(jsonData));
                    }else{
                        SvgTaskManage.getInstance().setWorking(false);
                        SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                        FissionLogUtils.d("--------前加载svg文件发送成功---------"+jsonData);
                    }
                }

                @Override
                public void sendSugFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SUG_REC;
                    String sugPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.SUG_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onSugSearchRec(sugPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendPoiFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_REC_POI;
                    String poiPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.POI_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().recPoi(poiPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendRouteWalkFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_ROUTE_WALKING_SEARCH_REC;
                    String routeWalkPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.ROUTE_WALK_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onRouteWalkingSearchRec(routeWalkPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendRouteRidingFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_ROUTE_RIDING_SEARCH_REC;
                    String routeRidingPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.ROUTE_RIDING_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onRouteRidingSearchRec(routeRidingPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendRouteTransitFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_ROUTE_TRANSIT_SEARCH_REC;
                    String routeTransitPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.ROUTE_TRANSIT_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onRouteTransitSearchRec(routeTransitPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendRemainRouteUpdateFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_REMAIN_ROUTE_REC;
                    String routeTransitPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.REMAIN_ROUTE_UPDATE_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onRemainRouteUpdateRec(routeTransitPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendReRouteCompleteFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_REROUTE_COMPLETE_REC;
                    String routeTransitPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.REROUTE_COMPLETE_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onReRouteCompleteRec(routeTransitPath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }

                @Override
                public void sendRegeocodeFileComplete() {
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_REGEOCODE_SEARCH_REC;
                    String regeocodePath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.REGEOCODE_POI_JSON_DATA_FILENAME;
                    FissionSdkBleManage.getInstance().onRegeocodeSearchRec(regeocodePath+"\0");

                    tryNum = 0;
                    initSendCmdTimeOutListener();
                }
            };
        }
        BdMapFileManage.getInstance().setBdMapFileTransportListener(bdMapFileTransportListener);

        if(svgReqTaskListener == null){
            svgReqTaskListener = new SvgTaskManage.HiSiWatchReqTaskListener() {
                @Override
                public void onExecuteTask(HiSiWatchReqTask task) {
                    SvgTaskManage.getInstance().setWorking(true);
                    if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_SVG))){
                        BdWatchMapSvgReqInfo req = (BdWatchMapSvgReqInfo)task.getData();
                        FissionLogUtils.d("svg111", "---onExecuteTask--"+req.toString());
                        onSvgDataReq(req);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_SUG_SEARCH_REQ))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.SUG_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_POI))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.POI_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_WALKING_SEARCH_REQ))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.ROUTE_WALK_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_RIDING_SEARCH_REQ))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.ROUTE_RIDING_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_TRANSIT_SEARCH_REQ))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.ROUTE_TRANSIT_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_ROUTE_UPDATE_REC))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.REMAIN_ROUTE_UPDATE_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REROUTE_COMPLETE_REC))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.REROUTE_COMPLETE_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_SEND_SVG))){
                        SvgTaskInfo svgTaskInfo = (SvgTaskInfo) task.getData();
                        BdMapFileManage.getInstance().sendSvgFile(svgTaskInfo.getSvgPath(), svgTaskInfo.getFileName(), svgTaskInfo.getCurCmdId(), svgTaskInfo.getSvgJsonDataRec());
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REGEOCODE_SEARCH_REQ))){
                        try {
                            String jsonData = (String) task.getData();
                            BdMapFileManage.getInstance().sendFile(jsonData, BdMapFileManage.REGEOCODE_POI_JSON_DATA_FILENAME, task.getCmdId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFinish() {
                    FissionLogUtils.d("svg111", "--svg文件上传完毕--onFinish--数量："+preloadSvgNum);
                    BdMapFileManage.getInstance().getPreloadSvgList().clear();
                    BdMapFileManage.getInstance().getPreloadSvgList().addAll(BdMapFileManage.getInstance().getUpdateSvgList());
                    BdMapFileManage.getInstance().setCachePreloadSvgList(BdMapFileManage.getInstance().getPreloadSvgList());
                    preloadSvgNum = 0;
                    HiSiTaskManage.getInstance().setWorking(false);
                    HiSiTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("svg111", "svg_task任务完成时间："+(System.currentTimeMillis()-svgTaskStartTime)/1000+"s");
                }
            };

        }
        SvgTaskManage.getInstance().setHiSiWatchReqTaskListener(svgReqTaskListener);

        if(hiSiWatchReqTaskListener == null){
            hiSiWatchReqTaskListener = new HiSiTaskManage.HiSiWatchReqTaskListener() {
                @Override
                public void onExecuteTask(HiSiWatchReqTask task) {
                    if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_SVG))){
                        HiSiTaskManage.getInstance().setWorking(true);
                        BdWatchMapSvgReqInfo req = (BdWatchMapSvgReqInfo)task.getData();
//                        onSvgDataReq(req);
                        svgTaskStartTime = System.currentTimeMillis();
//                        onPreloadingSvgDataReq(req);
                        batchDownloadSvgFile(req);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROAD_GUIDE_ICON_UPDATE_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onRouteGuideIconUpdateRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROAD_GUIDE_TEXT_UPDATE_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onRoadGuideTextUpdateRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_DISTANCE_UPDATE_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onRemainDistanceUpdateRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_TIME_UPDATE_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onRemainTimeUpdateRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_FAR_AWAYE_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onRouteFarAwayRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ROUTE_PLAN_YAWING_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onRoutePlanYawingRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_ARRIVE_DEST_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onArriveDestRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_GPS_STATUS_CHANGE_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onGpsStatusChangeRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_GET_GEOLOCATION_CALLBACK))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onGeolocationRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_GEOCODE_SEARCH_REC))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onGeocodeSearchRec(jsonData);
                    }else if(task.getCmdId().equals(HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_FIRST_BATCH))){
                        String jsonData = (String) task.getData();
                        FissionSdkBleManage.getInstance().onFirstBatchNotify(jsonData);
                    }
                }

                @Override
                public void onTimeOut() {
                    BdMapFileManage.getInstance().getPreloadSvgList().clear();
                    BdMapFileManage.getInstance().getPreloadSvgList().addAll(BdMapFileManage.getInstance().getUpdateSvgList());
                    BdMapFileManage.getInstance().setCachePreloadSvgList(BdMapFileManage.getInstance().getPreloadSvgList());

                    preloadSvgNum = 0;
                }
            };
        }

        HiSiTaskManage.getInstance().setHiSiWatchReqTaskListener(hiSiWatchReqTaskListener);

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
                public void reqPoiInfo(BdWatchMapPoiReq req) {
                    super.reqPoiInfo(req);
                    sendPoiRec(req);
                }

                @Override
                public void onAuthLicenseReq(BdWatchMapAuthLicense req) {
                    super.onAuthLicenseReq(req);
                    onAuthLicense(req);
                }

                @Override
                public void onSugSearchReq(BdWatchMapSugSearchReqInfo req) {
                    super.onSugSearchReq(req);
                    onSugSearch(req);
                }

                @Override
                public void onSvgDownloadReq(BdWatchMapSvgReqInfo req) {
                    super.onSvgDownloadReq(req);
                    BdWatchMapSvgReqInfo bdWatchMapSvgReqInfo = new BdWatchMapSvgReqInfo();
                    bdWatchMapSvgReqInfo.setCoord(req.getCoord());
                    bdWatchMapSvgReqInfo.setAk(req.getAk());
                    bdWatchMapSvgReqInfo.setTime(req.getTime());
                    bdWatchMapSvgReqInfo.setSign(req.getSign());
                    bdWatchMapSvgReqInfo.setDevice_id(req.getDevice_id());
                    bdWatchMapSvgReqInfo.setRequest(req.getRequest());
                    FissionLogUtils.d("svg222", "手表请求svg信息"+bdWatchMapSvgReqInfo);
                    if(req.getRequest() == 0){
                        isSendSvgNotice = true;
                        HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_SVG), bdWatchMapSvgReqInfo);
                        HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
//                        onBatchPreloadingSvgDataReq(req);
                    }else if(req.getRequest() == 2){
                        isSendSvgNotice = false;
                        HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_SVG), bdWatchMapSvgReqInfo);
                        HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                    }else{
                        SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_HIGH,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_SVG), bdWatchMapSvgReqInfo));
                        FissionLogUtils.d("svg222", "手表请求svg信息222"+bdWatchMapSvgReqInfo);
                    }
//                onSvgDataReq(req);
                }

                @Override
                public void onSvgRecSuccess() {
                    super.onSvgRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("svg111", "---onSvgRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_SEND_SVG.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void onSugRecSuccess() {
                    super.onSugRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("svg111", "---onSugRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_SUG_SEARCH_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void onPoiRecSuccess() {
                    super.onPoiRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("svg111", "---onPoiRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_REC_POI.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void geolocation(BdWatchGeolocationReq req) {
                    super.geolocation(req);
                    if(mLocationClient!=null){
                        if(req.isOn_location()){
                            mLocationClient.start();
                            FissionLogUtils.d("wl", "手表请求开始定位");
                            isFirstBatch = true;
                            FissionLogUtils.d("--------前加载开始---------");

                        }else{
                            mLocationClient.stop();
                            SvgTaskManage.getInstance().clearTask();
                            HiSiTaskManage.getInstance().clearTask();
                            FissionLogUtils.d("wl", "手表请求结束定位");
                            if(HiSiliconFileTransferUtils.getInstance().isTransmitting()){
                                FissionSdkBleManage.getInstance().transferNotify(new TransferNotify(HiSiliconFileTransferUtils.getInstance().getTransmitId(), HiSiliconSppCmdID.COMMAND_ID_NOTIFY_FINISH, 0, ""));
                                FissionLogUtils.d("wl", "退出Js百度地图， 通知停止svg文件传输");
                            }
                            finish();
                        }
                    }
                }

                @Override
                public void onGeocodeSearchReq(BdWatchGeocodeSearchReq req) {
                    super.onGeocodeSearchReq(req);
                    geocodeSearch(req);
                }

                @Override
                public void onRegeocodeSearchReq(BdWatchReGeocodeSearchReq req) {
                    super.onRegeocodeSearchReq(req);
                    regeocodeSearch(req);
                }

                @Override
                public void onRegeocodeRecSuccess() {
                    super.onRegeocodeRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("wl", "---onRegeocodeRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_REGEOCODE_SEARCH_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void onRouteWalkingSearchReq(BdWatchRouteSearch routeSearchReq) {
                    super.onRouteWalkingSearchReq(routeSearchReq);
                    routeWalkingSearch(routeSearchReq);
                }

                @Override
                public void onRouteWalkingSearchRecSuccess() {
                    super.onRouteWalkingSearchRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("wl", "---onRouteWalkingSearchRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_ROUTE_WALKING_SEARCH_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void onRouteRidingSearchReq(BdWatchRouteRidingSearch routeSearchReq) {
                    super.onRouteRidingSearchReq(routeSearchReq);
                    routeRidingSearch(routeSearchReq);
                }

                @Override
                public void onRouteRidingSearchRecSuccess() {
                    super.onRouteRidingSearchRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("wl", "---onRouteRidingSearchRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_ROUTE_RIDING_SEARCH_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void onRouteTransitSearchReq(BdWatchRouteTransitSearch routeSearchReq) {
                    super.onRouteTransitSearchReq(routeSearchReq);
                    routeTransitSearch(routeSearchReq);
                }

                @Override
                public void onRouteTransitSearchRecSuccess() {
                    super.onRouteTransitSearchRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("wl", "---onRouteTransitSearchRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_ROUTE_TRANSIT_SEARCH_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void startNaviByWatch(BdWatchMapNaviInitReqInfo reqInfo) {
                    super.startNaviByWatch(reqInfo);
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initNaviEngine(reqInfo);
                        }
                    });
                }

                @Override
                public void destroyNaviByWatch() {
                    super.destroyNaviByWatch();
                    onNaviDestroyRec();
                }

                @Override
                public void onRemainRouteUpdateRecSuccess() {
                    super.onRemainRouteUpdateRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("wl", "---onRemainRouteUpdateRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_REMAIN_ROUTE_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }

                @Override
                public void onReRouteCompleteRecSuccess() {
                    super.onReRouteCompleteRecSuccess();
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.d("wl", "---onReRouteCompleteRecSuccess---");
                    if(rxTimerUtil != null && HiSiliconSppCmdID.COMMAND_ID_REROUTE_COMPLETE_REC.equals(curCmdId)){
                        rxTimerUtil.cancelTimer();
                        rxTimerUtil = null;
                    }
                }
            };
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(hiSiliconDataResultListener);
        FissionSdkBleManage.getInstance().addCmdResultListener(hiSiliconDataResultListener);
    }

//    private void clear(){
//        curFrames =0;
//        framesCount = 0;
//        resultData = null;
//        fileSize = 0;
//        if(fileDataHexList!=null){
//            fileDataHexList.clear();
//        }
//    }

    public String getPath() {
        File dir = null;
        boolean state = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (state) {
//            if (Build.VERSION.SDK_INT >= 28) {
//                //Android10之后
//                dir = this.getExternalFilesDir(null);
//            } else {
            dir = Environment.getExternalStorageDirectory();
//            }
        } else {
            dir = Environment.getRootDirectory();
        }
        return dir.toString();
    }

    private void onNaviDestroyRec(){
        BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
        recInfo.setMsg_type(BdWatchMsgType.ON_NAVI_DESTROY);
        recInfo.setNavi_type(naviType);
        StringBuffer sb = new StringBuffer();
        sb.append("百度手表矢量地图结束导航:"+ GsonUtils.toJson(recInfo));
        LogUtils.d("wl", sb.toString());
        FissionSdkBleManage.getInstance().onNaviDestroyRec(GsonUtils.toJson(recInfo)+"\0");
    }

    private void geocodeSearch(BdWatchGeocodeSearchReq req){
        mCoder = GeoCoder.newInstance();
        mCoder.setOnGetGeoCodeResultListener(getGeoCoderResultListener);

        //city 和 address是必填项
        mCoder.geocode(new GeoCodeOption()
                .city(req.getCity())
                .address(req.getAddress()));
    }

    private void regeocodeSearch(BdWatchReGeocodeSearchReq req){
        mCoder = GeoCoder.newInstance();
        mCoder.setOnGetGeoCodeResultListener(getGeoCoderResultListener);

        mCoder.reverseGeoCode(new ReverseGeoCodeOption()
                .location(new LatLng(req.getLocation().latitude, req.getLocation().longitude))
                // 设置是否返回新数据 默认值0不返回，1返回
                .newVersion(req.isLatestAdmin() ? 1 : 0)
                // POI召回半径，允许设置区间为0-1000米，超过1000米按1000米召回。默认值为1000
                .radius(req.getRadius()));
    }

    OnGetGeoCoderResultListener getGeoCoderResultListener = new OnGetGeoCoderResultListener() {
        @Override
        public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
            if (null != geoCodeResult && null != geoCodeResult.getLocation()) {
                if (geoCodeResult == null || geoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有检索到结果
                    return;
                } else {
                    double latitude = geoCodeResult.getLocation().latitude;
                    double longitude = geoCodeResult.getLocation().longitude;
                    BdWatchGeocodeSearchRec geocodeSearchRec = new BdWatchGeocodeSearchRec();
                    BdWatchGeocodeSearchRec.Location location = geocodeSearchRec.new Location();
                    location.latitude = latitude;
                    location.longitude = longitude;
                    geocodeSearchRec.setConfidence(geoCodeResult.getConfidence());
                    geocodeSearchRec.setLevel(geoCodeResult.getLevel());
                    geocodeSearchRec.setPrecise(geocodeSearchRec.getPrecise());
                    geocodeSearchRec.setLocation(location);

                    BaseBdWatchJsonData<BdWatchGeocodeSearchRec> jsonData = new BaseBdWatchJsonData<>();
                    jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(geoCodeResult.error));
                    jsonData.setResult(geocodeSearchRec);
                    sb.append(GsonUtils.toJson(jsonData));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", "正地理编码检索转换为手表数据："+ GsonUtils.toJson(jsonData));

                    HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_GEOCODE_SEARCH_REC), GsonUtils.toJson(jsonData)+"\0");
                    HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
                }
            }
        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
            if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                //没有找到检索结果
                return;
            } else {
                //详细地址
                String address = reverseGeoCodeResult.getAddress();
                //行政区号
                int adCode = reverseGeoCodeResult.getAdcode();

                BdWatchReGeocodeSearchRec reGeocodeSearchRec = new BdWatchReGeocodeSearchRec();
                if(reverseGeoCodeResult.getLocation()!=null){
                    BdWatchReGeocodeSearchRec.Location location = reGeocodeSearchRec.new Location();
                    location.latitude = reverseGeoCodeResult.getLocation().latitude;
                    location.longitude = reverseGeoCodeResult.getLocation().longitude;
                    reGeocodeSearchRec.setLocation(location);
                }
                reGeocodeSearchRec.setAddress(address);

                reGeocodeSearchRec.setBusinessCircle(reverseGeoCodeResult.getBusinessCircle());

                if(reverseGeoCodeResult.getAddressDetail()!=null){
                    BdWatchReGeocodeSearchRec.AddressDetail addressDetail = reGeocodeSearchRec.new AddressDetail();
                    addressDetail.country = reverseGeoCodeResult.getAddressDetail().countryName;
                    addressDetail.province = reverseGeoCodeResult.getAddressDetail().province;
                    addressDetail.city = reverseGeoCodeResult.getAddressDetail().city;
                    addressDetail.district = reverseGeoCodeResult.getAddressDetail().district;
                    addressDetail.town = reverseGeoCodeResult.getAddressDetail().town;
                    addressDetail.streetName = reverseGeoCodeResult.getAddressDetail().street;
                    addressDetail.streetNumber = reverseGeoCodeResult.getAddressDetail().streetNumber;
                    addressDetail.adCode = String.valueOf(reverseGeoCodeResult.getAddressDetail().adcode);
                    addressDetail.countryCode = String.valueOf(reverseGeoCodeResult.getAddressDetail().countryCode);
                    addressDetail.direction = reverseGeoCodeResult.getAddressDetail().direction;
                    addressDetail.distance = reverseGeoCodeResult.getAddressDetail().distance;
                    reGeocodeSearchRec.setAddressDetail(addressDetail);
                }

                if(reverseGeoCodeResult.getPoiList()!=null && reverseGeoCodeResult.getPoiList().size()>0){
                    List<BdWatchReGeocodeSearchRec.BMKPoiInfo> list = new ArrayList<>();
                    for(PoiInfo poiInfo: reverseGeoCodeResult.getPoiList()){
                        BdWatchReGeocodeSearchRec.BMKPoiInfo bmkPoiInfo = reGeocodeSearchRec.new BMKPoiInfo();
                        bmkPoiInfo.name = poiInfo.name;
                        if(poiInfo.location!=null){
                            BdWatchReGeocodeSearchRec.Location location = reGeocodeSearchRec.new Location();
                            location.latitude = poiInfo.location.latitude;
                            location.longitude = poiInfo.location.longitude;
                            bmkPoiInfo.pt = location;
                        }
                        bmkPoiInfo.address = poiInfo.address;
                        bmkPoiInfo.phone = poiInfo.phoneNum;
                        bmkPoiInfo.UID = poiInfo.uid;
                        bmkPoiInfo.adcode = String.valueOf(poiInfo.getAdCode());
                        bmkPoiInfo.province = poiInfo.province;
                        bmkPoiInfo.city = poiInfo.city;
                        bmkPoiInfo.area = poiInfo.area;
                        bmkPoiInfo.streetID = poiInfo.street_id;
                        bmkPoiInfo.tag = poiInfo.tag;
                        bmkPoiInfo.hasDetailInfo = poiInfo.hasCaterDetails;
                        if(poiInfo.poiDetailInfo!=null){
                            BdWatchReGeocodeSearchRec.DetailInfo detailInfo = reGeocodeSearchRec.new DetailInfo();
                            detailInfo.distance = poiInfo.poiDetailInfo.distance;
                            detailInfo.type = poiInfo.poiDetailInfo.type;
                            detailInfo.tag = poiInfo.poiDetailInfo.tag;
                            if(poiInfo.poiDetailInfo.naviLocation!=null){
                                BdWatchReGeocodeSearchRec.Location location = reGeocodeSearchRec.new Location();
                                location.latitude = poiInfo.poiDetailInfo.naviLocation.latitude;
                                location.longitude = poiInfo.poiDetailInfo.naviLocation.longitude;
                                detailInfo.naviLocation = location;
                            }
                            bmkPoiInfo.detailInfo = detailInfo;
                        }
                        list.add(bmkPoiInfo);
                    }
                    reGeocodeSearchRec.setPoiList(list);
                }

                if(reverseGeoCodeResult.getPoiRegionsInfoList()!=null && reverseGeoCodeResult.getPoiRegionsInfoList().size()>0){
                    List<BdWatchReGeocodeSearchRec.PoiRegion> list = new ArrayList<>();
                    for(ReverseGeoCodeResult.PoiRegionsInfo poiRegionsInfo: reverseGeoCodeResult.getPoiRegionsInfoList()){
                        BdWatchReGeocodeSearchRec.PoiRegion poiRegion = reGeocodeSearchRec.new PoiRegion();
                        poiRegion.regionDescription = poiRegionsInfo.getDirectionDesc();
                        poiRegion.regionName = poiRegionsInfo.regionName;
                        poiRegion.regionTag = poiRegionsInfo.regionTag;
                        list.add(poiRegion);
                    }
                    reGeocodeSearchRec.setPoiRegions(list);
                }

                reGeocodeSearchRec.setSematicDescription(reverseGeoCodeResult.getSematicDescription());

                BaseBdWatchJsonData<BdWatchReGeocodeSearchRec> jsonData = new BaseBdWatchJsonData<>();
                jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(reverseGeoCodeResult.error));
                jsonData.setResult(reGeocodeSearchRec);
                sb.append(GsonUtils.toJson(jsonData));
                sb.append("\n");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", "逆地理编码检索转换为手表数据："+ GsonUtils.toJson(jsonData));

                curCmdId = HiSiliconSppCmdID.COMMAND_ID_REGEOCODE_SEARCH_REC;
                SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REGEOCODE_SEARCH_REQ), GsonUtils.toJson(jsonData)));
            }
        }
    };


    OnGetPoiSearchResultListener onGetPoiSearchResultListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            LogUtils.d("wl", "---onGetPoiResult----"+poiResult.getAllPoi());
            BdWatchMapPoi bdWatchMapPoi = new BdWatchMapPoi();
            bdWatchMapPoi.setTotalPOINum(poiResult.getTotalPoiNum());
            bdWatchMapPoi.setTotalPageNum(poiResult.getTotalPageNum());
            bdWatchMapPoi.setCurPOINum(poiResult.getCurrentPageCapacity());
            bdWatchMapPoi.setCurPageIndex(poiResult.getCurrentPageNum());
            List<BdWatchMapPoi.PoiInfo> poiInfoList = new ArrayList<>();
            if(poiResult.getAllPoi()!=null && poiResult.getAllPoi().size()>0){
                for(PoiInfo bdPoiInfo: poiResult.getAllPoi()){
                    BdWatchMapPoi.PoiInfo poiInfo = bdWatchMapPoi.new PoiInfo();
                    poiInfo.name = bdPoiInfo.name;
                    BdWatchMapPoi.Point pt = bdWatchMapPoi.new Point();
                    pt.latitude = bdPoiInfo.location.latitude;
                    pt.longitude = bdPoiInfo.location.longitude;
                    poiInfo.pt = pt;
                    poiInfo.address = bdPoiInfo.address;
                    poiInfo.phone = bdPoiInfo.phoneNum;
                    poiInfo.UID = bdPoiInfo.uid;
                    poiInfo.adcode = String.valueOf(bdPoiInfo.getAdCode());
                    poiInfo.province = bdPoiInfo.getProvince();
                    poiInfo.city = bdPoiInfo.getCity();
                    poiInfo.area = bdPoiInfo.getArea();
                    poiInfo.streetID = bdPoiInfo.getStreetId();
                    poiInfo.tag = bdPoiInfo.tag;
                    poiInfo.hasDetailInfo = poiResult.isHasAddrInfo();
                    if(poiInfo.hasDetailInfo){
                        PoiDetailInfo poiDetailInfo = bdPoiInfo.poiDetailInfo;
                        BdWatchMapPoi.DetailsInfo detailsInfo = bdWatchMapPoi.new DetailsInfo();
                        detailsInfo.distance = poiDetailInfo.distance;
                        detailsInfo.type = poiDetailInfo.type;
                        detailsInfo.tag = poiDetailInfo.tag;
                        BdWatchMapPoi.Point naviLocation = bdWatchMapPoi.new Point();
                        pt.latitude = poiDetailInfo.getLocation().latitude;
                        pt.longitude = poiDetailInfo.getLocation().longitude;
                        detailsInfo.naviLocation = naviLocation;
                        poiInfo.detailInfo = detailsInfo;
                    }
                    poiInfoList.add(poiInfo);
                }
                bdWatchMapPoi.setPoiInfoList(poiInfoList);
            }

            BaseBdWatchJsonData<BdWatchMapPoi> jsonData = new BaseBdWatchJsonData<>();
            jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(poiResult.error));
            jsonData.setResult(bdWatchMapPoi);
            sb.append(GsonUtils.toJson(jsonData));
            sb.append("\n");
            sb.append("------------------------------------------");
            tv_log.setText(sb.toString());
            LogUtils.d("wl", "POI检索转换为手表数据："+ GsonUtils.toJson(jsonData));

            curCmdId = HiSiliconSppCmdID.COMMAND_ID_REC_POI;
            SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REQ_POI), GsonUtils.toJson(jsonData)));
        }
        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

        }
        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
        //废弃
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }
    };

    private void sendPoiRec(BdWatchMapPoiReq req){
        mPoiSearch = PoiSearch.newInstance();

        mPoiSearch.setOnGetPoiSearchResultListener(onGetPoiSearchResultListener);

        mPoiSearch.searchNearby(new PoiNearbySearchOption()
                .location(new LatLng(req.getLocation().latitude, req.getLocation().longitude))
                .keyword(req.getKeywords()) //必填
                .radius(req.getRadius())
                .pageNum(req.getPageIndex()));

        FissionLogUtils.d("wl", "---sendPoiRec--");
    }

    private void onAuthLicense(BdWatchMapAuthLicense licenseReq){
        //        // svg瓦片license校验
        SVGLicenseOption svgLicenseOption = new SVGLicenseOption();
        svgLicenseOption.setAkCipher(licenseReq.getAk());
        svgLicenseOption.setDeviceIDCipher(licenseReq.getDevice_id());
        svgLicenseOption.setTime(licenseReq.getTime());
        svgLicenseOption.setSign(licenseReq.getSign());
        svgLicenseOption.setMode(licenseReq.getMode());
        svgLicenseOption.setOSVersion(licenseReq.getOs_version());
        svgLicenseOption.setAppVersion(licenseReq.getApp_version());
        svgLicenseOption.setCuid(licenseReq.getCuid());

        SVGLicenseManager svgLicenseManager = SVGLicenseManager.getInstance();
        svgLicenseManager.setSVGLicenseListener(new ISVGLicenseListener() {
            @Override
            public void auth(int type, int code) {
                sb.append("百度手表矢量地图鉴权返回code："+code);
                sb.append("\n");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", sb.toString());
                BaseBdWatchJsonData jsonData = new BaseBdWatchJsonData();
                jsonData.setError_code(code);
                FissionSdkBleManage.getInstance().onAuthLicenseRec(GsonUtils.toJson(jsonData));
            }
        });
        svgLicenseManager.loadSVGLicense(this, svgLicenseOption);
    }

    OnGetSuggestionResultListener sugListener = new OnGetSuggestionResultListener()
    {
        @Override
        public void onGetSuggestionResult(SuggestionResult suggestionResult) {
            FissionLogUtils.d("wl", "------onGetSuggestionResult-----");
            //处理sug检索结果
            BdWatchMapSugSearchRecInfo recInfo = new BdWatchMapSugSearchRecInfo();
            List<BdWatchMapSugSearchRecInfo.Suggestion> suggestionList = new ArrayList<>();
            List<SuggestionResult.SuggestionInfo> suggestionInfos = suggestionResult.getAllSuggestions();
            if(suggestionInfos!=null && suggestionInfos.size()>0){
                for(SuggestionResult.SuggestionInfo suggestionInfo: suggestionInfos){
                    BdWatchMapSugSearchRecInfo.Suggestion suggestion = recInfo.new Suggestion();
                    suggestion.key = suggestionInfo.key;
                    suggestion.address = suggestionInfo.address;
                    suggestion.tag = suggestionInfo.tag;
                    suggestion.district = suggestionInfo.district;
                    suggestion.city = suggestionInfo.city;
                    suggestion.uid = suggestionInfo.uid;

                    if(suggestionInfo.getPt() !=null){
                        BdWatchMapSugSearchRecInfo.Location location = recInfo.new Location();
                        location.latitude = suggestionInfo.getPt().latitude;
                        location.longitude = suggestionInfo.getPt().longitude;
                        suggestion.location = location;
                    }

                    List<PoiChildrenInfo> poiChildrenInfos = suggestionInfo.getPoiChildrenInfoList();
                    if(poiChildrenInfos!=null && poiChildrenInfos.size()>0){
                        BdWatchMapSugSearchRecInfo.Children children = recInfo.new Children();
                        children.name = poiChildrenInfos.get(0).getName();
                        children.showName = poiChildrenInfos.get(0).getShowName();
                        children.uid = poiChildrenInfos.get(0).getUid();
                        suggestion.children = children;
                    }

                    suggestionList.add(suggestion);

                }
                recInfo.setSuggestionList(suggestionList);

                BaseBdWatchJsonData jsonData = new BaseBdWatchJsonData();
                jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(suggestionResult.error));
                jsonData.setResult(recInfo);
                sb.append("百度手表矢量地图sug搜索结果："+GsonUtils.toJson(jsonData));
                sb.append("\n");
                sb.append("------------------------------------------");
                tv_log.setText(sb.toString());
                LogUtils.d("wl", sb.toString());
                SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_SUG_SEARCH_REQ), GsonUtils.toJson(jsonData)));
            }else{
                BaseBdWatchJsonData jsonData = new BaseBdWatchJsonData();
                jsonData.setError_code(ParseErrorCodeUtils.getInstance().getErrorCode(suggestionResult.error));
                SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_SUG_SEARCH_REQ), GsonUtils.toJson(jsonData)));
            }
        }
    };

    IBRoutePlanListener ibRoutePlanListener = new IBRoutePlanListener() {
        @Override
        public void onRoutePlanStart() {

        }

        @Override
        public void onRoutePlanSuccess() {

        }

        @Override
        public void onRoutePlanFail(BikeRoutePlanError bikeRoutePlanError) {

        }
    };

    private void onSugSearch(BdWatchMapSugSearchReqInfo sugSearchReqInfo){
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(sugListener);
        /**
         * 在您的项目中，keyword为随您的输入变化的值
         */
        mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                .city(sugSearchReqInfo.getCityname())
                .keyword(sugSearchReqInfo.getKeyword()));
        FissionLogUtils.d("wl", "---onSugSearch--");
    }

    private void onSvgDataReq(BdWatchMapSvgReqInfo req){
        curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SVG;
        // svg瓦片下载
        String coord = req.getCoord();
        BdCoord bdCoord = GsonUtils.fromJson(coord, BdCoord.class);
        SVGTileSearchOption svgTileSearchOption = new SVGTileSearchOption();
        svgTileSearchOption.akCipher(req.getAk());
        svgTileSearchOption.deviceIDCipher(req.getDevice_id());
        svgTileSearchOption.coordCipher(req.getCoord());
        svgTileSearchOption.sign(req.getSign());
        svgTileSearchOption.time(req.getTime());
        svgTileSearchOption.x(215893);
        svgTileSearchOption.y(99299);
        svgTileSearchOption.z(18);
        if(bdCoord!=null){
            svgTileSearchOption.x(bdCoord.getX());
            svgTileSearchOption.y(bdCoord.getY());
            svgTileSearchOption.z(bdCoord.getZ());
            FissionLogUtils.d("wl", "--onSvgDataReq---"+bdCoord);
        }
        FissionLogUtils.d("wl", "---onSvgDataReq222----"+svgTileSearchOption);

        SVGTileSearch svgTileSearch = SVGTileSearch.newInstance();
        svgTileSearch.setOnGetSVGTileSearchResultListener(new OnGetSVGTileSearchResultListener() {
            @Override
            public void onGetSVGTileResult(SVGTileResult result) {
                errorCode = ParseErrorCodeUtils.getInstance().getErrorCode(result.error);
                String svgPath = result.getTilePath();
                bdWatchMapSvgRecInfo = new BdWatchMapSvgRecInfo();
                bdWatchMapSvgRecInfo.setCoord(req.getCoord());
                bdWatchMapSvgRecInfo.setTime(req.getTime());
                LogUtils.d("svg111", "百度地图SDK下载svg图片地址："+svgPath+", xyz信息："+req.getCoord());
                fileName = req.getTime()+".bin";
                curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SVG;
                BdMapFileManage.getInstance().sendSvgFile(svgPath, fileName, curCmdId);
            }
        });
        svgTileSearch.requestSVGTile(svgTileSearchOption);
    }

    private void initSendCmdTimeOutListener(){
        if(rxTimerUtil != null){
            rxTimerUtil.cancelTimer();
            rxTimerUtil = null;
        }
        rxTimerUtil = new RxTimerUtil();
        rxTimerUtil.timer(200, new RxTimerUtil.RxAction() {
            @Override
            public void action(long number) {
                if(tryNum < MAX_TRY_NUM){
                    if(HiSiliconSppCmdID.COMMAND_ID_SEND_SVG.equals(curCmdId)){
                        if(!TextUtils.isEmpty(lastJsonData)){
                            FissionSdkBleManage.getInstance().sendSvg(lastJsonData);
                        }
                    }else if(HiSiliconSppCmdID.COMMAND_ID_SEND_SUG_REC.equals(curCmdId)){
                        String sugPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.SUG_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onSugSearchRec(sugPath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_REC_POI.equals(curCmdId)){
                        String poiPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.POI_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().recPoi(poiPath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_ROUTE_WALKING_SEARCH_REC.equals(curCmdId)){
                        String routeWalkPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.ROUTE_WALK_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onRouteWalkingSearchRec(routeWalkPath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_ROUTE_RIDING_SEARCH_REC.equals(curCmdId)){
                        String routeRidingPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.ROUTE_RIDING_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onRouteRidingSearchRec(routeRidingPath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_ROUTE_TRANSIT_SEARCH_REC.equals(curCmdId)){
                        String routeTransitPath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.ROUTE_TRANSIT_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onRouteTransitSearchRec(routeTransitPath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_REMAIN_ROUTE_REC.equals(curCmdId)){
                        String remainRoutePath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.REMAIN_ROUTE_UPDATE_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onRemainRouteUpdateRec(remainRoutePath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_REROUTE_COMPLETE_REC.equals(curCmdId)){
                        String rerouteCompletePath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.REROUTE_COMPLETE_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onReRouteCompleteRec(rerouteCompletePath+"\0");
                    }else if(HiSiliconSppCmdID.COMMAND_ID_REGEOCODE_SEARCH_REC.equals(curCmdId)){
                        String regeocodePath = BdMapFileManage.DEFAULT_FILE_PATH_JSON+BdMapFileManage.REGEOCODE_POI_JSON_DATA_FILENAME;
                        FissionSdkBleManage.getInstance().onRegeocodeSearchRec(regeocodePath+"\0");
                    }
                    tryNum++;
                    initSendCmdTimeOutListener();
                }else{
                    SvgTaskManage.getInstance().setWorking(false);
                    SvgTaskManage.getInstance().executeHiSiWatchReqTask();
                    FissionLogUtils.e("svg111", "---发送完成三次通知失败--"+lastJsonData);
                }
            }
        });
    }

    public void onBatchPreloadingSvgDataReq(BdWatchMapSvgReqInfo req){
        getAllSvgFileByCoord(GsonUtils.fromJson(req.getCoord(), BdCoord.class));
        if(BdMapFileManage.getInstance().getUpdateSvgList().size()>0){
            for(String svgFile: BdMapFileManage.getInstance().getUpdateSvgList()){
                ArrayList<String> preloadSvgData = BdMapFileManage.getInstance().getPreloadSvgList();
                if(preloadSvgData.size() == 0){
                    preloadSvgData = BdMapFileManage.getInstance().getCachePreloadSvgList();
                }
                if(!preloadSvgData.contains(svgFile)){
                    String name = svgFile.substring(0, svgFile.lastIndexOf("."));
                    String [] xyz = name.split("_");
                    BdCoord bdCoord = new BdCoord();

                    bdCoord.setX(Integer.parseInt(xyz[0]));
                    bdCoord.setY(Integer.parseInt(xyz[1]));
                    bdCoord.setZ(Integer.parseInt(xyz[2]));

                    BdWatchMapSvgReqInfo bdWatchMapSvgReqInfo = new BdWatchMapSvgReqInfo();
                    bdWatchMapSvgReqInfo.setCoord(GsonUtils.toJson(bdCoord));
                    bdWatchMapSvgReqInfo.setAk(req.getAk());
                    bdWatchMapSvgReqInfo.setTime(req.getTime());
                    bdWatchMapSvgReqInfo.setSign(req.getSign());
                    bdWatchMapSvgReqInfo.setDevice_id(req.getDevice_id());
                    bdWatchMapSvgReqInfo.setRequest(req.getRequest());

                    preloadSvgNum++;
                    batchDownloadSvgFile(bdWatchMapSvgReqInfo);
                }
            }
            FissionLogUtils.d("svg222", "---onBatchPreloadingSvgDataReq---"+preloadSvgNum);
            BdMapFileManage.getInstance().getPreloadSvgList().clear();
            BdMapFileManage.getInstance().getPreloadSvgList().addAll(BdMapFileManage.getInstance().getUpdateSvgList());
            BdMapFileManage.getInstance().setCachePreloadSvgList(BdMapFileManage.getInstance().getPreloadSvgList());
            preloadSvgNum = 0;
        }
    }

    public List<String> onDiffSvgReqList(BdWatchMapSvgReqInfo req){
        List<String> list = new ArrayList<>();
        getAllSvgFileByCoord(GsonUtils.fromJson(req.getCoord(), BdCoord.class));
        if(BdMapFileManage.getInstance().getUpdateSvgList().size()>0){
            for(String svgFile: BdMapFileManage.getInstance().getUpdateSvgList()){
                ArrayList<String> preloadSvgData = BdMapFileManage.getInstance().getPreloadSvgList();
                if(preloadSvgData.size() == 0){
                    preloadSvgData = BdMapFileManage.getInstance().getCachePreloadSvgList();
                }
                if(!preloadSvgData.contains(svgFile)){
                    list.add(svgFile);
                    FissionLogUtils.d("diff", "---onDiffSvgReqList---"+svgFile);
                }
            }
            FissionLogUtils.d("diff", "---onDiffSvgReqList---"+preloadSvgNum);
            BdMapFileManage.getInstance().getPreloadSvgList().clear();
            BdMapFileManage.getInstance().getPreloadSvgList().addAll(BdMapFileManage.getInstance().getUpdateSvgList());
            BdMapFileManage.getInstance().setCachePreloadSvgList(BdMapFileManage.getInstance().getPreloadSvgList());
            preloadSvgNum = 0;
        }
        return list;
    }

    public void sendPreLoadingSvgFileList(){
        BdMapFileManage.getInstance().setStartTime(System.currentTimeMillis());
        String dir = getPath() + "/fission_baidu_svg/";
        List<File> fileList = new ArrayList<>();
        int initX = 215827;
        int initY = 99317;
        int initZ = 18;
        for(int i = initX-(MAX_PRELOADING_NUM /2); i < initX+ MAX_PRELOADING_NUM /2; i++){
            for(int j = initY-(MAX_PRELOADING_NUM /2); j< initY+ MAX_PRELOADING_NUM /2; j++){
                StringBuffer sb = new StringBuffer();
                sb.append(dir);
                sb.append(i);
                sb.append("_");
                sb.append(j);
                sb.append("_");
                sb.append(initZ);
                sb.append(".bin");
                File file = new File(sb.toString());
                FissionLogUtils.d("preload", "--svgPath--"+file.getAbsolutePath());
                fileList.add(file);
                BdMapFileManage.getInstance().getPreloadSvgList().add(file.getName());
            }
        }
        BdMapFileManage.getInstance().setCachePreloadSvgList(BdMapFileManage.getInstance().getPreloadSvgList());
        FissionLogUtils.d("preload", "预加载Svg文件数量："+fileList.size());
        curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SVG;
        BdMapFileManage.getInstance().sendSvgFileList(fileList, curCmdId);
    }

    private void getAllSvgFileByCoord(BdCoord bdCoord){
        BdMapFileManage.getInstance().getUpdateSvgList().clear();
        int initX = bdCoord.getX();
        int initY = bdCoord.getY();
        int initZ = bdCoord.getZ();
        for(int i = initX-(MAX_PRELOADING_NUM /2); i < initX+ MAX_PRELOADING_NUM /2; i++){
            for(int j = initY-(MAX_PRELOADING_NUM /2); j< initY+ MAX_PRELOADING_NUM /2; j++){
                StringBuffer sb = new StringBuffer();
                sb.append(i);
                sb.append("_");
                sb.append(j);
                sb.append("_");
                sb.append(initZ);
                sb.append(".bin");
                BdMapFileManage.getInstance().getUpdateSvgList().add(sb.toString());
            }
        }
        FissionLogUtils.d("svg111", "--getAllSvgFileByCoord--"+BdMapFileManage.getInstance().getUpdateSvgList().size());
    }

    // 实现定位监听器
    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            double latitude = location.getLatitude();    //获取纬度信息
            double longitude = location.getLongitude();    //获取经度信息
            float radius = location.getRadius();    //获取定位精度，默认值为0.0f
            String coorType = location.getCoorType();
            String address = location.getAddrStr();
            // 停止定位
//            mLocationClient.stop();
            FissionLogUtils.d("wl", "持续定位信息，经度："+latitude+", 纬度："+location);

            if(isFirstBatch){
                isFirstBatch = false;
                BdWatchLocation bdWatchLocation = new BdWatchLocation(latitude, longitude);
                HiSiTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_FIRST_BATCH), GsonUtils.toJson(bdWatchLocation)));
            }
            BdWatchMapGeolocation geolocation = new BdWatchMapGeolocation();
            int coordType =0;
            if(BDLocation.BDLOCATION_COOR_TYPE_GCJ02.equals(location.getCoorType())){
                coordType = 1;
            }else if(BDLocation.BDLOCATION_GCJ02_TO_BD09LL.equals(location.getCoorType())){
                coordType = 2;
            }
            geolocation.setCoord_type(coordType);
            geolocation.setTime(String.valueOf(System.currentTimeMillis()));
            geolocation.setDirection((int)location.getDirection());
            geolocation.setLatitude(latitude);
            geolocation.setLongitude(longitude);
            geolocation.setSpeed(location.getSpeed());
            BaseBdWatchJsonData jsonData = new BaseBdWatchJsonData();
            jsonData.setError_code(0);
            jsonData.setResult(geolocation);
//            FissionSdkBleManage.getInstance().onGeolocationRec(GsonUtils.toJson(jsonData)+"\0");

            HiSiWatchReqTask hiSiWatchReqTask = new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM, HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_GET_GEOLOCATION_CALLBACK), GsonUtils.toJson(jsonData)+"\0");
            HiSiTaskManage.getInstance().addHiSiWatchReqTask(hiSiWatchReqTask);
            FissionLogUtils.d("wl", "百度地图定位结果， 发送给手表json："+GsonUtils.toJson(jsonData));
        }
    }


    private void batchDownloadSvgFile(BdWatchMapSvgReqInfo req){
        curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SVG;
        // svg瓦片下载
        String coord = req.getCoord();
        BdCoord bdCoord = GsonUtils.fromJson(coord, BdCoord.class);
        SVGTileSearchOption svgTileSearchOption = new SVGTileSearchOption();
        svgTileSearchOption.akCipher(req.getAk());
        svgTileSearchOption.deviceIDCipher(req.getDevice_id());
        svgTileSearchOption.coordCipher(req.getCoord());
        svgTileSearchOption.sign(req.getSign());
        svgTileSearchOption.time(req.getTime());
        svgTileSearchOption.x(215893);
        svgTileSearchOption.y(99299);
        svgTileSearchOption.z(18);
        if(bdCoord!=null){
            svgTileSearchOption.x(bdCoord.getX());
            svgTileSearchOption.y(bdCoord.getY());
            svgTileSearchOption.z(bdCoord.getZ());
            FissionLogUtils.d("wl", "--onSvgDataReq---"+bdCoord);
        }
        FissionLogUtils.d("wl", "---onSvgDataReq222----"+svgTileSearchOption);

        SVGTileSearch svgTileSearch = SVGTileSearch.newInstance();

        svgTileSearch.setOnGetSVGTileBatchSearchResultListener(new OnGetSVGTileBatchSearchResultListener() {
            @Override
            public void onGetBatchSVGTilesResult(SVGTileResult svgTileResult) {
                String svgPath = svgTileResult.getTileZipPath();
                String dir = getPath() + "/fission_baidu_svg/";
                FissionLogUtils.d("wl", "----onGetBatchSVGTilesResult-----"+svgPath);
                try {
                    List<File> fileList = ZipUtils.unzipFile(svgPath, dir);
                    if(fileList!=null && fileList.size()>0){
                        curCmdId = HiSiliconSppCmdID.COMMAND_ID_SEND_SVG;
                        FissionLogUtils.d("wl", "svg批量下载文件数量："+fileList.size());
                        for(File file: fileList){
                                int errorCode = ParseErrorCodeUtils.getInstance().getErrorCode(svgTileResult.error);
                                BdWatchMapSvgRecInfo bdWatchMapSvgRecInfo = new BdWatchMapSvgRecInfo();
                                FissionLogUtils.d("wl", "svg批量文件名称："+file.getName());
                                String name = file.getName().replace(".bin", "");
                                String [] xyz = name.split("_");
                                BdCoord bdCoord = new BdCoord();

                                bdCoord.setX(Integer.parseInt(xyz[0]));
                                bdCoord.setY(Integer.parseInt(xyz[1]));
                                bdCoord.setZ(Integer.parseInt(xyz[2]));
                                bdWatchMapSvgRecInfo.setCoord(GsonUtils.toJson(bdCoord));
                                bdWatchMapSvgRecInfo.setTime(String.valueOf(System.currentTimeMillis()));
                                LogUtils.d("svg111", "--batchDownloadSvgFile--百度地图SDK下载svg图片地址："+svgPath+", xyz信息："+req.getCoord());
                                fileName = name+".bin";

                                bdWatchMapSvgRecInfo.setSvg(BdMapFileManage.DEFAULT_FILE_PATH+fileName);
                                BaseBdWatchJsonData<BdWatchMapSvgRecInfo> jsonData = new BaseBdWatchJsonData<>();
                                jsonData.setError_code(errorCode);
                                jsonData.setResult(bdWatchMapSvgRecInfo);
                                SvgTaskInfo svgTaskInfo = new SvgTaskInfo();
                                svgTaskInfo.setSvgPath(file.getAbsolutePath());
                                svgTaskInfo.setFileName(fileName);
                                svgTaskInfo.setCurCmdId(HiSiliconSppCmdID.COMMAND_ID_SEND_SVG);
                                svgTaskInfo.setSvgJsonDataRec(GsonUtils.toJson(jsonData)+"\0");
                                ThreadUtils.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_SEND_SVG), svgTaskInfo));
                                        FissionLogUtils.d("svg111", "svg批量下载："+svgTaskInfo);
                                    }
                                });
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        svgTileSearch.searchBatchSVGTiles(svgTileSearchOption, 6, !isFirstBatch);
        FissionLogUtils.d("wl", "svg批量下载开始");
    }

    private void routeWalkingSearch(BdWatchRouteSearch routeSearch){
        if(mRoutePlanSearch == null){
            mRoutePlanSearch = RoutePlanSearch.newInstance();
            mRoutePlanSearch.setOnGetRoutePlanResultListener(listener);
        }

        PlanNode stNode;
        PlanNode enNode;
        if(routeSearch.getFrom().pt != null){
            stNode = PlanNode.withLocation(new LatLng(routeSearch.getFrom().pt.latitude, routeSearch.getFrom().pt.longitude));
        }else if(routeSearch.getFrom().cityID >0){
            stNode = PlanNode.withCityCodeAndPlaceName(routeSearch.getFrom().cityID, routeSearch.getFrom().name);
        }else {
            stNode = PlanNode.withCityNameAndPlaceName(routeSearch.getFrom().cityName, routeSearch.getFrom().name);
        }

        if(routeSearch.getTo().pt != null){
            enNode = PlanNode.withLocation(new LatLng(routeSearch.getTo().pt.latitude, routeSearch.getTo().pt.longitude));
        }else if(routeSearch.getFrom().cityID >0){
            enNode = PlanNode.withCityCodeAndPlaceName(routeSearch.getTo().cityID, routeSearch.getTo().name);
        }else {
            enNode = PlanNode.withCityNameAndPlaceName(routeSearch.getTo().cityName, routeSearch.getTo().name);
        }

        mRoutePlanSearch.walkingSearch((new WalkingRoutePlanOption())
                .from(stNode)
                .to(enNode));

        FissionLogUtils.d("wl", "--routeWalkingSearch--"+routeSearch);
    }

    private void routeRidingSearch(BdWatchRouteRidingSearch routeSearch){
        if(mRoutePlanSearch == null){
            mRoutePlanSearch = RoutePlanSearch.newInstance();
            mRoutePlanSearch.setOnGetRoutePlanResultListener(listener);
        }

        PlanNode stNode;
        PlanNode enNode;
        if(routeSearch.getFrom().pt != null){
            stNode = PlanNode.withLocation(new LatLng(routeSearch.getFrom().pt.latitude, routeSearch.getFrom().pt.longitude));
        }else if(routeSearch.getFrom().cityID >0){
            stNode = PlanNode.withCityCodeAndPlaceName(routeSearch.getFrom().cityID, routeSearch.getFrom().name);
        }else {
            stNode = PlanNode.withCityNameAndPlaceName(routeSearch.getFrom().cityName, routeSearch.getFrom().name);
        }

        if(routeSearch.getTo().pt != null){
            enNode = PlanNode.withLocation(new LatLng(routeSearch.getTo().pt.latitude, routeSearch.getTo().pt.longitude));
        }else if(routeSearch.getFrom().cityID >0){
            enNode = PlanNode.withCityCodeAndPlaceName(routeSearch.getTo().cityID, routeSearch.getTo().name);
        }else {
            enNode = PlanNode.withCityNameAndPlaceName(routeSearch.getTo().cityName, routeSearch.getTo().name);
        }

        mRoutePlanSearch.bikingSearch((new BikingRoutePlanOption())
                .from(stNode)
                .to(enNode)
                // ridingType  0 普通骑行，1 电动车骑行
                // 默认普通骑行
                .ridingType(routeSearch.getRidingType()));
        FissionLogUtils.d("wl", "--routeRidingSearch--"+routeSearch);
    }

    private void routeTransitSearch(BdWatchRouteTransitSearch routeSearch){
        if(mRoutePlanSearch == null){
            mRoutePlanSearch = RoutePlanSearch.newInstance();
            mRoutePlanSearch.setOnGetRoutePlanResultListener(listener);
        }

        PlanNode stNode;
        PlanNode enNode;
        if(routeSearch.getFrom().pt != null){
            stNode = PlanNode.withLocation(new LatLng(routeSearch.getFrom().pt.latitude, routeSearch.getFrom().pt.longitude));
        }else if(routeSearch.getFrom().cityID >0){
            stNode = PlanNode.withCityCodeAndPlaceName(routeSearch.getFrom().cityID, routeSearch.getFrom().name);
        }else {
            stNode = PlanNode.withCityNameAndPlaceName(routeSearch.getFrom().cityName, routeSearch.getFrom().name);
        }

        if(routeSearch.getTo().pt != null){
            enNode = PlanNode.withLocation(new LatLng(routeSearch.getTo().pt.latitude, routeSearch.getTo().pt.longitude));
        }else if(routeSearch.getFrom().cityID >0){
            enNode = PlanNode.withCityCodeAndPlaceName(routeSearch.getTo().cityID, routeSearch.getTo().name);
        }else {
            enNode = PlanNode.withCityNameAndPlaceName(routeSearch.getTo().cityName, routeSearch.getTo().name);
        }

        TransitRoutePlanOption.TransitPolicy transitPolicy = TransitRoutePlanOption.TransitPolicy.EBUS_TIME_FIRST;
        switch (routeSearch.getTransitPolicy()){
            case 0:
                transitPolicy =  TransitRoutePlanOption.TransitPolicy.EBUS_TIME_FIRST;
                break;

            case 1:
                transitPolicy =  TransitRoutePlanOption.TransitPolicy.EBUS_TRANSFER_FIRST;
                break;

            case 2:
                transitPolicy =  TransitRoutePlanOption.TransitPolicy.EBUS_WALK_FIRST;
                break;

            case 3:
                transitPolicy =  TransitRoutePlanOption.TransitPolicy.EBUS_NO_SUBWAY;
                break;
        }

        mRoutePlanSearch.transitSearch((new TransitRoutePlanOption())
                .from(stNode)
                .to(enNode)
                .policy(transitPolicy)
                .city(routeSearch.getCity()));
        FissionLogUtils.d("wl", "--routeRidingSearch--"+routeSearch);
    }

    private void initNaviEngine(BdWatchMapNaviInitReqInfo reqInfo){
        naviType = reqInfo.getNavi_type();

        if(naviType == 1){
            BikeNavigateHelper.getInstance().initNaviEngine(this, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    //骑行导航初始化成功之后的回调
                    sb.append("\n");
                    sb.append("导航初始化成功");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", "导航初始化成功");
                    FissionSdkBleManage.getInstance().onNaviInitRec("1");

                    routePlanWithRouteNode(reqInfo);
                }

                @Override
                public void engineInitFail() {
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                    //骑行导航初始化失败之后的回调
                    sb.append("\n");
                    sb.append("导航初始化失败");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", "导航初始化失败");
                    FissionSdkBleManage.getInstance().onNaviInitRec("0");
                }
            });
        }else if(naviType == 0){
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {

                @Override
                public void engineInitSuccess() {
                    sb.append("\n");
                    sb.append("步行导航初始化成功");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", "步行导航初始化成功");
                    FissionSdkBleManage.getInstance().onNaviInitRec("1");

                    routePlanWithRouteNode(reqInfo);
                }

                @Override
                public void engineInitFail() {
                    WalkNavigateHelper.getInstance().unInitNaviEngine();
                    //引擎初始化失败的回调
                    sb.append("\n");
                    sb.append("步行导航初始化失败");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", "步行导航初始化失败");
                    FissionSdkBleManage.getInstance().onNaviInitRec("0");
                }
            });
        }

    }

    private void routePlanWithRouteNode(BdWatchMapNaviInitReqInfo reqInfo){
        if(reqInfo == null || reqInfo.getFrom()== null || reqInfo.getTo() == null){
            FissionLogUtils.d("wl", "发起算路参数异常："+reqInfo);
            return;
        }
        if(naviType == 0){
            WalkRouteNodeInfo startNodeInfo = new WalkRouteNodeInfo();
            startNodeInfo.setLocation(new LatLng(reqInfo.getFrom().latitude, reqInfo.getFrom().longitude));
            WalkRouteNodeInfo endNodeInfo = new WalkRouteNodeInfo();
            endNodeInfo.setLocation(new LatLng(reqInfo.getTo().latitude, reqInfo.getTo().longitude));
            mWalkNaviLaunchParam = new WalkNaviLaunchParam().startNodeInfo(startNodeInfo).endNodeInfo(endNodeInfo);
            //发起算路
            WalkNavigateHelper.getInstance().routePlanWithRouteNode(mWalkNaviLaunchParam, new IWRoutePlanListener() {
                @Override
                public void onRoutePlanStart() {
                    //开始算路的回调
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_START);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路开始："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanStartRec(GsonUtils.toJson(recInfo));
                }

                @Override
                public void onRoutePlanSuccess() {
                    //算路成功
                    //跳转至诱导页面
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_SUCCESS);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路成功："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanSuccessRec(GsonUtils.toJson(recInfo));

                    WalkRouteResult walkRouteResult = WalkNavigateHelper.getInstance().getWalkNaviRouteInfo();
                    if(walkRouteResult == null){
                        return;
                    }
                    BdWatchMapRemainRoute remainRoute = new BdWatchMapRemainRoute();
                    remainRoute.setMsg_type(BdWatchMsgType.ON_REMAIN_ROUTE_UPDATE);
                    remainRoute.setNavi_type(naviType);
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

                    sb.append("\n");
                    sb.append("百度手表步行导航路线回调："+GsonUtils.toJson(remainRoute));
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
//                    FissionSdkBleManage.getInstance().onRemainRouteUpdateRec(GsonUtils.toJson(remainRoute));
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_REMAIN_ROUTE_REC;
                    SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_ROUTE_UPDATE_REC), GsonUtils.toJson(remainRoute)));

                    startWalkingNavi();
                }

                @Override
                public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {
                    //算路失败的回调
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_FAIL);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路失败："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanFailRec(GsonUtils.toJson(recInfo));
                }
            });
        }else if(naviType == 1){
            BikeRouteNodeInfo startNodeInfo = new BikeRouteNodeInfo();
            startNodeInfo.setLocation(new LatLng(reqInfo.getFrom().latitude, reqInfo.getFrom().longitude));
            BikeRouteNodeInfo endNodeInfo = new BikeRouteNodeInfo();
            endNodeInfo.setLocation(new LatLng(reqInfo.getTo().latitude, reqInfo.getTo().longitude));
            mBikeNaviLaunchParam = new BikeNaviLaunchParam().startNodeInfo(startNodeInfo).endNodeInfo(endNodeInfo).vehicle(0);
            //发起算路
            BikeNavigateHelper.getInstance().routePlanWithRouteNode(mBikeNaviLaunchParam, new IBRoutePlanListener() {
                @Override
                public void onRoutePlanStart() {
                    //执行算路开始的逻辑
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_START);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路开始："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanStartRec(GsonUtils.toJson(recInfo));

                }

                @Override
                public void onRoutePlanSuccess() {
                    //算路成功
                    //跳转至诱导页面
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_SUCCESS);
                    recInfo.setNavi_type(naviType);

                    sb.append("\n");
                    sb.append("百度手表导航算路成功："+GsonUtils.toJson(recInfo));
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanSuccessRec(GsonUtils.toJson(recInfo)+"\0");

                    BikeRouteResult bikeRouteResult = BikeNavigateHelper.getInstance().getBikeNaviRouteInfo();
                    if(bikeRouteResult == null){
                        return;
                    }
                    BdWatchMapRemainRoute remainRoute = new BdWatchMapRemainRoute();
                    remainRoute.setMsg_type(BdWatchMsgType.ON_REMAIN_ROUTE_UPDATE);
                    remainRoute.setNavi_type(1);
                    BdWatchMapRemainRoute.RouteLine routeLine = remainRoute.new RouteLine();
                    routeLine.distance = bikeRouteResult.getDistance();
                    routeLine.duration = bikeRouteResult.getDuration();
                    List<BdWatchMapRemainRoute.Point> points = new ArrayList<>();
                    ArrayList<LatLng> latLngs = bikeRouteResult.getPositions();
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

                    sb.append("\n");
                    sb.append("百度手表导航路线回调："+GsonUtils.toJson(remainRoute));
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
//                    FissionSdkBleManage.getInstance().onRemainRouteUpdateRec(GsonUtils.toJson(remainRoute));
                    curCmdId = HiSiliconSppCmdID.COMMAND_ID_REMAIN_ROUTE_REC;
                    SvgTaskManage.getInstance().addHiSiWatchReqTask(new HiSiWatchReqTask(System.currentTimeMillis(), FissionConstant.CMD_PRIORITY_MEDIUM,HiSiliconSppCmdHelper.getCmdType(HiSiliconSppCmdID.COMMAND_ID_MESSAGE_CENTER_REMAIN_ROUTE_UPDATE_REC), GsonUtils.toJson(remainRoute)));

                    startBikingNavi();
                }

                @Override
                public void onRoutePlanFail(BikeRoutePlanError bikeRoutePlanError) {
                    //执行算路失败的逻辑
                    BdWatchMapRoutePlanRecInfo recInfo = new BdWatchMapRoutePlanRecInfo();
                    recInfo.setMsg_type(BdWatchMsgType.ON_ROUTE_PLAN_FAIL);
                    recInfo.setNavi_type(naviType);

                    sb.append("百度手表导航算路失败："+GsonUtils.toJson(recInfo));
                    sb.append("\n");
                    sb.append("------------------------------------------");
                    tv_log.setText(sb.toString());
                    LogUtils.d("wl", sb.toString());
                    FissionSdkBleManage.getInstance().onRoutePlanFailRec(GsonUtils.toJson(recInfo)+"\0");
                }
            });
        }
    }

    private void initVoiceSearch(){
        BaiDuAiUtils.setBdAiVoiceListener(new BaiDuAiUtils.BdAiVoiceListener() {
            @Override
            public void onChat(String question, String answer) {

            }

            @Override
            public void onVoiceSearch(String text) {
                JsAiVoiceJsonResult voiceJsonResult = new JsAiVoiceJsonResult(JsAiVoiceJsonResult.TYPE_VOICE_CONTENT, text);
                if(TextUtils.isEmpty(text)){
                    voiceJsonResult.setCode(1); //语音搜索失败
                }else{
                    voiceJsonResult.setCode(0); //语音搜索成功
                }
                FissionSdkBleManage.getInstance().sendQuestionData(GsonUtils.toJson(voiceJsonResult));
            }

            @Override
            public void onSpeechResult(String result, String type) {

            }

            @Override
            public void onCreateDial(String text) {

                FissionLogUtils.d("wl", "---Ai创建表盘--"+text);
            }

            @Override
            public void onVoiceFile(File file) {

            }

            @Override
            public void onError(int code, String msg) {

            }
        });
    }
}
