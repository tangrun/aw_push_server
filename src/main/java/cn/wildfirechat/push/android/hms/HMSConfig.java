package cn.wildfirechat.push.android.hms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix="hms")
@PropertySource(value = "file:config/hms.properties")
public class HMSConfig {
    private String appSecret;
    private String appId;
    private String mainClass;
    private String launcherClass;

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getLauncherClass() {
        return launcherClass;
    }

    public void setLauncherClass(String launcherClass) {
        this.launcherClass = launcherClass;
    }
}
