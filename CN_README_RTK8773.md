# Fission-SDK-Android集成文档 (RTK8773兼容)
### 一、产品介绍
* 该文档用于集成支持RTK8773芯片的智能穿戴设备。
* 接入流程参考CN_README文档。本文档主要描述差异化问题。

### 二、快速入门
#### 1，导入SDK
将**fissionsdk_v2-release-vx.x.x.aar**、**rtk-bbpro-core-x.x.x.jar**、**rtk-core-x.x.x.jar**、**rtk-dfu-x.x.x.jar** 导入工程，一般复制到libs目录下，然后在module中的build.gradle中如下设置：
```
repositories {
    flatDir {
        dirs  file('libs')
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation files('libs/rtk-bbpro-core-1.6.9.jar')
    implementation files('libs/rtk-core-1.2.8.jar')
    implementation files('libs/rtk-dfu-3.4.1.jar')
    implementation(name: 'fissionsdk_v2-release', ext: 'aar')
    api rootProject.ext.dependencies["rx-java2"]
    api rootProject.ext.dependencies["rx-android2"]
}
```
PS：如果需要集成ChatGpt AI功能， 将**rtk-audioconnect-ai-x.x.x.aar**、**rtk-media-codec-x.x.x.jar**、**rtk-wear-x.x.x.jar** 导入工程，一般复制到libs目录下.
```
    dependencies {
        implementation fileTree(include: ['*.jar'], dir: 'libs')
        
        implementation files('libs/rtk-bbpro-core-1.8.15.jar')
        implementation files('libs/rtk-core-1.5.25.jar')
        implementation files('libs/rtk-dfu-3.10.13.jar')
        implementation files("libs/rtk-wear-1.5.12.jar")
        implementation files("libs/rtk-media-codec-1.2.1.aar")
        implementation files("libs/rtk-audioconnect-ai-1.0.1.aar")
    
        // Microsoft SpeechService SDK
        implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.33.0")
    }
```

#### 2， 差异化部分
App在和手表连接成功之后，需要先交换双方配置项。主要包含以下配置项：
1. 获取固件信息， 连接成功之后，优先获取手表固件信息。部分配置项可能和固件版本，型号有关。
```    
   /**
     *  获取固件信息
     */
    public void getHardwareInfo(HardWareInfo hardWareInfo){

    }

    FissionSdkBleManage.getInstance().getHardwareInfo();
```
FissionSdkBleManage.getInstance().getHardwareInfo() 这是发送获取固件信息的指令方法。getHardwareInfo(HardWareInfo hardWareInfo)这是设备执行指令成功后，返回数据给App的回调方法。具体使用方式请参考demo。HardWareInfo包含硬件版本，固件版本，协议版本，适配号，蓝牙地址，蓝牙名称等等信息，主要用于功能模块适配和以后固件升级时的版本判断。

PS: 调用这个方法之后，可以获取到智能穿戴设备的芯片信息。
```
       if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK){
            // 代表设备芯片类型是非rtk8773. (包含8762/8763)
        }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
            // 代表设备芯片类型是rtk8773
        }
```
 设置自定义表盘功能。 因为rtk8773表盘打包协议不同，因此需要区分打包。示例代码如下：
```
        if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK){
            resultData = com.fission.wear.sdk.v2.utils.FissionDialUtil.getDiaInfoBinData(this, dialModel);
        }else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
            resultData = RtkDialUtil.getInstance().getSimpleDialBinFile(this, dialModel);
        }
```

