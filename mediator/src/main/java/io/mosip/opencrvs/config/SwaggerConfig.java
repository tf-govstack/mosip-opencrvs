package io.mosip.opencrvs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${openapi.info.title:Mosip OpenCRVS Mediator}")
    private String title;

    @Value("${openapi.info.version:1.0}")
    private String version;

    @Value("${openapi.info.description:Apis for OpenCRVS Mediator to call}")
    private String description;

    @Value("${openapi.info.license.name:Mosip}")
    private String licenseName;

    @Value("${openapi.info.license.url:https://docs.mosip.io/platform/license}")
    private String licenseUrl;

    @Value("${openapi.service.server.url:/}")
    private String serverUrl;

    @Bean
    public OpenAPI openApi() {
        OpenAPI api = new OpenAPI().components(new Components())
                .info(new Info().title(title)
                        .version(version)
                        .description(description)
                        .license(new License().name(licenseName).url(licenseUrl)));

        api.addServersItem(new Server().url(serverUrl));
        return api;
    }
}
