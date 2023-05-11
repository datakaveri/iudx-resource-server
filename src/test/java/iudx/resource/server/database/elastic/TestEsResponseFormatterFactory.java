package iudx.resource.server.database.elastic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestEsResponseFormatterFactory {
  private static Configuration configuration;
  private static String filePath;
  private EsResponseFormatterFactory esResponseFormatterFactory;
  private File file;

  @BeforeAll
  public static void setUp(VertxTestContext vertxTestContext) {
    configuration = new Configuration();
    JsonObject asyncConfig = configuration.configLoader(8, Vertx.vertx());
    filePath = asyncConfig.getString("filePath");
    vertxTestContext.completeNow();
  }

  static Stream<Arguments> values() {
    return Stream.of(
        Arguments.of("csv", filePath +  "dummy_file" + "." + "csv"),
        Arguments.of("json", filePath + "dummy_file" + "." + "json"));
  }

  @ParameterizedTest
  @DisplayName("Test setTopicPermissions method : with different status code")
  @MethodSource("values")
  public void testCreateInstance(String format, String path, VertxTestContext vertxTestContext) {
    file = new File(path);
    esResponseFormatterFactory = new EsResponseFormatterFactory(format, file);
    assertNotNull(esResponseFormatterFactory.createInstance());
    vertxTestContext.completeNow();
  }
}
