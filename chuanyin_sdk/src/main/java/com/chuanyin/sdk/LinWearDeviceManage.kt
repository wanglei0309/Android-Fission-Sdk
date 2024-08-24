package com.chuanyin.sdk

import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fission.wear.sdk.v2.FissionSdkBleManage
import com.fission.wear.sdk.v2.bean.DeviceBattery
import com.fission.wear.sdk.v2.bean.FssStatus
import com.fission.wear.sdk.v2.bean.SystemFunctionSwitch
import com.fission.wear.sdk.v2.callback.BleConnectListener
import com.fission.wear.sdk.v2.callback.FissionAtCmdResultListener
import com.fission.wear.sdk.v2.callback.FissionBigDataCmdResultListener
import com.fission.wear.sdk.v2.config.BleComConfig
import com.fission.wear.sdk.v2.constant.FissionConstant
import com.fission.wear.sdk.v2.utils.FissionLogUtils
import com.polidea.rxandroidble2.RxBleConnection
import com.szfission.wear.sdk.AnyWear
import com.szfission.wear.sdk.bean.FissionAlarm
import com.szfission.wear.sdk.bean.HeartRateRecord
import com.szfission.wear.sdk.bean.param.DkWaterRemind
import com.szfission.wear.sdk.bean.param.DndRemind
import com.szfission.wear.sdk.bean.param.LiftWristPara
import com.szfission.wear.sdk.constant.FissionEnum
import com.transsion.wearablelinksdk.ITransHealthDevice
import com.transsion.wearablelinksdk.bean.TemperatureFormat
import com.transsion.wearablelinksdk.bean.TimeFormat
import com.transsion.wearablelinksdk.bean.VolumeAction
import com.transsion.wearablelinksdk.bean.WatchAlarmBean
import com.transsion.wearablelinksdk.bean.WatchBrightScreenPeriodBean
import com.transsion.wearablelinksdk.bean.WatchCallInfoBean
import com.transsion.wearablelinksdk.bean.WatchContactBean
import com.transsion.wearablelinksdk.bean.WatchDialBean
import com.transsion.wearablelinksdk.bean.WatchDialLayoutBean
import com.transsion.wearablelinksdk.bean.WatchDoNotDisturbBean
import com.transsion.wearablelinksdk.bean.WatchDrinkWaterBean
import com.transsion.wearablelinksdk.bean.WatchElectronCardInfoBean
import com.transsion.wearablelinksdk.bean.WatchFutureWeatherBean
import com.transsion.wearablelinksdk.bean.WatchHeartRateBean
import com.transsion.wearablelinksdk.bean.WatchPushMessageBean
import com.transsion.wearablelinksdk.bean.WatchSedentaryBean
import com.transsion.wearablelinksdk.bean.WatchTodayWeatherBean
import com.transsion.wearablelinksdk.listener.OnAlarmChangedListener
import com.transsion.wearablelinksdk.listener.OnBloodOxygenChangeListener
import com.transsion.wearablelinksdk.listener.OnCallOperationListener
import com.transsion.wearablelinksdk.listener.OnConnectionStateListener
import com.transsion.wearablelinksdk.listener.OnContactsListener
import com.transsion.wearablelinksdk.listener.OnFirmwareUpgradeListener
import com.transsion.wearablelinksdk.listener.OnHeartRateChangeListener
import com.transsion.wearablelinksdk.listener.OnMediaControlActionListener
import com.transsion.wearablelinksdk.listener.OnSleepChangeListener
import com.transsion.wearablelinksdk.listener.OnSportDataListener
import com.transsion.wearablelinksdk.listener.OnWatchBatteryChangeListener
import com.transsion.wearablelinksdk.listener.OnWatchCameraListener
import com.transsion.wearablelinksdk.listener.OnWatchDialSwitchListener
import com.transsion.wearablelinksdk.listener.OnWatchFaceTransListener
import com.transsion.wearablelinksdk.listener.OnWatchFindPhoneListener
import com.transsion.wearablelinksdk.listener.OnWatchStepChangeListener
import com.transsion.wearablelinksdk.listener.OnWatchStepsDataListener
import java.io.File
import java.lang.RuntimeException
import java.util.Calendar
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * describe:
 * author: wl
 * createTime: 2024/3/22
 */
