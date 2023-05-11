package iudx.resource.server.database.elastic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestEsResponseFormatterToJson {
    @Mock
    List<Hit<ObjectNode>> searchHits;
    private EsResponseFormatterToJson responseFormatterToJson;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        Configuration configuration = new Configuration();
        JsonObject config = configuration.configLoader(8, Vertx.vertx());
        String filePath = config.getString("filePath") + "dummy_file" + ".json";
        File file = new File(filePath);
        responseFormatterToJson = new EsResponseFormatterToJson(file);
        responseFormatterToJson.fileWriter = mock(FileWriter.class);

        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test write method : Success")
    public void testWrite(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToJson.write(searchHits);
        verify(responseFormatterToJson.fileWriter,times(1)).write('[');
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test write method : Failure")
    public void testWriteFailure(VertxTestContext vertxTestContext) throws IOException {
        doThrow(IOException.class).when(responseFormatterToJson.fileWriter).write('[');
        assertThrows(RuntimeException.class, ()-> responseFormatterToJson.write(searchHits));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test finish method : Success")
    public void testFinish(VertxTestContext vertxTestContext) throws IOException {
        responseFormatterToJson.finish();
        verify(responseFormatterToJson.fileWriter,times(1)).write(']');
        verify(responseFormatterToJson.fileWriter,times(1)).close();
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test finish method : Failure")
    public void testFinishFailure(VertxTestContext vertxTestContext) throws IOException {
        doThrow(IOException.class).when(responseFormatterToJson.fileWriter).close();

        assertThrows(RuntimeException.class,()-> responseFormatterToJson.finish());

        verify(responseFormatterToJson.fileWriter,times(1)).write(']');
        verify(responseFormatterToJson.fileWriter,times(1)).close();
        vertxTestContext.completeNow();
    }
}
