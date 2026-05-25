package com.devteam.aiauditserver.Tools.configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class SpringfoxFixConfig {
    @Bean
    public static BeanDefinitionRegistryPostProcessor springfoxFix() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                String[] beans = {"controllerEndpointHandlerMapping", "webEndpointServletHandlerMapping"};
                for (String b : beans) {
                    if (registry.containsBeanDefinition(b)) {
                        AbstractBeanDefinition def = (AbstractBeanDefinition) registry.getBeanDefinition(b);
                        def.setAutowireCandidate(false);
                    }
                }
            }
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory f) throws BeansException {}
        };
    }
}
