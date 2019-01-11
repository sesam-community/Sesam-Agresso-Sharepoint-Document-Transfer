package io.sesam.banenor.sp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author Timur Samkharadze
 */
@ConfigurationProperties
@Component
public class SpServiceConfiguration {

    @Value("${SP_USERNAME:null}")
    private String spUsername;
    @Value("${SP_PASSWORD:null}")
    private String spPassword;
    @Value("${SP_BASE_URL}")
    private String baseUrl;
    @Value("${SP_LIBRARY}")
    private String library;

    public String getSpUsername() {
        return this.spUsername;
    }

    public String getSpPassword() {
        return this.spPassword;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getLibrary() {
        return this.library;
    }



    @Override
    public String toString() {
        return "SpServiceConfiguration{" + "username=" + spUsername + ", password=" + spPassword.replaceAll("\\.", "*")
                + ", baseUrl=" + baseUrl + ", library=" + library + '}';
    }

}
