package iudx.resource.server.database.latest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;

@ExtendWith({VertxExtension.class})
public class RedisCommandArgsBuilderTest {
  
  static RedisCommandArgsBuilder redisCmdArgsBuilder;
  
  @BeforeAll
  static void init() {
    redisCmdArgsBuilder=new RedisCommandArgsBuilder();
  }
  
  
  @Test
  public void resourceLevelArgs() {
    String id="asdas/asdasdas/adsasdasd/asdasda/asdasdas";
    RedisArgs redisArgs=redisCmdArgsBuilder.getRedisCommandArgs(id, true);
    
    String resourceGroup = id.split("/")[3];
    resourceGroup = resourceGroup.replace("-", "_");
    
    assertEquals(resourceGroup, redisArgs.getKey());
    assertEquals(".", redisArgs.getPath());
  }
  
  @Test
  public void groupLevelArgs() {
    String id="asdas/asdasdas/adsasdasd/asdasda";
    RedisArgs redisArgs=redisCmdArgsBuilder.getRedisCommandArgs(id, false);
    
    String resourceGroup = id.split("/")[3];
    resourceGroup = resourceGroup.replace("-", "_");
    
    String sha = DigestUtils.sha1Hex(id);
    
    assertEquals(resourceGroup, redisArgs.getKey());
    assertEquals("._"+sha+"_d", redisArgs.getPath());
  }

}
