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
public class Credentials {

    @Value("${USERNAME}")
    private String username;
    @Value("${CLIENT}")
    private String client;
    @Value("${PASSWORD}")
    private String password;

    @Nullable
    public String getUsername() {
        return this.username;
    }

    @Nullable
    public String getClient() {
        return this.client;
    }

    @Nullable
    public String getPassword() {
        return this.password;
    }

    @Override
    public String toString() {
        return "Credentials{"
                + "username=" + this.username
                + ", client=" + this.client
                + ", password=" + this.password + '}';
    }

}
