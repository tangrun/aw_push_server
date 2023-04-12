package cn.wildfirechat.push.android;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.PushMessageType;
import cn.wildfirechat.push.Utility;
import cn.wildfirechat.push.android.fcm.FCMPush;
import cn.wildfirechat.push.android.hms.HMSPush;
import cn.wildfirechat.push.android.honor.HonorPush;
import cn.wildfirechat.push.android.meizu.MeiZuPush;
import cn.wildfirechat.push.android.oppo.OppoPush;
import cn.wildfirechat.push.android.vivo.VivoPush;
import cn.wildfirechat.push.android.xiaomi.XiaomiPush;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class AndroidPushServiceImpl implements AndroidPushService {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidPushServiceImpl.class);
    @Autowired
    private HMSPush hmsPush;

    @Autowired
    private MeiZuPush meiZuPush;

    @Autowired
    private XiaomiPush xiaomiPush;

    @Autowired
    private VivoPush vivoPush;

    @Autowired
    private OppoPush oppoPush;

    @Autowired
    private FCMPush fcmPush;

    @Autowired
    private HonorPush honorPush;

    private ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 100,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

    @Override
    public Object push(PushMessage pushMessage) {
        LOG.info("Android push {}", new Gson().toJson(pushMessage));
        if(Utility.filterPush(pushMessage)) {
            LOG.info("canceled");
            return "Canceled";
        }
        final long start = System.currentTimeMillis();
        executorService.execute(()->{
            long now = System.currentTimeMillis();
            if (now - start > 15000) {
                LOG.error("等待太久，消息抛弃");
                return;
            }

            switch (pushMessage.getPushType()) {
                case AndroidPushType.ANDROID_PUSH_TYPE_XIAOMI:
                    xiaomiPush.push_aw_chat(pushMessage);
                    break;
                case AndroidPushType.ANDROID_PUSH_TYPE_HUAWEI:
                    hmsPush.push_aw_chat_v1(pushMessage);//旧方式 官方已废弃（不推荐使用）
                    break;
                case AndroidPushType.ANDROID_PUSH_TYPE_MEIZU:
                    meiZuPush.push(pushMessage);
                    break;
                case AndroidPushType.ANDROID_PUSH_TYPE_VIVO:
                    vivoPush.push_aw_chat(pushMessage);
                    break;
                case AndroidPushType.ANDROID_PUSH_TYPE_OPPO:
                    oppoPush.push_aw_chat(pushMessage);
                    break;
                case AndroidPushType.ANDROID_PUSH_TYPE_FCM:
                    fcmPush.push(pushMessage);
                    break;
                case AndroidPushType.ANDROID_PUSH_TYPE_HONOR:
                    honorPush.push(pushMessage);
                    break;
                default:
                    LOG.info("unknown push type");
                    break;
            }
        });
        return "ok";
    }
}
