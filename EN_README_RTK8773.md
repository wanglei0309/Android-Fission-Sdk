# Fission-SDK-Android Integration Document (RTK8773 compatible)
### 1. Product Introduction
* This document is used to integrate smart wearable devices that support RTK8773 chips.
* For the access process, refer to the CN_README document. This document mainly describes the differentiation issues.

### 2. Quick Start
#### 1. Import SDK
Import **fissionsdk_v2-release-vx.x.x.aar**, **rtk-bbpro-core-x.x.x.jar**, **rtk-core-x.x.x.jar**, **rtk-dfu-x.x.x.jar** into the project, usually copy them to the libs directory, and then set them as follows in the build.gradle in the module:
```
repositories {
flatDir {
dirs file('libs')
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
PS: If you need to integrate ChatGpt AI function, import **rtk-audioconnect-ai-x.x.x.aar**, **rtk-media-codec-x.x.x.jar**, **rtk-wear-x.x.x.jar** into the project, usually copy them to the libs directory.
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

#### 2. Differentiation
After the App successfully connects to the watch, it needs to exchange configuration items of both parties. Mainly includes the following configuration items:
1. Get firmware information. After the connection is successful, get the watch firmware information first. Some configuration items may be related to the firmware version and model.
```
/**
* Get firmware information
*/
public void getHardwareInfo(HardWareInfo hardWareInfo){

}

FissionSdkBleManage.getInstance().getHardwareInfo();
```
FissionSdkBleManage.getInstance().getHardwareInfo() This is the command method for sending instructions to obtain firmware information. getHardwareInfo(HardWareInfo hardWareInfo) This is the callback method that returns data to the App after the device successfully executes the command. Please refer to the demo for specific usage. HardWareInfo contains information such as hardware version, firmware version, protocol version, adapter number, Bluetooth address, Bluetooth name, etc., which is mainly used for function module adaptation and version judgment during future firmware upgrades.

PS: After calling this method, you can get the chip information of the smart wearable device.
```
if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK){
// Indicates that the device chip type is not rtk8773. (including 8762/8763)
}else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
// Indicates that the device chip type is rtk8773
}
```
Set the custom dial function. Because the rtk8773 dial packaging protocol is different, it is necessary to distinguish the packaging. The sample code is as follows:
```
if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK){
resultData = com.fission.wear.sdk.v2.utils.FissionDialUtil.getDiaInfoBinData(this, dialModel);
}else if(SPUtils.getInstance().getInt(SpKey.CHIP_CHANNEL_TYPE) == HardWareInfo.CHANNEL_TYPE_RTK8773){
resultData = RtkDialUtil.getInstance().getSimpleDialBinFile(this, dialModel);
}
```

#### 3. AI Function Access Guide
There are currently two main AI functions: 1. AI chat 2. AI dial
The AI function needs to be initialized first.
```
//After the connection is successful, initialization is performed in the callback result of the getHardwareInfo function.

//Ai chat voice channel initialization
RtkChatGptManage.getInstance().init(mContext);

//gpt service channel initialization,  
WatchInfo[] watchInfos = new WatchInfo[1];
WatchInfo watchInfo = new WatchInfo(PaymentModel.LICENSE_PAY, mac, LicenseModel.KNOWN_DEVICE, MemberModel.FREE, "", "", "640*640", "512*512", language, language, 0, 0, 0, 0);
watchInfos[0] = watchInfo;
AFlashChatGptUtils.getInstance().initSdk(context, StringUtils.getString(R.string.app_name), watchInfos);
```

1. AI chat
Ai chat function sample code
```
AFlashChatGptUtils.getInstance().setGptAiVoiceListener(new AFlashChatGptUtils.GptAiVoiceListener() {
           @Override
           public void onChat(String question, String answer) {
              //Send the results of AI chat to the watch
              FissionSdkBleManage.getInstance().sendJsiCmdByChat(answer, JsiCmd.XIAO_DU_AI, JsiCmd.SEND_ANSWER, true);
           }

           @Override
           public void onCreateDial(List<String> imgPaths) {

           }

           @Override
           public void onSpeechResult(String result, String type) {
              //Send the result of voice recognition to the watch
              if (StringUtils.equals(AFlashChatGptUtils.AI_VOICE_TYPE_CHAT, type)) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat(result, JsiCmd.XIAO_DU_AI, JsiCmd.SEND_QUESTION, true);
              }

           }

           @Override
           public void onError(int code, String msg) {

           }
       });
```

2. AI dial
AI dial function sample code
```
AFlashChatGptUtils.getInstance().setGptAiVoiceListener(new AFlashChatGptUtils.GptAiVoiceListener() {
           @Override
           public void onChat(String question, String answer) {
              //Send the results of AI chat to the watch
              FissionSdkBleManage.getInstance().sendJsiCmdByChat(answer, JsiCmd.XIAO_DU_AI, JsiCmd.SEND_ANSWER, true);
           }

           @Override
           public void onCreateDial(List<String> imgPaths) {

              // This is the dial background image generated by Ai,  Customers need to write their own logic, package it into a watch face, and push it to the watch. Watch face packaging logic reference album watch face

              // FissionSdkBleManage.getInstance().startDial(outData, FissionEnum.WRITE_AI_DIAL_DATA);

           }

           @Override
           public void onSpeechResult(String result, String type) {
              //Send the result of voice recognition to the watch
              if (StringUtils.equals(AFlashChatGptUtils.AI_VOICE_TYPE_DIAL, type)) {
                FissionSdkBleManage.getInstance().sendJsiCmdByChat(result, JsiCmd.XIAO_DU_AI, JsiCmd.SEND_QUESTION, true);
              }else{
                // Return the speech recognition result of the AI ​​watch face, and the customer can process it according to their needs
              }

           }

           @Override
           public void onError(int code, String msg) {

           }
       });

        // The AI ​​watch face function requires customers to record in the App and then convert the data into a byte array. Recording format support: pcm
        AFlashChatGptUtils.getInstance().voiceDrawing(fileBytes);
```
