package com.shike.config;

import com.shike.interceptor.JwtTokenAdminInterceptor;
import com.shike.interceptor.JwtTokenUserInterceptor;
import com.shike.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
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
 * 继承 WebMvcConfigurationSupport 会关闭 Spring Boot 的 WebMvc 自动配置，
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
     * 通过 SpringDoc（knife4j-openapi3）生成接口文档。
     *
     * <p>Spring Boot 3 + jakarta 下，原先的 knife4j 3.x / springfox 方案（{@code Docket}）
     * 已无法使用，改为 SpringDoc 的 {@link GroupedOpenApi} 按包分组；
     * 文档界面仍由 knife4j 提供，访问 {@code /doc.html}。</p>
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.shike.controller.admin")
                .build();
    }

    /**
     * 显式声明 OpenAPI 根级信息。
     *
     * <p>修复 Knife4j 4.5.0 白屏 / “Knife4j 文档请求异常”：Knife4j 4.5.0 依赖的
     * SpringDoc 2.3.0 在生成 {@link GroupedOpenApi} 分组文档时不会输出根级 {@code tags} 数组，
     * 而 Knife4j 前端正是依据该数组构建左侧接口菜单，取不到就渲染成空白页。
     * 此处显式提供带有 {@code tags} 的 OpenAPI 对象，保证菜单可正常渲染。</p>
     */
    @Bean
    public OpenAPI shikeOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("食客外卖接口文档").version("1.0"))
                .addTagsItem(new Tag().name("员工相关接口"))
                .addTagsItem(new Tag().name("分类相关接口"))
                .addTagsItem(new Tag().name("菜品相关接口"))
                .addTagsItem(new Tag().name("套餐相关接口"))
                .addTagsItem(new Tag().name("店铺设置接口"))
                .addTagsItem(new Tag().name("管理端订单管理接口"))
                .addTagsItem(new Tag().name("工作台相关接口"))
                .addTagsItem(new Tag().name("数据统计 相关接口"))
                .addTagsItem(new Tag().name("公共接口服务"))
                .addTagsItem(new Tag().name("RAG 调试接口"))
                .addTagsItem(new Tag().name("C端用户接口"))
                .addTagsItem(new Tag().name("C端-分类接口"))
                .addTagsItem(new Tag().name("C端-菜品浏览接口"))
                .addTagsItem(new Tag().name("C端-套餐浏览接口"))
                .addTagsItem(new Tag().name("C端地址簿接口"))
                .addTagsItem(new Tag().name("用户端 购物车接口"))
                .addTagsItem(new Tag().name("用户端订单类接口"));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .packagesToScan("com.shike.controller.user")
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
