package de.sellfish.enterprise;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Aktiviert eine Bean/Methode nur, wenn das angegebene Enterprise-Feature aktiv ist
 * (entweder via {@code app.enterprise.features.X=true} oder via gültiger License).
 *
 * <p>Verwendung: {@code @FeatureToggle("sso")} auf einer {@code @RestController}-Klasse
 * oder {@code @Bean}-Methode.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(FeatureToggle.Condition.class)
public @interface FeatureToggle {

    String value();

    class Condition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String feature = (String) metadata.getAnnotationAttributes(FeatureToggle.class.getName())
                    .get("value");
            if (feature == null || feature.isBlank()) {
                return ConditionOutcome.match("no feature specified — allowing");
            }
            LicenseService licenseService = context.getBeanFactory().getBean(LicenseService.class);
            if (licenseService.isEnterpriseFeatureEnabled(feature)) {
                return ConditionOutcome.match("enterprise feature '" + feature + "' enabled");
            }
            return ConditionOutcome.noMatch("enterprise feature '" + feature + "' not enabled");
        }
    }
}