object LinWearDeviceManage : ITransHealthDevice {

    private var mleConnectListener : BleConnectListener ?= null

    private val BIND_KEY: String  ="bindKey"

    private val TASK_TIME_OUT = 6000L;

    private var onWatchBatteryChangeListener : OnWatchBatteryChangeListener ?= null

    private var onHeartRateChangeListener : OnHeartRateChangeListener ?= null

    override fun connect(mac: String) {
        if (mac.isEmpty()) {
            mleConnectListener?.let {
                FissionSdkBleManage.getInstance().connectBleDevice(mac, null, false,
                    it
                )
            }
            return
        }
        if (TextUtils.isEmpty(SPUtils.getInstance().getString(BIND_KEY))) {
            val time = System.currentTimeMillis()
            val lastTime = (time % 10000).toInt()
            val bindKey = AnyWear.bindDevice(lastTime, mac)
            SPUtils.getInstance().put(BIND_KEY, "$lastTime,$bindKey")
        }
        val bleComConfig = BleComConfig().apply {
            isBind = true
            bindKeys = SPUtils.getInstance().getString("bindKey")
            isNeedSppConnect = true
        }
        mleConnectListener?.let {
            FissionSdkBleManage.getInstance().connectBleDevice(mac, bleComConfig, false,
                it
            )
        }

        FissionLogUtils.d("cy_sdk", "----connect---")
    }

    override fun deleteAlarm(vararg alarm: WatchAlarmBean) {
        val fissionAlarms: MutableList<FissionAlarm> = mutableListOf()
        for(watchAlarm in alarm){
            val fissionAlarm = watchAlarm.date?.let {
                FissionAlarm(watchAlarm.id,0,watchAlarm.enable, it.time,watchAlarm.repeatMode, "")
            }
            if (fissionAlarm != null) {
                fissionAlarms.add(fissionAlarm)
            }
        }
        FissionSdkBleManage.getInstance().setAlarmInfos(fissionAlarms)
        FissionLogUtils.d("cy_sdk", "----deleteAlarm---$fissionAlarms")

    }

    override fun disconnect() {
        SPUtils.getInstance().put(BIND_KEY, "")
        FissionSdkBleManage.getInstance().disconnectBleDevice()

        FissionLogUtils.d("cy_sdk", "----disconnect---")
    }

    override fun findDevice() {
        FissionSdkBleManage.getInstance().findDevice()

        FissionLogUtils.d("cy_sdk", "----findDevice---")
    }


    override fun query24hHeartRateSwitchState(): Boolean {
        // 创建一个 CountDownLatch，用于等待异步结果返回
        val latch = CountDownLatch(1)

        // 用于存储结果的变量
        var result = false

        // 创建回调监听器
        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getSystemFunctionSwitch(systemFunctionSwitch: SystemFunctionSwitch?) {
                super.getSystemFunctionSwitch(systemFunctionSwitch)
                if (systemFunctionSwitch != null) {
                    FissionLogUtils.d("cy_sdk", "----query24hHeartRateSwitchState---"+systemFunctionSwitch.isHrSwitch)
                    result = systemFunctionSwitch.isWtsSwitch
                }
                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }
        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getSystemFunctionSwitch()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(fissionBigDataCmdResultListener)
        // 返回结果
        return result
    }


