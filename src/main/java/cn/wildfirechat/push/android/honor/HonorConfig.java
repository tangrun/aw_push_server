package cn.wildfirechat.push.android.honor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Rain
 * @date 2023/4/12 10:49
 */
@Configuration
@ConfigurationProperties(prefix="honor")
@PropertySource(value = "file:config/honor.properties")
public class HonorConfig {
    private String appId;
    private String clientId;
    private String clientSecret;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
