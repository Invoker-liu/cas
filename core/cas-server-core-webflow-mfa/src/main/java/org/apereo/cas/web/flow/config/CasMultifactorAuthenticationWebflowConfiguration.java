package org.apereo.cas.web.flow.config;

import org.apereo.cas.authentication.DefaultMultifactorAuthenticationProviderResolver;
import org.apereo.cas.authentication.DefaultMultifactorAuthenticationTriggerSelectionStrategy;
import org.apereo.cas.authentication.MultifactorAuthenticationContextValidator;
import org.apereo.cas.authentication.MultifactorAuthenticationFailureModeEvaluator;
import org.apereo.cas.authentication.MultifactorAuthenticationPrincipalResolver;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderResolver;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.authentication.MultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.MultifactorAuthenticationTriggerSelectionStrategy;
import org.apereo.cas.authentication.adaptive.geo.GeoLocationService;
import org.apereo.cas.authentication.mfa.trigger.AdaptiveMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.AuthenticationAttributeMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.GlobalMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.GroovyScriptMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.HttpRequestMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.PredicatedPrincipalAttributeMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.PrincipalAttributeMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.RegisteredServiceMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.RegisteredServicePrincipalAttributeMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.RestEndpointMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.ScriptedRegisteredServiceMultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.mfa.trigger.TimedMultifactorAuthenticationTrigger;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.SingleSignOnParticipationStrategy;
import org.apereo.cas.web.flow.actions.MultifactorAuthenticationAvailableAction;
import org.apereo.cas.web.flow.actions.MultifactorAuthenticationBypassAction;
import org.apereo.cas.web.flow.actions.MultifactorAuthenticationFailureAction;
import org.apereo.cas.web.flow.actions.composite.MultifactorProviderSelectedAction;
import org.apereo.cas.web.flow.actions.composite.PrepareMultifactorProviderSelectionAction;
import org.apereo.cas.web.flow.authentication.ChainingMultifactorAuthenticationProviderSelector;
import org.apereo.cas.web.flow.authentication.GroovyScriptMultifactorAuthenticationProviderSelector;
import org.apereo.cas.web.flow.authentication.RankedMultifactorAuthenticationProviderSelector;
import org.apereo.cas.web.flow.configurer.CompositeProviderSelectionMultifactorWebflowConfigurer;
import org.apereo.cas.web.flow.resolver.CasDelegatingWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.CasWebflowEventResolutionConfigurationContext;
import org.apereo.cas.web.flow.resolver.impl.CompositeProviderSelectionMultifactorWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.DefaultCasDelegatingWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.RankedMultifactorAuthenticationProviderWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.SelectiveMultifactorAuthenticationProviderWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.mfa.DefaultMultifactorAuthenticationProviderWebflowEventResolver;

import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

import java.util.List;

/**
 * This is {@link CasMultifactorAuthenticationWebflowConfiguration}.
 *
 * @author Travis Schmidt
 * @since 6.0.0
 */
