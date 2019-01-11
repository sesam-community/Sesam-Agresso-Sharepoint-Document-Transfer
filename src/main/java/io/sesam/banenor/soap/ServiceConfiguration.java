
package io.sesam.banenor.soap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 *
 * @author Timur Samkharadze
 */
@ConfigurationProperties
@Component
public class ServiceConfiguration {
    @Value("${FILE_SERVICE_URL}")
    private String url;
    @Value("${FILE_SERVICE_ACTION}")
    private String action;

    @Nullable
    public String getUrl() {
        return this.url;
    }
    @Nullable
    public String getAction() {
        return action;
    }
    
    
    
    
}
