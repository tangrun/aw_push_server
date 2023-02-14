package cn.wildfirechat.push.android.hms;


import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.PushMessageType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.huawei.push.android.*;
import com.huawei.push.exception.HuaweiMesssagingException;
import com.huawei.push.message.AndroidConfig;
import com.huawei.push.message.Message;
import com.huawei.push.message.Notification;
import com.huawei.push.messaging.HuaweiApp;
import com.huawei.push.messaging.HuaweiCredential;
import com.huawei.push.messaging.HuaweiMessaging;
import com.huawei.push.messaging.HuaweiOption;
import com.huawei.push.model.Importance;
import com.huawei.push.model.Urgency;
import com.huawei.push.model.Visibility;
import com.huawei.push.reponse.SendResponse;
import com.huawei.push.util.InitAppUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;

@Component
public class HMSPush {
    private static final Logger LOG = LoggerFactory.getLogger(HMSPush.class);
    private static final String tokenUrl = "https://login.vmall.com/oauth2/token"; //获取认证Token的URL
    private static final String apiUrl = "https://api.push.hicloud.com/pushsend.do"; //应用级消息下发API
    private String accessToken;//下发通知消息的认证Token
    private long tokenExpiredTime;  //accessToken的过期时间

    @Autowired
    private HMSConfig mConfig;

    private HuaweiApp huaweiApp;

    @PostConstruct
    private void init() {
        HuaweiCredential credential = HuaweiCredential.builder()
                .setAppId(mConfig.getAppId())
                .setAppSecret(mConfig.getAppSecret())
                .build();

        // Create HuaweiOption
        HuaweiOption option = HuaweiOption.builder()
                .setCredential(credential)
                .build();

        // Initialize HuaweiApp
        huaweiApp = HuaweiApp.getInstance(option);
    }

    //发送Push消息
    public void push_aw_chat_v1(PushMessage pushMessage) {
        if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_RECALLED || pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_DELETED) {
            //Todo not implement
            //撤回或者删除消息，需要更新远程通知，暂未实现
            return;
        }

