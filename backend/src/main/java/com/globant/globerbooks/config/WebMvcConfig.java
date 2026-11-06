package com.globant.globerbooks.config;

import com.globant.globerbooks.config.json.StrictJsonBodyResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Puts the strict body resolver in front of the framework's own {@code @RequestBody} processor, so
 * no body is read by the lenient path.
 */
@Configuration(proxyBeanMethods = false)
public class WebMvcConfig {

    /**
     * Creates the post-processor that prepends the strict resolver to the handler adapter.
     *
     * @param mapper the strict mapper, resolved lazily because this bean is created early
     * @return the post-processor
     */
    @Bean
    public static BeanPostProcessor strictBodyResolverInstaller(ObjectProvider<JsonMapper> mapper) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof RequestMappingHandlerAdapter adapter) {
                    install(adapter, mapper.getObject());
                }
                return bean;
            }
        };
    }

    private static void install(RequestMappingHandlerAdapter adapter, JsonMapper mapper) {
        List<HandlerMethodArgumentResolver> existing = adapter.getArgumentResolvers();
        List<HandlerMethodArgumentResolver> all = new ArrayList<>();
        all.add(new StrictJsonBodyResolver(mapper));
        if (existing != null) {
            all.addAll(existing);
        }
        adapter.setArgumentResolvers(all);
    }
}
