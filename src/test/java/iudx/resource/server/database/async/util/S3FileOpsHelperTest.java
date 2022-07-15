package iudx.resource.server.database.async.util;

import com.amazonaws.regions.Regions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class S3FileOpsHelperTest {
    @Mock
    Regions clientRegion;
    @Mock
    File file;
    String bucketName;
    S3FileOpsHelper opsHelper;
    String bucket;
    String expected_error;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
//        bucketName = "arn:/abcd:abcd:abcd:abcd:accesspoint:dummy/access/point.:abcd/abcd/:abcd/:abcd/:abcd/::abcd:abcdabcd:s3-object-lambda";
        bucket = "arn:aws:s3:::examplebucket/developers/design_info.doc";
        when(clientRegion.getName()).thenReturn("Dummy.client-region.3");
//        S3FileOpsHelper.fileInputStream = mock(FileInputStream.class);
        opsHelper = new S3FileOpsHelper(clientRegion, bucket);
        vertxTestContext.completeNow();
    }

    /**
     * this test case fails due to SdkClientException
     * which is caused by NULL access key and secret key
     **/

    @Test
    @DisplayName("Test generatePreSignedUrl method : with SdkClientException")
    public void test_generatePreSignedUrl_with_SdkClientException(VertxTestContext vertxTestContext) {
        long expiryTimeMillis = 3032000;
        String objectKey = "Dummy objectKey";
        assertNotNull(opsHelper.generatePreSignedUrl(expiryTimeMillis, objectKey));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test s3Upload method : with NULL result due to failure in connecting endpoint")
    public void test_s3Upload_AmazonClientException(VertxTestContext vertxTestContext) {
        String objectKey = "Dummy object key";
        when(file.getName()).thenReturn("Dummy_file.pdf");
        when(file.length()).thenReturn(30302000L);
        opsHelper.s3Upload(file, objectKey, handler -> {
            if (handler.succeeded()) {
                    vertxTestContext.failNow(handler.cause());
            } else {
                expected_error = "Unable to execute HTTP request";
                assertTrue(handler.cause().getMessage().contains(expected_error));
                vertxTestContext.completeNow();
            }
        });
    }

}
