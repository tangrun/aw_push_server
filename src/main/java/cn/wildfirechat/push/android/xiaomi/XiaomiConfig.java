package cn.wildfirechat.push.android.xiaomi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix="xiaomi")
@PropertySource(value = "file:config/xiaomi.properties")
public class XiaomiConfig {
    private String appSecret;
    private String extraParamIntentURI;
    private String extraParamSoundURI;


    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getExtraParamIntentURI() {
        return extraParamIntentURI;
    }

    public void setExtraParamIntentURI(String extraParamIntentURI) {
        this.extraParamIntentURI = extraParamIntentURI;
    }

    public String getExtraParamSoundURI() {
        return extraParamSoundURI;
    }

    public void setExtraParamSoundURI(String extraParamSoundURI) {
        this.extraParamSoundURI = extraParamSoundURI;
    }
}
