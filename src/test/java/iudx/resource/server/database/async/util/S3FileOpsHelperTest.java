package iudx.resource.server.database.async.util;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.ZonedDateTime;

import static io.vertx.core.impl.future.CompositeFutureImpl.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class S3FileOpsHelperTest {

    S3FileOpsHelper s3FileOpsHelper;
    @Mock
    File file;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    DefaultAWSCredentialsProviderChain credentialProviderChain;
    @Mock
    AmazonS3 s3Client;
    @Mock
    Regions clientRegion;
    @Mock
    TransferManager tm;
    @Mock
    TransferManagerBuilder tmb;
    @Mock
    ObjectMetadata objectMetadata;
    @Mock
    Upload upload;
    @Mock
    ZonedDateTime zdt;


    @Test
    @DisplayName("Test S3Upload success")
    public void tests3Upload(VertxTestContext vertxTestContext){
        JsonObject jsonObject=new JsonObject();
       jsonObject.put("s3_url",any());
        jsonObject.put("object_id", "dummy string");
        jsonObject.put("expiry",10000);


        s3Client =
                AmazonS3ClientBuilder.standard()
                        .withRegion(clientRegion)
                        .withCredentials(credentialProviderChain)
                        .build();


        when(TransferManagerBuilder.standard()).thenReturn(tmb);
        when(tmb.withS3Client(s3Client)).thenReturn(tmb);
        when(tmb.build()).thenReturn(tm);
        when(file.getName()).thenReturn(anyString());
        when(file.length()).thenReturn((long) anyInt());
        s3FileOpsHelper.s3Upload();


    }

}

