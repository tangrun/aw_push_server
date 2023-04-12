package cn.wildfirechat.push.android.honor;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.PushMessageType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.huawei.push.android.AndroidNotification;
import com.huawei.push.android.BadgeNotification;
import com.huawei.push.android.ClickAction;
import com.huawei.push.model.Importance;
import com.huawei.push.model.Visibility;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Rain
 * @date 2023/4/12 11:48
 */
@Service
public class HonorPush {
    private static final Logger log = LoggerFactory.getLogger(HonorPush.class);
    @Autowired
    HonorConfig config;

    private String access_token, token_type;
    private long expires_in, expires_base_time;

    public void push(PushMessage pushMessage) {
        if (expires_in - expires_base_time <= 0) {
            requestToken();
        }
        requestSendMessage(pushMessage);
    }


    private void requestSendMessage(PushMessage pushMessage) {
        if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_RECALLED || pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_DELETED) {
            //Todo not implement
            //撤回或者删除消息，需要更新远程通知，暂未实现
            return;
        }

        // 通知栏消息
        String title, content;
        if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_FRIEND_REQUEST) {
            if (StringUtils.isEmpty(pushMessage.senderName)) {
                title = "好友请求";
            } else {
                title = pushMessage.senderName + " 请求加您为好友";
            }
            content = pushMessage.pushContent;
        } else if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_NORMAL) {
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
            content = pushMessage.unReceivedMsg == 2 ? "邀请您语音通话" : "邀请您视频通话";
        }

        try {


            CloseableHttpClient httpClient = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost(String.format("https://push-api.cloud.hihonor.com/api/v1/%s/sendMessage", config.getAppId()));
            httpPost.setHeader("Authorization", String.format("%s %s", token_type, access_token));
            httpPost.setHeader("timestamp", System.currentTimeMillis() + "");

            JSONObject jsonObject = new JSONObject();
            {
//             JSONObject notification = new JSONObject();
//             notification.put("title",title);
//             notification.put("body",content);
//             notification.put("image","");
//             jsonObject.put("notification",notification);

                JSONArray tokenList = new JSONArray();
                tokenList.add(pushMessage.getDeviceToken());
                jsonObject.put("token", tokenList);

                JSONObject android = new JSONObject();
                jsonObject.put("android", android);
                {
                    JSONObject notification = new JSONObject();
                    android.put("notification", notification);
                    {
                        notification.put("title", title);
                        notification.put("body", content);
                        JSONObject clickAction = new JSONObject();
                        notification.put("clickAction", clickAction);
                        {
                            clickAction.put("type", 3);
                        }
                        notification.put("importance", "NORMAL");
                        notification.put("notifyId", pushMessage.messageId);
                        JSONObject badge = new JSONObject();
                        notification.put("badge",badge);
                        {
                         badge.put("addNum",1);
                        }
                    }
                }
            }
            httpPost.setEntity(new StringEntity(jsonObject.toJSONString(), ContentType.APPLICATION_JSON));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String s = EntityUtils.toString(response.getEntity());
                log.info("honor推送 response: {}", s);
            } else {
                log.error("honor推送 status code: {}", response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            log.error("honor推送 error", e);
        }
    }

    private void requestToken() {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost("https://iam.developer.hihonor.com/auth/realms/developer/protocol/openid-connect/token");
            httpPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                    new BasicNameValuePair("grant_type", "client_credentials"),
                    new BasicNameValuePair("client_id", config.getClientId()),
                    new BasicNameValuePair("client_secret", config.getClientSecret())
            ), StandardCharsets.UTF_8));

            CloseableHttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String s = EntityUtils.toString(response.getEntity());
                log.info("获取鉴权接口 response: {}", s);
                JSONObject jsonObject = JSON.parseObject(s);
                access_token = jsonObject.getString("access_token");
                token_type = jsonObject.getString("token_type");
                expires_in = jsonObject.getLong("expires_in");
                expires_base_time = System.currentTimeMillis();
            } else {
                log.error("获取鉴权接口 status code: {}", response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取鉴权接口", e);
        }
    }


}

