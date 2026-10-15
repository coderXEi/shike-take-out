package com.shike.config;

import com.shike.interceptor.JwtTokenAdminInterceptor;
import com.shike.interceptor.JwtTokenUserInterceptor;
import com.shike.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 *
 * <p>注意：此处实现 {@link WebMvcConfigurer} 而<b>不是</b>继承 WebMvcConfigurationSupport。
 * 注意：继承 WebMvcConfigurationSupport 会关闭 Spring Boot 的 WebMvc 自动配置，
 * 进而导致 SpringDoc 的 {@code SpringDocConfigProperties}（由自动配置注册）不会生效，
 * 使得 {@code springdoc.api-docs.*}、{@code springdoc.cache.*} 等配置项全部被忽略。</p>
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry 拦截器注册
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                // 将拦截器注册在admin/下
                .addPathPatterns("/admin/**")
                // 登录接口排除
                .excludePathPatterns("/admin/employee/login")
                // 店铺状态查询不需要登录
                .excludePathPatterns("/admin/shop/status");

        registry.addInterceptor(jwtTokenUserInterceptor)
                // 将拦截器注册在user下
                .addPathPatterns("/user/**")
                // 微信登录接口无需token
                .excludePathPatterns("/user/user")
                // 店铺营业状态查询无需登录（与admin端保持一致）
                .excludePathPatterns("/user/shop/status");
    }

    /**
     * 通过 SpringDoc（knife4j-openapi3）生成接口文档，按包分为「管理端接口」「用户端接口」两组。
     *
     * <p>Spring Boot 3 + jakarta 下，原先的 knife4j 3.x / springfox 方案（{@code Docket}）
     * 已无法使用，改为 SpringDoc 的 {@link GroupedOpenApi} 按包分组；
     * 文档界面仍由 knife4j 提供，访问 {@code /doc.html}。</p>
     *
     * <p><b>左侧菜单的标签（{@code tags}）由 SpringDoc 根据 Controller 上的
     * {@code @Tag} 自动生成，无需手工维护。</b>SpringDoc 的
     * {@code OpenAPIService#buildTags} 会在为一个分组构建文档时，
     * 读取该分组内 Controller / 方法上的 {@code @Tag} 并塞进分组文档的 {@code tags} 数组，
     * Knife4j 正是据此渲染左侧菜单。</p>
     *
     * <p><b>因此严禁再提供全局的 {@link OpenAPI} Bean。</b>SpringDoc 会以缓存的
     * （即全局 Bean 构建出的）{@code OpenAPI} 作为<b>每个</b>分组文档的起点，
     * 于是全局 Bean 里声明的 {@code tags} 会被带进所有分组：
     * 切到「管理端」时左侧却列出用户端的所有标签（接口虽不可调用，但菜单冗余）。
     * 两个分组各写一份 {@code tags} 同样是错的（写少了会漏、写多了会串）。</p>
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.shike.controller.admin")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info().title("食客外卖接口文档 - 管理端").version("1.0")))
                .build();
    }

    /**
     * 用户端接口分组。标签同样由 Controller 上的 {@code @Tag} 自动生成，
     * 详见 {@link #adminApi()} 的说明。
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .packagesToScan("com.shike.controller.user")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info().title("食客外卖接口文档 - 用户端").version("1.0")))
                .build();
    }

    /**
     * 设置静态资源映射
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * Web 全局的json格式化器，用于Controller入参和出参的 json - java对象 序列化与反序列化
     * 添加消息转换器  spring mvc提供
     *
     * @param converters 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("自定义消息转换器");
        // 就地替换已有 Jackson 转换器的 ObjectMapper，而不是在索引 0 新增一个转换器。
        //
        // 原因：SpringDoc 的 /v3/api-docs 接口返回的是 byte[]（内部已是 JSON 字节），
        // 按消息转换器顺序应由 ByteArrayHttpMessageConverter 直接输出。
        // 若把自定义 Jackson 转换器插到最前面，byte[] 会被它当成普通 Java 对象
        // 序列化成 Base64 字符串（响应体变成 "eyJvcG..."，Content-Type 变为 application/json），
        // Knife4j 4.5 前端按 JSON 解析失败 → 白屏 / “Knife4j 文档请求异常”。
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                // 使用项目自定义的 JacksonObjectMapper：
                // 1. 忽略未知属性（反序列化时不因多余字段报错）
                // 2. 统一 LocalDateTime/LocalDate/LocalTime 的序列化与反序列化格式
                jacksonConverter.setObjectMapper(new JacksonObjectMapper());
                return;
            }
        }
    }
}