    override fun queryAlarmsInfo(): List<WatchAlarmBean> {

        val latch = CountDownLatch(1)
        var result : MutableList<WatchAlarmBean> = mutableListOf()

        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getAlarm(fissionAlarms: MutableList<FissionAlarm>?) {
                super.getAlarm(fissionAlarms)
                if(fissionAlarms!=null && fissionAlarms.size>0){
                    for(alarm in fissionAlarms){
                        val enable = alarm.isOpen && alarm.isAlarmActive
                        val year = alarm.year
                        val month = alarm.month
                        val day = alarm.day
                        val hour = alarm.hour
                        val min = alarm.minute

                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month - 1) // 月份从0开始，所以需要减1
                        calendar.set(Calendar.DAY_OF_MONTH, day)
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, min)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        var watchAlarmBean = WatchAlarmBean(alarm.index, alarm.hour, alarm.minute, alarm.weekCode, enable, calendar.time)
                        result.add(watchAlarmBean)
                    }
                    FissionLogUtils.d("cy_sdk", "----queryAlarmsInfo---$result")
                }
            }

        }
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getAlarm()

        try {
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }

        FissionSdkBleManage.getInstance().removeCmdResultListener(
            fissionBigDataCmdResultListener)

        // 返回结果
        return result
    }

    override fun queryBrightScreenPeriod(): WatchBrightScreenPeriodBean {
        // 创建一个 CountDownLatch，用于等待异步结果返回
        val latch = CountDownLatch(1)

        // 用于存储结果的变量
        var result = WatchBrightScreenPeriodBean(0,0,0,0)

        // 创建回调监听器
        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getLiftWristPara(liftWristPara: LiftWristPara?) {
                super.getLiftWristPara(liftWristPara)

                if(liftWristPara!=null){
                    FissionLogUtils.d("cy_sdk", "----queryBrightScreenPeriod---$liftWristPara")
                    val startHour = liftWristPara.startTime/60
                    val startMin = liftWristPara.startTime%60
                    val endHour = liftWristPara.endTime/60
                    val endMin = liftWristPara.endTime%60
                    result.startHour = startHour
                    result.startMinute = startMin
                    result.endHour = endHour
                    result.endMinute = endMin
                }

                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }
        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getLiftWristPara()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(fissionBigDataCmdResultListener)
        // 返回结果
        return result

    }

    override fun queryBrightScreenSwitchState(): Boolean {

        // 创建一个 CountDownLatch，用于等待异步结果返回
        val latch = CountDownLatch(1)

        // 用于存储结果的变量
        var result = false

        // 创建回调监听器
        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getSystemFunctionSwitch(systemFunctionSwitch: SystemFunctionSwitch?) {
                super.getSystemFunctionSwitch(systemFunctionSwitch)
                // 处理获取到的数据
                if (systemFunctionSwitch != null) {
                    FissionLogUtils.d("cy_sdk", "----queryBrightScreenSwitchState---"+systemFunctionSwitch.isWtsSwitch)
                    result = systemFunctionSwitch.isWtsSwitch
                }
                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }
        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getSystemFunctionSwitch()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }

        FissionSdkBleManage.getInstance().removeCmdResultListener(
            fissionBigDataCmdResultListener)
        // 返回结果
        return result
    }

    override fun queryBrightScreenTime(): Int {
        // 创建一个 CountDownLatch，用于等待异步结果返回
        val latch = CountDownLatch(1)

        // 用于存储结果的变量
        var result = 0

        // 创建回调监听器
        val fissionAtCmdResultListener = object : FissionAtCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getScreenKeep(time: String?) {
                super.getScreenKeep(time)

                FissionLogUtils.d("cy_sdk", "----queryBrightScreenTime---$time")

                if (time != null) {
                    result = time.toInt()
                }

                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }

        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionAtCmdResultListener)
        FissionSdkBleManage.getInstance().getScreenKeep()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }

        FissionSdkBleManage.getInstance().removeCmdResultListener(
            fissionAtCmdResultListener)
        // 返回结果
        return result
    }

    override fun queryDeviceBattery() {
        val latch = CountDownLatch(1)

        // 创建回调监听器
        val fissionAtCmdResultListener = object : FissionAtCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getDeviceBattery(deviceBattery: DeviceBattery?) {
                super.getDeviceBattery(deviceBattery)
                if(deviceBattery != null && onWatchBatteryChangeListener!=null){
                    onWatchBatteryChangeListener!!.onBatteryChanged(deviceBattery.battery)
                    FissionLogUtils.d("cy_sdk", "----queryDeviceBattery---$deviceBattery")
                }
                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }

        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionAtCmdResultListener)
        FissionSdkBleManage.getInstance().getDeviceBattery()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }

        FissionSdkBleManage.getInstance().removeCmdResultListener(
            fissionAtCmdResultListener)
    }

    override fun queryDoNotDisturbInfo(): WatchDoNotDisturbBean {
        val latch = CountDownLatch(1)

        // 用于存储结果的变量
        var result = WatchDoNotDisturbBean(false,0,0,0, 0)

        // 创建回调监听器
        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getDndPara(dndRemind: DndRemind?) {
                super.getDndPara(dndRemind)
                if(dndRemind != null){
                    FissionLogUtils.d("cy_sdk", "----queryDoNotDisturbInfo---$dndRemind")
                    val enable = dndRemind.isEnable
                    val startHour = dndRemind.startTime/60
                    val startMin = dndRemind.startTime%60
                    val endHour = dndRemind.endTime/60
                    val endMin = dndRemind.endTime%60
                    result.enable = enable;
                    result.startHour = startHour
                    result.startMinute = startMin
                    result.endHour = endHour
                    result.endMinute = endMin
                }

                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }
        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getDndPara()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(fissionBigDataCmdResultListener)
        // 返回结果
        return result
    }

    override fun queryDrinkWaterInfo(): WatchDrinkWaterBean {
        val latch = CountDownLatch(1)

        // 用于存储结果的变量
        var result = WatchDrinkWaterBean(false,0,0,0, 0, 0)

        // 创建回调监听器
        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getDrinkWaterPara(dkWaterRemind: DkWaterRemind?) {
                super.getDrinkWaterPara(dkWaterRemind)
                if(dkWaterRemind != null){
                    FissionLogUtils.d("cy_sdk", "----queryDrinkWaterInfo---$dkWaterRemind")
                    val enable = dkWaterRemind.isEnable
                    val startHour = dkWaterRemind.startTime/60
                    val startMin = dkWaterRemind.startTime%60
                    val endHour = dkWaterRemind.endTime/60
                    val endMin = dkWaterRemind.endTime%60
                    val remindWeek = dkWaterRemind.remindWeek
                    result.enable = enable;
                    result.startHour = startHour
                    result.startMinute = startMin
                    result.endHour = endHour
                    result.endMinute = endMin
                    result.period = remindWeek
                }

                // 结果获取完成，减少 latch 计数
                latch.countDown()
            }
        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getDrinkWaterPara()

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(fissionBigDataCmdResultListener)
        // 返回结果
        return result
    }

    override fun queryElectronCard(): WatchElectronCardInfoBean {
        FissionLogUtils.d("cy_sdk", "----queryElectronCard---固件暂时不支持电子卡功能！！")
        return WatchElectronCardInfoBean(0, 0, emptyList())
    }

    override fun queryFirmwareVersion(): kotlin.Triple<Int, String, String> {
        TODO("Not yet implemented")
        // 等待app提供OTA查询， 下载功能jar包。 //0325下班前未能提供， 自己根据api文档开发。
    }

    override fun queryHeartRateData(date: Date) {
        val startTime = getMidnightTimestamp(date) /1000
        val endTime = startTime + 3600*24 -1

        val latch = CountDownLatch(1)

        // 创建回调监听器
        val fissionBigDataCmdResultListener = object : FissionBigDataCmdResultListener() {
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun getHeartRateRecord(heartRateRecords: MutableList<HeartRateRecord>?) {
                super.getHeartRateRecord(heartRateRecords)
                if(heartRateRecords != null && onHeartRateChangeListener!=null){
                    val timeList = mutableListOf<Long>()
                    val hrList = mutableListOf<Int>()
                    var timeInterval = 10;
                    for(heartRateRecord in heartRateRecords){
                        timeList.addAll(heartRateRecord.hrListTime)
                        hrList.addAll(heartRateRecord.hrList)
                    }
                    val resultArray = fillHeartRateValues(timeList, hrList, 24*60/timeInterval, date)
                    FissionLogUtils.d("cy_sdk", "-------queryHeartRateData------$resultArray")
                    val watchHeartRateBean = WatchHeartRateBean(date, timeInterval, resultArray.toList())
                    onHeartRateChangeListener!!.onTodayHeartRateData(watchHeartRateBean)
                }
            }

        }

        // 添加监听器
        FissionSdkBleManage.getInstance().addCmdResultListener(fissionBigDataCmdResultListener)
        FissionSdkBleManage.getInstance().getHeartRateRecord(startTime, endTime)

        try {
            // 等待异步结果返回，但最多等待一定的时间（例如 5 秒）
            latch.await(TASK_TIME_OUT, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // 处理等待超时异常
            e.printStackTrace()
        }
        FissionSdkBleManage.getInstance().removeCmdResultListener(fissionBigDataCmdResultListener)
    }

    override fun queryHistoryBloodOxygen() {
        TODO("Not yet implemented")
    }

    override fun queryHistoryHeartRateData() {
        TODO("Not yet implemented")
    }

    override fun queryHistorySleepData() {
        TODO("Not yet implemented")
    }

    override fun queryHistorySports() {
        TODO("Not yet implemented")
    }

    override fun queryHistoryStepsData() {
        TODO("Not yet implemented")
    }

    override fun querySedentaryInfo(): WatchSedentaryBean {
        TODO("Not yet implemented")
    }

    override fun querySelectedDialInfo(): WatchDialBean {
        TODO("Not yet implemented")
    }

    override fun queryTemperatureFormat(): TemperatureFormat {
        TODO("Not yet implemented")
    }

    override fun queryTimeFormat(): TimeFormat {
        TODO("Not yet implemented")
    }

    override fun queryTodaySleepData() {
        TODO("Not yet implemented")
    }

    override fun queryTodayStepsData() {
        TODO("Not yet implemented")
    }

    override fun queryWatchContactsNumber(): Int {
        TODO("Not yet implemented")
    }

    override fun queryWatchDialLayout(): WatchDialLayoutBean {
        TODO("Not yet implemented")
    }

    override fun send24hHeartRateSwitchState(enable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun sendAbortDialBinFile() {
        TODO("Not yet implemented")
    }

    override fun sendAbortFirmwareUpgrade() {
        TODO("Not yet implemented")
    }

    override fun sendAbortTransCustomDial() {
        TODO("Not yet implemented")
    }

    override fun sendAlarmsInfo(vararg alarm: WatchAlarmBean) {
        val fissionAlarms: MutableList<FissionAlarm> = mutableListOf()
        for(watchAlarm in alarm){
            val fissionAlarm = watchAlarm.date?.let {
                FissionAlarm(watchAlarm.id,1,watchAlarm.enable, it.time,watchAlarm.repeatMode, "")
            }
            if (fissionAlarm != null) {
                fissionAlarms.add(fissionAlarm)
            }
        }
        FissionSdkBleManage.getInstance().setAlarmInfos(fissionAlarms)
        FissionLogUtils.d("cy_sdk", "----sendAlarmsInfo---$fissionAlarms")
    }

    override fun sendBrightScreenPeriod(period: WatchBrightScreenPeriodBean) {
        TODO("Not yet implemented")
    }

    override fun sendBrightScreenSwitchState(enable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun sendBrightScreenTime(watchBrightScreenTime: Int) {
        TODO("Not yet implemented")
    }

    override fun sendCallInfo(callEntity: WatchCallInfoBean) {
        TODO("Not yet implemented")
    }

    override fun sendCustomWatchDialFile(
        watchDialLayoutBean: WatchDialLayoutBean,
        imagePath: String,
        onTransListener: OnWatchFaceTransListener
    ) {
        TODO("Not yet implemented")
    }

    override fun sendDialBinFile(file: File, listener: OnWatchFaceTransListener) {
        TODO("Not yet implemented")
    }

    override fun sendDoNotDisturbInfo(watchDoNotDisturbBean: WatchDoNotDisturbBean) {
        TODO("Not yet implemented")
    }

    override fun sendDrinkWaterInfo(bean: WatchDrinkWaterBean) {
        TODO("Not yet implemented")
    }

    override fun sendFutureWeatherInfo(futureWeatherBeans: List<WatchFutureWeatherBean>) {
        TODO("Not yet implemented")
    }

    override fun sendMediaControlAction(action: Int) {
        TODO("Not yet implemented")
    }

    override fun sendPhoneVolume(action: VolumeAction, volumePrecent: Int) {
        TODO("Not yet implemented")
    }

    override fun sendPushMessageInfo(messageEntity: WatchPushMessageBean) {
        TODO("Not yet implemented")
    }

    override fun sendSedentaryInfo(bean: WatchSedentaryBean) {
        TODO("Not yet implemented")
    }

    override fun sendSelectedDialInfo(selectDialBean: WatchDialBean) {
        TODO("Not yet implemented")
    }

    override fun sendStartFirmwareUpgrade() {
        TODO("Not yet implemented")
    }

    override fun sendStepGoal(goal: Int) {
        TODO("Not yet implemented")
    }

    override fun sendTemperatureFormat(format: TemperatureFormat) {
        TODO("Not yet implemented")
    }

    override fun sendTimeFormat(format: TimeFormat) {
        TODO("Not yet implemented")
    }

    override fun sendTodayWeatherInfo(todayWeatherBean: WatchTodayWeatherBean) {
        TODO("Not yet implemented")
    }

    override fun sendUserInfo(weight: Float, height: Int, gender: Int, age: Int) {
        TODO("Not yet implemented")
    }

    override fun sendWatchContacts(contactList: List<WatchContactBean>) {
        TODO("Not yet implemented")
    }

    override fun sendWatchEnterCamera(isOpenCamera: Boolean) {
        TODO("Not yet implemented")
    }

    override fun sendWatchExitFindPhone() {
        TODO("Not yet implemented")
    }

    override fun setAlarmChangeListener(listener: OnAlarmChangedListener?) {
        TODO("Not yet implemented")
    }

    override fun setBloodOxygenChangeListener(bloodOxygenChangeListener: OnBloodOxygenChangeListener?) {
        TODO("Not yet implemented")
    }

    override fun setContactListener(listener: OnContactsListener?) {
        TODO("Not yet implemented")
    }

    override fun setOnCallOperationListener(onCallOperationListener: OnCallOperationListener) {
        TODO("Not yet implemented")
    }

    override fun setOnConnectionStateListener(listener: OnConnectionStateListener?) {
        mleConnectListener = object : BleConnectListener {
            override fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
                LogUtils.d("wl", "FissionSdk_v2----onConnectionStateChange: $newState")
                when (newState) {
                    RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                        listener?.onStateChanged(OnConnectionStateListener.ConnectState.STATE_DISCONNECTED)
                    }
                    RxBleConnection.RxBleConnectionState.CONNECTING -> {
                        listener?.onStateChanged(OnConnectionStateListener.ConnectState.STATE_CONNECTING)
                    }else ->{

                    }
                }
            }

            override fun isBindNewDevice() {
            }

            override fun onBinding() {
                LogUtils.d("wl", "---onBinding--")

            }

            override fun onBindSucceeded(address: String, name: String) {
                LogUtils.d("wl", "---onBindSucceeded--")
                listener?.onStateChanged(OnConnectionStateListener.ConnectState.STATE_CONNECTED)
            }

            override fun onBindFailed(code: Int) {
                LogUtils.d("wl", "---onBindFailed--")
                if (code == FissionConstant.BIND_FAIL_KEY_ERROR) { //绑定秘钥出错，重置秘钥
                    SPUtils.getInstance().put(BIND_KEY, "")
                }
            }

            override fun onConnectionFailure(throwable: Throwable) {
            }

            override fun onServiceDisconnected() {
                listener?.onStateChanged(OnConnectionStateListener.ConnectState.STATE_DISCONNECTED)
            }
        }

    }

    override fun setOnFirmwareUpgradeListener(listener: OnFirmwareUpgradeListener?) {
        TODO("Not yet implemented")
    }

    override fun setOnHeartRateChangeListener(onHeartRateChangeListener: OnHeartRateChangeListener?) {
        this.onHeartRateChangeListener = onHeartRateChangeListener
        FissionSdkBleManage.getInstance().addCmdResultListener(object : FissionBigDataCmdResultListener(){
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

        })
    }

    override fun setOnMediaControlActionListener(listener: OnMediaControlActionListener?) {
        TODO("Not yet implemented")
    }

    override fun setOnWatchBatteryChangeListener(listener: OnWatchBatteryChangeListener?) {
        this.onWatchBatteryChangeListener = listener
        FissionSdkBleManage.getInstance().addCmdResultListener(object : FissionAtCmdResultListener(){
            override fun sendSuccess(cmdId: String?) {
            }

            override fun sendFail(cmdId: String?) {
            }

            override fun onResultTimeout(cmdId: String?) {
            }

            override fun onResultError(errorMsg: String?) {
            }

            override fun fssSuccess(fssStatus: FssStatus?) {
                super.fssSuccess(fssStatus)
                if (fssStatus != null) {
                    when(fssStatus.fssType){
                        FissionEnum.SC_CUR_BATTERY_PERCENT -> onWatchBatteryChangeListener?.onBatteryChanged(fssStatus.fssStatus)
                    }
                }
            }

        })
    }

    override fun setOnWatchCameraListener(onWatchCameraListener: OnWatchCameraListener?) {
        TODO("Not yet implemented")
    }

    override fun setOnWatchStepChangeListener(listener: OnWatchStepChangeListener?) {
        TODO("Not yet implemented")
    }

    override fun setSleepChangeListener(listener: OnSleepChangeListener?) {
        TODO("Not yet implemented")
    }

    override fun setSportDataListener(sportDataListener: OnSportDataListener?) {
        TODO("Not yet implemented")
    }

    override fun setWatchDialSwitchListener(onDialSwitchLisenter: OnWatchDialSwitchListener) {
        TODO("Not yet implemented")
    }

    override fun setWatchFindPhoneListener(listener: OnWatchFindPhoneListener?) {
        TODO("Not yet implemented")
    }

    override fun setWatchStepsDataListener(stepChangeListener: OnWatchStepsDataListener?) {
        TODO("Not yet implemented")
    }

    override fun startMeasureBloodOxygen() {
        TODO("Not yet implemented")
    }

    override fun stopMeasureBloodOxygen() {
        TODO("Not yet implemented")
    }

    override fun syncLanguage() {
        TODO("Not yet implemented")
    }

    override fun syncTime() {
        TODO("Not yet implemented")
    }

    private fun getMidnightTimestamp(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    fun fillHeartRateValues(
        timeArray: List<Long>, // 时间数组，以毫秒为单位的时间戳
        heartRateArray: List<Int>, // 心率数组
        newArraySize: Int, // 新数组的大小
        date: Date
    ): IntArray {
        require(heartRateArray.size == timeArray.size) { "心率列表和时间戳列表必须具有相同的大小" }

        val timeHeartRateMap = timeArray.zip(heartRateArray).toMap() // 将时间戳与心率对应起来

        val interval = 24*3600 / newArraySize

        val startTime = getMidnightTimestamp(date) / 1000

        val endTime = startTime + 24*3600 -1

        val filledHeartRates = mutableListOf<Int>()
        var currentTime = startTime
        while (currentTime <= endTime) {
            val heartRate = timeHeartRateMap[currentTime] ?: 0 // 如果心率缺失，则设置为默认值0
            filledHeartRates.add(heartRate)
            currentTime += interval
        }

        return filledHeartRates.toIntArray()
    }

}