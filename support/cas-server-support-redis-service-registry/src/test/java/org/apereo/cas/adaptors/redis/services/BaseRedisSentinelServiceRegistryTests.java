package org.apereo.cas.adaptors.redis.services;

import org.apereo.cas.config.CasCoreNotificationsAutoConfiguration;
import org.apereo.cas.config.CasCoreServicesAutoConfiguration;
import org.apereo.cas.config.CasCoreUtilAutoConfiguration;
import org.apereo.cas.config.CasCoreWebAutoConfiguration;
import org.apereo.cas.config.RedisServiceRegistryAutoConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.AbstractServiceRegistryTests;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServiceRegistry;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Common class of Unit test for {@link RedisServiceRegistry} class.
 *
 * @author Julien Gribonvald
 * @since 6.1.0
 */
@SpringBootTest(classes = {
    RedisServiceRegistryAutoConfiguration.class,
    CasCoreNotificationsAutoConfiguration.class,
    CasCoreServicesAutoConfiguration.class,
    CasCoreWebAutoConfiguration.class,
    CasCoreUtilAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    RefreshAutoConfiguration.class
})
@EnableScheduling
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Getter
public abstract class BaseRedisSentinelServiceRegistryTests extends AbstractServiceRegistryTests {
    @Autowired
    @Qualifier("redisServiceRegistry")
    private ServiceRegistry newServiceRegistry;

    @Test
    void verifyFailures() throws Throwable {
        assertNull(newServiceRegistry.save((RegisteredService) null));
        assertFalse(newServiceRegistry.delete(null));
    }
}