        try {
            HuaweiMessaging huaweiMessaging = HuaweiMessaging.getInstance(huaweiApp);

            // 指示灯控制
            LightSettings lightSettings = LightSettings.builder()
                    .setColor(Color.builder().setAlpha(0f).setRed(0f).setBlue(1f).setGreen(1f).build())
                    .setLightOnDuration("3.5")
                    .setLightOffDuration("5S")
                    .build();

            AndroidNotification androidNotification = AndroidNotification.builder()
                    //.setIcon("/raw/ic_launcher2")
                    //.setColor("#AACCDD")
                    //.setSound("/raw/call")//自定义铃声
                    .setDefaultSound(true)
                    //.setTag("tagBoom")
                    .setClickAction(ClickAction.builder().setType(3).setIntent("cn.cdblue.awchat.app.main.MainActivity").build())
                    //.setBodyLocKey("key2")
                    //.addBodyLocArgs("boy").addBodyLocArgs("dog")
                    //.setTitleLocKey("key1")
                    //.addTitleLocArgs("Girl").addTitleLocArgs("Cat")
                    .setChannelId("message")//渠道ID
                    //.setNotifySummary("some summary")//通知摘要
                    //.setMultiLangkey(multiLangKey)//多语言
                    //.setStyle(1)
                    //.setBigTitle("Big Boom Title")
                    //.setBigBody("Big Boom Body")
                    //.setAutoClear(86400000)
                    .setNotifyId((int)(System.currentTimeMillis()/1000))
                    .setGroup(pushMessage.sender)
                    .setImportance(Importance.NORMAL.getValue())
                    .setLightSettings(lightSettings)
                    .setBadge(BadgeNotification.builder().setAddNum(1).setBadgeClass("cn.cdblue.awchat.app.main.SplashActivity").build())
                    .setVisibility(Visibility.PUBLIC.getValue())
                    .setForegroundShow(true)
                    //.addInboxContent("content1").addInboxContent("content2").addInboxContent("content3").addInboxContent("content4").addInboxContent("content5")
                    //.addButton(Button.builder().setName("button1").setActionType(0).build())
                    //.addButton(Button.builder().setName("button2").setActionType(1).setIntentType(0).setIntent("https://com.huawei.hms.hmsdemo/deeplink").build())
                    //.addButton(Button.builder().setName("button3").setActionType(4).setData("your share link").build())
                    .build();

            AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder().setCollapseKey(-1)
                    .setUrgency(Urgency.HIGH.getValue())
                    .setTtl("10000s")
                    .setBiTag("the_sample_bi_tag_for_receipt_service")
                    .setCategory("IM")
                    .setNotification(androidNotification);

            // 通知栏消息
            String title,content;
            if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_FRIEND_REQUEST) {
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "好友请求";
                } else {
                    title = pushMessage.senderName + " 请求加您为好友";
                }
                content = pushMessage.pushContent;
            }else if(pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_NORMAL){
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "消息";
                } else {
                    title = pushMessage.senderName;
                }
                content = pushMessage.pushContent;
            } else {
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "消息";
                } else {
                    title = pushMessage.senderName;
                }
                content = pushMessage.unReceivedMsg==2?"邀请您语音通话":"邀请您视频通话";
                androidNotification = AndroidNotification.builder()
                        .setSound("/raw/call")
                        .setDefaultSound(false)
                        .setClickAction(ClickAction.builder().setType(3).setIntent("cn.cdblue.awchat.app.main.MainActivity").build())
                        .setChannelId("ChatNotificationHelper")//渠道ID
                        .setNotifyId((int)(System.currentTimeMillis()/1000))
                        .setGroup(pushMessage.sender)
                        .setImportance(Importance.NORMAL.getValue())
                        .setLightSettings(lightSettings)
                        .setBadge(BadgeNotification.builder().setAddNum(1).setBadgeClass("cn.cdblue.awchat.app.main.SplashActivity").build())
                        .setVisibility(Visibility.PUBLIC.getValue())
                        .setForegroundShow(true)
                        .build();
                androidConfigBuilder
                          .setCategory("VOIP");
            }

            // 通知内容
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(content)
                    .build();

            //JSONObject multiLangKey = new JSONObject();
            //JSONObject titleKey = new JSONObject();
            //titleKey.put("en", "好友请求");
            //JSONObject bodyKey = new JSONObject();
            //bodyKey.put("en", "My name is %s, I am from %s.");
            //multiLangKey.put("key1", titleKey);
            //multiLangKey.put("key2", bodyKey);


            AndroidConfig androidConfig = androidConfigBuilder
                    .build();

            Message message = Message.builder().setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .addToken(pushMessage.deviceToken)
                    .build();

            SendResponse response = huaweiMessaging.sendMessage(message);
            LOG.info("HMS推送 token：{} 结果：{}", pushMessage.deviceToken, response.getMsg());
        }catch (HuaweiMesssagingException e){
            e.printStackTrace();
            LOG.info("HMS推送异常：", e);
        }
    }

    //发送Push消息
    //旧方式 官方已废弃（不推荐使用）
    public void push_aw_chat(PushMessage pushMessage) {
        if (tokenExpiredTime <= System.currentTimeMillis()) {
            try {
                refreshToken();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*PushManager.requestToken为客户端申请token的方法，可以调用多次以防止申请token失败*/
        /*PushToken不支持手动编写，需使用客户端的onToken方法获取*/
        JSONArray deviceTokens = new JSONArray();//目标设备Token
        deviceTokens.add(pushMessage.getDeviceToken());

        boolean isTransparentMessage = false;//是否发送透传消息
        String token = pushMessage.getDeviceToken();
        JSONObject payload = new JSONObject();
        if(isTransparentMessage) {
            // 透传消息
            JSONObject msg = new JSONObject();
            msg.put("type", 1);//3: 通知栏消息，异步透传消息请根据接口文档设置
            pushMessage.deviceToken = null;
            msg.put("body", new Gson().toJson(pushMessage));//通知栏消息body内容

            JSONObject hps = new JSONObject();//华为PUSH消息总结构体
            hps.put("msg", msg);

            payload.put("hps", hps);
        }else {
            // 通知栏消息
            String title,content;
            if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_FRIEND_REQUEST) {
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "好友请求";
                } else {
                    title = pushMessage.senderName + " 请求加您为好友";
                }
                content = pushMessage.pushContent;
            }else if(pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_NORMAL){
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "消息";
                } else {
                    title = pushMessage.senderName;
                }
                content = pushMessage.pushContent;
            } else {
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "消息";
                } else {
                    title = pushMessage.senderName;
                }
                content = pushMessage.unReceivedMsg==2?"邀请您语音通话":"邀请您视频通话";
            }

            JSONObject body = new JSONObject();
            body.put("title",title);
            body.put("content",content);

            JSONObject action = new JSONObject();
            action.put("type",3);
            JSONObject param = new JSONObject();
            param.put("appPkgName",pushMessage.getPackageName());
            action.put("param",param);

            JSONObject msg = new JSONObject();
            msg.put("type", 3);//3: 通知栏消息，异步透传消息请根据接口文档设置
            pushMessage.deviceToken = null;
            msg.put("body", body);//通知栏消息body内容
            msg.put("action", action);

            JSONObject ext = new JSONObject();
            msg.put("badgeAddNum", 1);
            msg.put("badgeClass", "cn.cdblue.awchat.app.main.MainActivity");

            JSONObject hps = new JSONObject();//华为PUSH消息总结构体
            hps.put("msg", msg);
            hps.put("ext", ext);

            payload.put("hps", hps);
        }


        LOG.info("send push to HMS {}", payload);

        try {
            String postBody = MessageFormat.format(
                    "access_token={0}&nsp_svc={1}&nsp_ts={2}&device_token_list={3}&payload={4}",
                    URLEncoder.encode(accessToken,"UTF-8"),
                    URLEncoder.encode("openpush.message.api.send","UTF-8"),
                    URLEncoder.encode(String.valueOf(System.currentTimeMillis() / 1000),"UTF-8"),
                    URLEncoder.encode(deviceTokens.toString(),"UTF-8"),
                    URLEncoder.encode(payload.toString(),"UTF-8"));

            String postUrl = apiUrl + "?nsp_ctx=" + URLEncoder.encode("{\"ver\":\"1\", \"appId\":\"" + mConfig.getAppId() + "\"}", "UTF-8");
            //String postUrl = apiUrl + "?nsp_ctx=" + URLEncoder.encode("{\"ver\":\"1\", \"appId\":\"104774635\"}", "UTF-8");
            String response = httpPost(postUrl, postBody, 5000, 5000);
            LOG.info("Push to {} response {}", token, response);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.info("Push to {} with exception", token, e);
        }
    }

    //获取下发通知消息的认证Token
    private void refreshToken() throws IOException {
        LOG.info("hms refresh token");
        String msgBody = MessageFormat.format(
                "grant_type=client_credentials&client_secret={0}&client_id={1}",
                URLEncoder.encode(mConfig.getAppSecret(), "UTF-8"), mConfig.getAppId());
        String response = httpPost(tokenUrl, msgBody, 5000, 5000);
        JSONObject obj = JSONObject.parseObject(response);
        accessToken = obj.getString("access_token");
        tokenExpiredTime = System.currentTimeMillis() + obj.getLong("expires_in") - 5 * 60 * 1000;
        LOG.info("hms refresh token with result {}", response);
    }

    //发送Push消息
    public void push(PushMessage pushMessage) {
        if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_RECALLED || pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_DELETED) {
            //Todo not implement
            //撤回或者删除消息，需要更新远程通知，暂未实现
            return;
        }

        if (tokenExpiredTime <= System.currentTimeMillis()) {
            try {
                refreshToken();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*PushManager.requestToken为客户端申请token的方法，可以调用多次以防止申请token失败*/
        /*PushToken不支持手动编写，需使用客户端的onToken方法获取*/
        JSONArray deviceTokens = new JSONArray();//目标设备Token
        deviceTokens.add(pushMessage.getDeviceToken());


        JSONObject msg = new JSONObject();
        msg.put("type", 1);//3: 通知栏消息，异步透传消息请根据接口文档设置
        String token = pushMessage.getDeviceToken();
        pushMessage.deviceToken = null;
        msg.put("body", new Gson().toJson(pushMessage));//通知栏消息body内容

        JSONObject hps = new JSONObject();//华为PUSH消息总结构体
        hps.put("msg", msg);

        JSONObject payload = new JSONObject();
        payload.put("hps", hps);

        LOG.info("send push to HMS {}", payload);

        try {
            String postBody = MessageFormat.format(
                    "access_token={0}&nsp_svc={1}&nsp_ts={2}&device_token_list={3}&payload={4}",
                    URLEncoder.encode(accessToken, "UTF-8"),
                    URLEncoder.encode("openpush.message.api.send", "UTF-8"),
                    URLEncoder.encode(String.valueOf(System.currentTimeMillis() / 1000), "UTF-8"),
                    URLEncoder.encode(deviceTokens.toString(), "UTF-8"),
                    URLEncoder.encode(payload.toString(), "UTF-8"));

            String postUrl = apiUrl + "?nsp_ctx=" + URLEncoder.encode("{\"ver\":\"1\", \"appId\":\"" + mConfig.getAppId() + "\"}", "UTF-8");
            String response = httpPost(postUrl, postBody, 5000, 5000);
            LOG.info("Push to {} response {}", token, response);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.info("Push to {} with exception", token, e);
        }
    }

    public String httpPost(String httpUrl, String data, int connectTimeout, int readTimeout) throws IOException {
        OutputStream outPut = null;
        HttpURLConnection urlConnection = null;
        InputStream in = null;

        try {
            URL url = new URL(httpUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.connect();

            // POST data
            outPut = urlConnection.getOutputStream();
            outPut.write(data.getBytes("UTF-8"));
            outPut.flush();

            // read response
            if (urlConnection.getResponseCode() < 400) {
                in = urlConnection.getInputStream();
            } else {
                in = urlConnection.getErrorStream();
            }

            List<String> lines = IOUtils.readLines(in, urlConnection.getContentEncoding());
            StringBuffer strBuf = new StringBuffer();
            for (String line : lines) {
                strBuf.append(line);
            }
            LOG.info(strBuf.toString());
            return strBuf.toString();
        } finally {
            IOUtils.closeQuietly(outPut);
            IOUtils.closeQuietly(in);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
