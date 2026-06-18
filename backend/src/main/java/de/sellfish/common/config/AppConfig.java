package de.sellfish.common.config;

import de.sellfish.enterprise.EnterpriseProperties;
import de.sellfish.sso.OidcProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Aktiviert die anwendungsweiten ConfigurationProperties und stellt geteilte Beans bereit.
 */
@Configuration
@EnableConfigurationProperties({
        CryptoProperties.class,
        StorageProperties.class,
        InfisicalProperties.class,
        EnterpriseProperties.class,
        OidcProperties.class
})
public class AppConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
