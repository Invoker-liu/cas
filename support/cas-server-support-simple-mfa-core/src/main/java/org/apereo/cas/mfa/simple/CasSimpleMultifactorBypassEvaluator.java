package org.apereo.cas.mfa.simple;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is {@link CasSimpleMultifactorBypassEvaluator}.
 *
 * @author Misagh Moayyed
 * @since 7.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CasSimpleMultifactorBypassEvaluator {
}
