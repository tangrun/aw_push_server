package cn.wildfirechat.push.android.vivo;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.PushMessageType;
import com.vivo.push.sdk.notofication.Message;
import com.vivo.push.sdk.notofication.Result;
import com.vivo.push.sdk.server.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class VivoPush {
    private static final Logger LOG = LoggerFactory.getLogger(VivoPush.class);
    private long tokenExpiredTime;

    @Autowired
    VivoConfig mConfig;

    private String authToken;

    public void push_aw_chat(PushMessage pushMessage) {
        if (tokenExpiredTime <= System.currentTimeMillis()) {
            refreshToken();
        }

        Result resultMessage = null;
        try {
            if (pushMessage.isHiddenDetail) {
                pushMessage.pushContent = "您收到一条新消息";
            }
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
            Sender senderMessage = new Sender(mConfig.getAppSecret());
            senderMessage.setAuthToken(authToken);
            Message.Builder builder = new Message.Builder()
                    .regId(pushMessage.getDeviceToken())//该测试手机设备订阅推送后生成的regId 
                    .notifyType(4)//通知类型 1:无，2:响铃，3:振动，4:响铃和振动
                    .title(title)
                    .content(content)
                    .classification(1)//消息类型 0：运营类消息，1：系统类消息。不填默认为0
                    .timeToLive(1000)
                    .skipType(1)//点击跳转类型 1：打开APP首页 2：打开链接 3：自定义 4:打开app内指定页面
                    .networkType(-1)//网络方式 -1：不限，1：wifi下发送，不填默认为-1
                    .requestId(System.currentTimeMillis() + "");
            if (pushMessage.pushMessageType != PushMessageType.PUSH_MESSAGE_TYPE_NORMAL) {
                builder.timeToLive(60); // 单位秒
            } else {
                builder.timeToLive(10 * 60);
            }
            resultMessage = senderMessage.sendSingle(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Vivo推送异常：" + e.getMessage());
        }
        if (resultMessage != null) {
            LOG.info("Server response: MessageId: " + resultMessage.getTaskId()
                    + " ErrorCode: " + resultMessage.getResult()
                    + " Reason: " + resultMessage.getDesc());
        }
    }

    private void refreshToken() {
        Sender sender = null;//注册登录开发平台网站获取到的appSecret 
        try {
            sender = new Sender(mConfig.getAppSecret());
            Result result = sender.getToken(mConfig.getAppId(), mConfig.getAppKey());//注册登录开发平台网站获取到的appId和appKey 
            authToken = result.getAuthToken();
            tokenExpiredTime = System.currentTimeMillis() + 12 * 60 * 60 * 1000;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("getToken error" + e.getMessage());
        }
    }

    public void push(PushMessage pushMessage) {
        if (tokenExpiredTime <= System.currentTimeMillis()) {
            refreshToken();
        }

        if(pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_RECALLED || pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_DELETED) {
            //Todo not implement
            //撤回或者删除消息，需要更新远程通知，暂未实现
            return;
        }

        Result resultMessage = null;
        try {
            if (pushMessage.isHiddenDetail) {
                pushMessage.pushContent = "您收到一条新消息";
            }
            String title;
            if (pushMessage.pushMessageType == PushMessageType.PUSH_MESSAGE_TYPE_FRIEND_REQUEST) {
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "好友请求";
                } else {
                    title = pushMessage.senderName + " 请求加您为好友";
                }
            } else {
                if (StringUtils.isEmpty(pushMessage.senderName)) {
                    title = "消息";
                } else {
                    title = pushMessage.senderName;
                }
            }
            Sender senderMessage = new Sender(mConfig.getAppSecret());
            senderMessage.setAuthToken(authToken);
            Message.Builder builder = new Message.Builder()
                    .regId(pushMessage.getDeviceToken())//该测试手机设备订阅推送后生成的regId 
                    .notifyType(4)
                    .title(title)
                    .content(pushMessage.pushContent)
                    .timeToLive(1000)
                    .skipType(1)
                    .networkType(-1)
                    .requestId(System.currentTimeMillis() + "");
            if (pushMessage.pushMessageType != PushMessageType.PUSH_MESSAGE_TYPE_NORMAL) {
                builder.timeToLive(60); // 单位秒
            } else {
                builder.timeToLive(10 * 60);
            }
            resultMessage = senderMessage.sendSingle(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("sendSingle error " + e.getMessage());
        }
        if (resultMessage != null) {

            LOG.info("Server response: MessageId: " + resultMessage.getTaskId()
                    + " ErrorCode: " + resultMessage.getResult()
                    + " Reason: " + resultMessage.getDesc());
        }
    }
}
