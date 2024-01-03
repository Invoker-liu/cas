package org.apereo.cas.audit;

import org.apereo.cas.audit.spi.BaseAuditConfigurationTests;
import org.apereo.cas.config.CasCoreAuditAutoConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationAutoConfiguration;
import org.apereo.cas.config.CasCoreAutoConfiguration;
import org.apereo.cas.config.CasCoreNotificationsAutoConfiguration;
import org.apereo.cas.config.CasCoreServicesAutoConfiguration;
import org.apereo.cas.config.CasCoreTicketsAutoConfiguration;
import org.apereo.cas.config.CasCoreUtilAutoConfiguration;
import org.apereo.cas.config.CasCoreWebAutoConfiguration;
import org.apereo.cas.config.CasSupportDynamoDbAuditAutoConfiguration;
import org.apereo.cas.util.junit.EnabledIfListeningOnPort;
import lombok.Getter;
import org.apereo.inspektr.audit.AuditTrailManager;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import software.amazon.awssdk.core.SdkSystemSetting;

/**
 * This is {@link DynamoDbAuditTrailManagerTests}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@SpringBootTest(classes = {
    CasCoreAuditAutoConfiguration.class,
    CasCoreAutoConfiguration.class,
    CasCoreAuthenticationAutoConfiguration.class,
    CasSupportDynamoDbAuditAutoConfiguration.class,
    CasCoreUtilAutoConfiguration.class,
    CasCoreServicesAutoConfiguration.class,
    CasCoreTicketsAutoConfiguration.class,
    CasCoreWebAutoConfiguration.class,
    CasCoreNotificationsAutoConfiguration.class,
    RefreshAutoConfiguration.class,
    WebMvcAutoConfiguration.class
},
    properties = {
        "cas.audit.dynamo-db.endpoint=http://localhost:8000",
        "cas.audit.dynamo-db.drop-tables-on-startup=true",
        "cas.audit.dynamo-db.local-instance=true",
        "cas.audit.dynamo-db.region=us-east-1",
        "cas.audit.dynamo-db.asynchronous=false"
    }
)
@Tag("DynamoDb")
@Getter
@EnabledIfListeningOnPort(port = 8000)
class DynamoDbAuditTrailManagerTests extends BaseAuditConfigurationTests {

    static {
        System.setProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property(), "AKIAIPPIGGUNIO74C63Z");
        System.setProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property(), "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");
    }
    
    @Autowired
    @Qualifier("dynamoDbAuditTrailManager")
    private AuditTrailManager auditTrailManager;
}
