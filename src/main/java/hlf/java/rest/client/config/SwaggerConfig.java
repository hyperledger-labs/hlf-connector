package hlf.java.rest.client.config;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger configuration for the swagger api. Swagger UI is accessible at {hostname}/swagger-ui.html
 * Swagger Open API Doc is accessible at {hostname}/v3/api-docs
 */
@Slf4j
@Configuration
public class SwaggerConfig {

  /**
   * This bean is responsible for adding api key header for all the APIs exposed. Open api global
   * header customizer open api customiser.
   *
   * @return the open api customiser
   */
  @Bean
  public OpenApiCustomiser openApiGlobalHeaderCustomizer() {
    log.info("Inside API customizer");
    StringSchema schema = new StringSchema();
    return openApi ->
        openApi.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(
                operation -> {
                  operation.addParametersItem(
                      new HeaderParameter()
                          .required(true)
                          .name("api-key")
                          .description("API Key")
                          .schema(schema));
                });
  }

  /*
   * @Bean public OpenAPI customOpenAPI(@Value("${springdoc.version}") String
   * appVersion) { return new OpenAPI() .components(new Components()
   * .addParameters("myHeader1", new Parameter().in("header").schema(new
   * StringSchema()).name("myHeader1")).addHeaders("myHeader2", new
   * Header().description("myHeader2 header").schema(new StringSchema())))
   * .info(new Info() .title("Hyperledger Fabric REST Client")
   * .version(appVersion)
   * .description("Hyperledger Fabric REST Client for invoking and query fabric smart-contract"
   * )); }
   */
  @Bean
  SpringDocConfiguration springDocConfiguration() {
    return new SpringDocConfiguration();
  }

  @Bean
  public SpringDocConfigProperties springDocConfigProperties() {
    return new SpringDocConfigProperties();
  }
}
