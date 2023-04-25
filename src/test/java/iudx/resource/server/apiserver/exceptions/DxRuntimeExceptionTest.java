package iudx.resource.server.apiserver.exceptions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.common.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class DxRuntimeExceptionTest {

  @Test
  @DisplayName("Test getApplicableFilters method Success")
  public void getStatusCode(VertxTestContext vertxTestContext) {
    int statusCode = 3;
    String message = "dummy string";
    ResponseUrn responseUrn = ResponseUrn.SUCCESS_URN;
    DxRuntimeException dxRuntimeException =
        new DxRuntimeException(statusCode, responseUrn, message);
    assertEquals(dxRuntimeException.getStatusCode(), 3);
    assertEquals(dxRuntimeException.getMessage(), "dummy string");
    assertEquals(dxRuntimeException.getUrn(), responseUrn);

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getApplicableFilters method Success")
  public void constructorTest(VertxTestContext vertxTestContext) {
    int statusCode = 3;
    String message = "dummy string";
    ResponseUrn responseUrn = ResponseUrn.SUCCESS_URN;
    DxRuntimeException dxRuntimeException = new DxRuntimeException(statusCode, responseUrn);
    assertEquals(dxRuntimeException.getStatusCode(), 3);
    assertEquals(dxRuntimeException.getUrn(), responseUrn);
    assertEquals(dxRuntimeException.getMessage(), responseUrn.getMessage());

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getApplicableFilters method Success")
  public void constructorTest2(VertxTestContext vertxTestContext) {
    int statusCode = 3;
    String message = "dummy string";
    Throwable cause = new Throwable("failed");
    ResponseUrn responseUrn = ResponseUrn.SUCCESS_URN;
    DxRuntimeException dxRuntimeException = new DxRuntimeException(statusCode, responseUrn, cause);
    assertEquals(dxRuntimeException.getStatusCode(), 3);
    assertEquals(dxRuntimeException.getUrn(), responseUrn);
    assertEquals(dxRuntimeException.getMessage(), responseUrn.getMessage());

    vertxTestContext.completeNow();
  }
}