@Configuration(value = "casMfaWebflowConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnWebApplication
public class CasMultifactorAuthenticationWebflowConfiguration {

    @Configuration(value = "CasMultifactorAuthenticationWebflowResolverConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowResolverConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "multifactorAuthenticationProviderResolver")
        @RefreshScope
        public MultifactorAuthenticationProviderResolver multifactorAuthenticationProviderResolver(
            final List<MultifactorAuthenticationPrincipalResolver> resolvers) {
            AnnotationAwareOrderComparator.sort(resolvers);
            return new DefaultMultifactorAuthenticationProviderResolver(resolvers);
        }

        @Bean
        @RefreshScope
        @ConditionalOnMissingBean(name = "defaultMultifactorAuthenticationPrincipalResolver")
        public MultifactorAuthenticationPrincipalResolver defaultMultifactorAuthenticationPrincipalResolver() {
            return MultifactorAuthenticationPrincipalResolver.identical();
        }
    }
    
    @Configuration(value = "CasMultifactorAuthenticationWebflowAdaptiveTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowAdaptiveTriggerConfiguration {

        @ConditionalOnMissingBean(name = "adaptiveAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver adaptiveAuthenticationPolicyWebflowEventResolver(
            @Qualifier("adaptiveMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger adaptiveMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                adaptiveMultifactorAuthenticationTrigger);
        }

        @ConditionalOnMissingBean(name = "adaptiveMultifactorAuthenticationTrigger")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger adaptiveMultifactorAuthenticationTrigger(
            @Qualifier("geoLocationService")
            final ObjectProvider<GeoLocationService> geoLocationService,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new AdaptiveMultifactorAuthenticationTrigger(geoLocationService.getIfAvailable(), casProperties, applicationContext);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowTimedTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowTimedTriggerConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "timedMultifactorAuthenticationTrigger")
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger timedMultifactorAuthenticationTrigger(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new TimedMultifactorAuthenticationTrigger(casProperties, applicationContext);
        }

        @ConditionalOnMissingBean(name = "timedAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver timedAuthenticationPolicyWebflowEventResolver(
            @Qualifier("timedMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger timedMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext, timedMultifactorAuthenticationTrigger);
        }

    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowPrincipalAttributeConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowPrincipalAttributeConfiguration {
        @ConditionalOnMissingBean(name = "principalAttributeMultifactorAuthenticationTrigger")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger principalAttributeMultifactorAuthenticationTrigger(
            @Qualifier("multifactorAuthenticationProviderResolver")
            final MultifactorAuthenticationProviderResolver multifactorAuthenticationProviderResolver,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new PrincipalAttributeMultifactorAuthenticationTrigger(casProperties,
                multifactorAuthenticationProviderResolver, applicationContext);
        }

        @ConditionalOnMissingBean(name = "principalAttributeAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver principalAttributeAuthenticationPolicyWebflowEventResolver(
            @Qualifier("principalAttributeMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger principalAttributeMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                principalAttributeMultifactorAuthenticationTrigger);
        }

    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowPredicateTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowPredicateTriggerConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "predicatedPrincipalAttributeMultifactorAuthenticationTrigger")
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger predicatedPrincipalAttributeMultifactorAuthenticationTrigger(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new PredicatedPrincipalAttributeMultifactorAuthenticationTrigger(casProperties, applicationContext);
        }

        @ConditionalOnMissingBean(name = "predicatedPrincipalAttributeMultifactorAuthenticationPolicyEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver predicatedPrincipalAttributeMultifactorAuthenticationPolicyEventResolver(
            @Qualifier("predicatedPrincipalAttributeMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger predicatedPrincipalAttributeMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(casWebflowConfigurationContext,
                predicatedPrincipalAttributeMultifactorAuthenticationTrigger);
        }
    }
    
    @Configuration(value = "CasMultifactorAuthenticationWebflowAuthenticationAttributeTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowAuthenticationAttributeTriggerConfiguration {
        @ConditionalOnMissingBean(name = "authenticationAttributeMultifactorAuthenticationTrigger")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger authenticationAttributeMultifactorAuthenticationTrigger(
            @Qualifier("multifactorAuthenticationProviderResolver")
            final MultifactorAuthenticationProviderResolver multifactorAuthenticationProviderResolver,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new AuthenticationAttributeMultifactorAuthenticationTrigger(casProperties,
                multifactorAuthenticationProviderResolver, applicationContext);
        }

        @ConditionalOnMissingBean(name = "authenticationAttributeAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver authenticationAttributeAuthenticationPolicyWebflowEventResolver(
            @Qualifier("authenticationAttributeMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger authenticationAttributeMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                authenticationAttributeMultifactorAuthenticationTrigger);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowSelectorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowSelectorConfiguration {
        @ConditionalOnMissingBean(name = "multifactorAuthenticationProviderSelector")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector(
            @Qualifier("failureModeEvaluator")
            final MultifactorAuthenticationFailureModeEvaluator failureModeEvaluator,
            final CasConfigurationProperties casProperties) {
            val mfa = casProperties.getAuthn().getMfa();
            val script = mfa.getCore().getProviderSelectorGroovyScript();
            if (script.getLocation() != null) {
                return new GroovyScriptMultifactorAuthenticationProviderSelector(script.getLocation());
            }
            if (mfa.getCore().isProviderSelectionEnabled()) {
                return new ChainingMultifactorAuthenticationProviderSelector(failureModeEvaluator);
            }
            return new RankedMultifactorAuthenticationProviderSelector();
        }

        @RefreshScope
        @Bean
        @ConditionalOnMissingBean(name = "defaultMultifactorTriggerSelectionStrategy")
        @Autowired
        public MultifactorAuthenticationTriggerSelectionStrategy defaultMultifactorTriggerSelectionStrategy(
            final List<MultifactorAuthenticationTrigger> triggers) {
            AnnotationAwareOrderComparator.sortIfNecessary(triggers);
            return new DefaultMultifactorAuthenticationTriggerSelectionStrategy(triggers);
        }
    }
    
    @Configuration(value = "CasMultifactorAuthenticationWebflowContextConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowContextConfiguration {
        @ConditionalOnMissingBean(name = "rankedAuthenticationProviderWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasDelegatingWebflowEventResolver rankedAuthenticationProviderWebflowEventResolver(
            @Qualifier("initialAuthenticationAttemptWebflowEventResolver")
            final CasDelegatingWebflowEventResolver initialAuthenticationAttemptWebflowEventResolver,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext,
            @Qualifier("authenticationContextValidator")
            final MultifactorAuthenticationContextValidator authenticationContextValidator,
            @Qualifier("singleSignOnParticipationStrategy")
            final SingleSignOnParticipationStrategy webflowSingleSignOnParticipationStrategy) {
            return new RankedMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                initialAuthenticationAttemptWebflowEventResolver,
                authenticationContextValidator,
                webflowSingleSignOnParticipationStrategy);
        }
        
        @ConditionalOnMissingBean(name = "initialAuthenticationAttemptWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasDelegatingWebflowEventResolver initialAuthenticationAttemptWebflowEventResolver(
            @Qualifier("selectiveAuthenticationProviderWebflowEventResolver")
            final CasWebflowEventResolver selectiveAuthenticationProviderWebflowEventResolver,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext,
            @Qualifier("adaptiveAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver adaptiveAuthenticationPolicyWebflowEventResolver,
            @Qualifier("timedAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver timedAuthenticationPolicyWebflowEventResolver,
            @Qualifier("globalAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver globalAuthenticationPolicyWebflowEventResolver,
            @Qualifier("httpRequestAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver httpRequestAuthenticationPolicyWebflowEventResolver,
            @Qualifier("restEndpointAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver restEndpointAuthenticationPolicyWebflowEventResolver,
            @Qualifier("groovyScriptAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver groovyScriptAuthenticationPolicyWebflowEventResolver,
            @Qualifier("scriptedRegisteredServiceAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver scriptedRegisteredServiceAuthenticationPolicyWebflowEventResolver,
            @Qualifier("registeredServicePrincipalAttributeAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver registeredServicePrincipalAttributeAuthenticationPolicyWebflowEventResolver,
            @Qualifier("predicatedPrincipalAttributeMultifactorAuthenticationPolicyEventResolver")
            final CasWebflowEventResolver predicatedPrincipalAttributeMultifactorAuthenticationPolicyEventResolver,
            @Qualifier("principalAttributeAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver principalAttributeAuthenticationPolicyWebflowEventResolver,
            @Qualifier("authenticationAttributeAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver authenticationAttributeAuthenticationPolicyWebflowEventResolver,
            @Qualifier("registeredServiceAuthenticationPolicyWebflowEventResolver")
            final CasWebflowEventResolver registeredServiceAuthenticationPolicyWebflowEventResolver) {

            val resolver = new DefaultCasDelegatingWebflowEventResolver(casWebflowConfigurationContext,
                selectiveAuthenticationProviderWebflowEventResolver);
            resolver.addDelegate(adaptiveAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(timedAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(globalAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(httpRequestAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(restEndpointAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(groovyScriptAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(scriptedRegisteredServiceAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(registeredServicePrincipalAttributeAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(predicatedPrincipalAttributeMultifactorAuthenticationPolicyEventResolver);
            resolver.addDelegate(principalAttributeAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(authenticationAttributeAuthenticationPolicyWebflowEventResolver);
            resolver.addDelegate(registeredServiceAuthenticationPolicyWebflowEventResolver);
            return resolver;
        }

    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowRestConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowRestConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "restEndpointMultifactorAuthenticationTrigger")
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger restEndpointMultifactorAuthenticationTrigger(
            @Qualifier("multifactorAuthenticationProviderResolver")
            final MultifactorAuthenticationProviderResolver multifactorAuthenticationProviderResolver,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new RestEndpointMultifactorAuthenticationTrigger(casProperties, multifactorAuthenticationProviderResolver, applicationContext);
        }

        @ConditionalOnMissingBean(name = "restEndpointAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver restEndpointAuthenticationPolicyWebflowEventResolver(
            @Qualifier("restEndpointMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger restEndpointMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                restEndpointMultifactorAuthenticationTrigger);
        }

    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowGlobalTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowGlobalTriggerConfiguration {
        @ConditionalOnMissingBean(name = "globalAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger globalMultifactorAuthenticationTrigger(
            @Qualifier("multifactorAuthenticationProviderSelector")
            final MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new GlobalMultifactorAuthenticationTrigger(casProperties, applicationContext, multifactorAuthenticationProviderSelector);
        }

        @ConditionalOnMissingBean(name = "globalAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver globalAuthenticationPolicyWebflowEventResolver(
            @Qualifier("globalMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger globalMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext, globalMultifactorAuthenticationTrigger);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowGroovyTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowGroovyTriggerConfiguration {

        @ConditionalOnMissingBean(name = "groovyScriptMultifactorAuthenticationTrigger")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger groovyScriptMultifactorAuthenticationTrigger(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new GroovyScriptMultifactorAuthenticationTrigger(casProperties, applicationContext);
        }

        @ConditionalOnMissingBean(name = "groovyScriptAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver groovyScriptAuthenticationPolicyWebflowEventResolver(
            @Qualifier("groovyScriptMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger groovyScriptMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                groovyScriptMultifactorAuthenticationTrigger);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowHttpRequestTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowHttpRequestTriggerConfiguration {
        @ConditionalOnMissingBean(name = "httpRequestMultifactorAuthenticationTrigger")
        @Bean
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger httpRequestMultifactorAuthenticationTrigger(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new HttpRequestMultifactorAuthenticationTrigger(casProperties, applicationContext);
        }

        @ConditionalOnMissingBean(name = "httpRequestAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver httpRequestAuthenticationPolicyWebflowEventResolver(
            @Qualifier("httpRequestMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger httpRequestMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                httpRequestMultifactorAuthenticationTrigger);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowRegisteredServiceWithPrincipalTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowRegisteredServiceWithPrincipalTriggerConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "registeredServicePrincipalAttributeMultifactorAuthenticationTrigger")
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger registeredServicePrincipalAttributeMultifactorAuthenticationTrigger(
            @Qualifier("multifactorAuthenticationProviderSelector")
            final MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector,
            @Qualifier("multifactorAuthenticationProviderResolver")
            final MultifactorAuthenticationProviderResolver multifactorAuthenticationProviderResolver,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new RegisteredServicePrincipalAttributeMultifactorAuthenticationTrigger(casProperties,
                multifactorAuthenticationProviderResolver, applicationContext, multifactorAuthenticationProviderSelector);
        }

        @ConditionalOnMissingBean(name = "registeredServicePrincipalAttributeAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver registeredServicePrincipalAttributeAuthenticationPolicyWebflowEventResolver(
            @Qualifier("registeredServicePrincipalAttributeMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger registeredServicePrincipalAttributeMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                registeredServicePrincipalAttributeMultifactorAuthenticationTrigger);
        }
    }

    @Deprecated(since = "6.2.0")
    @Configuration(value = "CasMultifactorAuthenticationWebflowScriptTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowScriptTriggerConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "scriptedRegisteredServiceMultifactorAuthenticationTrigger")
        @RefreshScope
        @Deprecated(since = "6.2.0")
        @SuppressWarnings("InlineMeSuggester")
        @Autowired
        public MultifactorAuthenticationTrigger scriptedRegisteredServiceMultifactorAuthenticationTrigger(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new ScriptedRegisteredServiceMultifactorAuthenticationTrigger(casProperties, applicationContext);
        }

        @ConditionalOnMissingBean(name = "scriptedRegisteredServiceAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        @Deprecated(since = "6.2.0")
        @SuppressWarnings("InlineMeSuggester")
        public CasWebflowEventResolver scriptedRegisteredServiceAuthenticationPolicyWebflowEventResolver(
            @Qualifier("scriptedRegisteredServiceMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger scriptedRegisteredServiceMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                scriptedRegisteredServiceMultifactorAuthenticationTrigger);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowRegisteredServiceTriggerConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowRegisteredServiceTriggerConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "registeredServiceMultifactorAuthenticationTrigger")
        @RefreshScope
        @Autowired
        public MultifactorAuthenticationTrigger registeredServiceMultifactorAuthenticationTrigger(
            @Qualifier("multifactorAuthenticationProviderSelector")
            final MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector,
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return new RegisteredServiceMultifactorAuthenticationTrigger(casProperties,
                multifactorAuthenticationProviderSelector, applicationContext);
        }

        @ConditionalOnMissingBean(name = "registeredServiceAuthenticationPolicyWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver registeredServiceAuthenticationPolicyWebflowEventResolver(
            @Qualifier("registeredServiceMultifactorAuthenticationTrigger")
            final MultifactorAuthenticationTrigger registeredServiceMultifactorAuthenticationTrigger,
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new DefaultMultifactorAuthenticationProviderWebflowEventResolver(
                casWebflowConfigurationContext,
                registeredServiceMultifactorAuthenticationTrigger);
        }
    }

    @Configuration(value = "CasMultifactorAuthenticationWebflowActionsConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasMultifactorAuthenticationWebflowActionsConfiguration {

        @Bean
        @RefreshScope
        @ConditionalOnMissingBean(name = "mfaAvailableAction")
        public Action mfaAvailableAction() {
            return new MultifactorAuthenticationAvailableAction();
        }

        @Bean
        @RefreshScope
        @ConditionalOnMissingBean(name = "mfaBypassAction")
        public Action mfaBypassAction() {
            return new MultifactorAuthenticationBypassAction();
        }

        @Bean
        @RefreshScope
        @ConditionalOnMissingBean(name = "mfaFailureAction")
        public Action mfaFailureAction() {
            return new MultifactorAuthenticationFailureAction();
        }

        @Bean
        @RefreshScope
        @ConditionalOnMissingBean(name = "multifactorProviderSelectedAction")
        public Action multifactorProviderSelectedAction() {
            return new MultifactorProviderSelectedAction();
        }

        @RefreshScope
        @Bean
        @ConditionalOnMissingBean(name = "prepareMultifactorProviderSelectionAction")
        public Action prepareMultifactorProviderSelectionAction() {
            return new PrepareMultifactorProviderSelectionAction();
        }
    }

    @Configuration(value = "CasCoreMultifactorAuthenticationProviderSelectiveConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @ConditionalOnProperty(prefix = "cas.authn.mfa.core", name = "provider-selection-enabled", havingValue = "false", matchIfMissing = true)
    public static class CasCoreMultifactorAuthenticationProviderSelectiveConfiguration {
        @ConditionalOnMissingBean(name = "selectiveAuthenticationProviderWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver selectiveAuthenticationProviderWebflowEventResolver(
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new SelectiveMultifactorAuthenticationProviderWebflowEventResolver(casWebflowConfigurationContext);
        }
    }

    @Configuration(value = "CasCoreMultifactorAuthenticationProviderCompositeConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @ConditionalOnProperty(prefix = "cas.authn.mfa.core", name = "provider-selection-enabled", havingValue = "true")
    public static class CasCoreMultifactorAuthenticationProviderCompositeConfiguration {
        @ConditionalOnMissingBean(name = "selectiveAuthenticationProviderWebflowEventResolver")
        @Bean
        @RefreshScope
        @Autowired
        public CasWebflowEventResolver selectiveAuthenticationProviderWebflowEventResolver(
            @Qualifier("casWebflowConfigurationContext")
            final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
            return new CompositeProviderSelectionMultifactorWebflowEventResolver(casWebflowConfigurationContext);
        }

        @Bean
        @ConditionalOnMissingBean(name = "compositeProviderSelectionMultifactorWebflowConfigurer")
        public CasWebflowConfigurer compositeProviderSelectionMultifactorWebflowConfigurer(
            @Qualifier(CasWebflowConstants.BEAN_NAME_LOGIN_FLOW_DEFINITION_REGISTRY)
            final FlowDefinitionRegistry loginFlowRegistry,
            @Qualifier(CasWebflowConstants.BEAN_NAME_FLOW_BUILDER_SERVICES)
            final FlowBuilderServices flowBuilderServices,
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext) {
            return new CompositeProviderSelectionMultifactorWebflowConfigurer(flowBuilderServices,
                loginFlowRegistry, applicationContext, casProperties);
        }

        @Bean
        @ConditionalOnMissingBean(name = "compositeProviderSelectionCasWebflowExecutionPlanConfigurer")
        @Autowired
        public CasWebflowExecutionPlanConfigurer compositeProviderSelectionCasWebflowExecutionPlanConfigurer(
            @Qualifier("compositeProviderSelectionMultifactorWebflowConfigurer")
            final CasWebflowConfigurer compositeProviderSelectionMultifactorWebflowConfigurer) {
            return plan -> plan.registerWebflowConfigurer(compositeProviderSelectionMultifactorWebflowConfigurer);
        }
    }
}
