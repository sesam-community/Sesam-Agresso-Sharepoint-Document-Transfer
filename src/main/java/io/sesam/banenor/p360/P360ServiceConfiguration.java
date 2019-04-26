package io.sesam.banenor.p360;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfigurasjon for P360 service
 *
 * @author Timur Samkharadze <timur.samkharadze@sysco.no>
 */
@ConfigurationProperties
@Component
public class P360ServiceConfiguration {

    /**
     * brukernavn for kobling til p360 saksystem
     */
    @Value("${P360_USERNAME:null}")
    private String p360Username;

    /**
     * passord for å koble til p360 saksystem
     */
    @Value("${P360_PASSWORD:null}")
    private String p360Password;

    /**
     * Sharepoint blibliteknavn hvor dokumenter fra P360 skal synkroniseres
     */
    @Value("${P360_SP_LIB_NAME:null}")
    private String p360SpLibraryName;

    public String getP360Username() {
        return p360Username;
    }

    public String getP360Password() {
        return p360Password;
    }

    public String getP360SpLibraryName() {
        return p360SpLibraryName;
    }

}
