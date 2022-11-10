package iudx.resource.server.encryption;


import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.utils.Base64MessageEncoder;
import com.goterl.lazysodium.utils.Key;
import com.goterl.lazysodium.utils.KeyPair;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static iudx.resource.server.encryption.util.Constants.*;

public class EncryptionServiceImpl implements EncryptionService {
    private static final Logger LOG = LoggerFactory.getLogger(EncryptionServiceImpl.class);
    private static Base64MessageEncoder base64MessageEncoder;
    private Box.Lazy box;

    public EncryptionServiceImpl() {
        LazySodiumJava lazySodiumJava = new LazySodiumJava(new SodiumJava());
        this.box = (Box.Lazy) lazySodiumJava;
    }

    public EncryptionServiceImpl(Box.Lazy box) {
        this.box = box;
    }

    @Override
    public Future<JsonObject> encrypt(String message, JsonObject encodedPublicKey) {
        Promise<JsonObject> promise = Promise.promise();
        if (message == null || message.isEmpty()) {
            promise.fail("message is null or empty");
        }
        if (encodedPublicKey != null && encodedPublicKey.getString(ENCODED_KEY) != null && !encodedPublicKey.getString(ENCODED_KEY).isEmpty()) {
            JsonObject result = new JsonObject();
            //decode public key
            try {
                Key key = decodePublicKey(encodedPublicKey);
                /*String cipherText = box.cryptoBoxSealEasy(message, key);*/
                final String reallyLongCipherText = org.apache.commons.lang.StringUtils.join( new String[] {
                        box.cryptoBoxSealEasy(message, key),
                } );
                // encode
                base64MessageEncoder = new Base64MessageEncoder();
                /*String encodedCipherText = base64MessageEncoder.encode(reallyLongCipherText.getBytes());*/
                final String reallyLongEncodedCipherText = org.apache.commons.lang.StringUtils.join( new String[] {
                        base64MessageEncoder.encode(reallyLongCipherText.getBytes())
                } );
                result.put(ENCODED_CIPHER_TEXT, reallyLongEncodedCipherText);
            }catch (IllegalArgumentException illegalArgumentException)
            {
                LOG.error("Exception while decoding the public key: " + illegalArgumentException);
                LOG.error("The public key should be in URL Safe base64 format");
                promise.fail("Exception while decoding public key: " + illegalArgumentException);
            }
            catch (SodiumException e) {
                LOG.error("Sodium Exception: " + e);
                promise.fail("Sodium exception: " + e);
            }
            promise.tryComplete(result);
        } else {
            LOG.error("public key is empty or null");
            promise.fail("Public key is empty or null");
        }
        return promise.future();
    }


    @Override
    public Future<JsonObject> decrypt(String encodedCipherText, JsonObject keyPair) {
        if (encodedCipherText == null) {
            return Future.failedFuture("encoded cipher text is null");
        }
        if (keyPair == null && keyPair.getValue(KEYPAIR) == null) {
            return Future.failedFuture("key pair is null");
        }
        JsonObject result = new JsonObject();
        // decode encoded cipher Text
        byte[] bytes = base64MessageEncoder.decode(encodedCipherText);
        String decodedMessage = new String(bytes, StandardCharsets.UTF_8);
        KeyPair keys = (KeyPair) keyPair.getValue(KEYPAIR);
        try {
            String message = box.cryptoBoxSealOpenEasy(decodedMessage, keys);
            result.put("message", message);
        } catch (SodiumException e) {
            LOG.error("Sodium Exception: " + e);
            return Future.failedFuture("Sodium Exception");
        }
        return Future.succeededFuture(result);
    }

    @Override
    public Future<JsonObject> encodePublicKey(JsonObject publicKey) {
        if (publicKey != null && publicKey.getString(PUBLIC_KEY) != null && !publicKey.getString(PUBLIC_KEY).isEmpty()) {
            byte[] bytes = publicKey.getBinary(PUBLIC_KEY);
            String encodedKey = Base64.getUrlEncoder().encodeToString(bytes);
            JsonObject result = new JsonObject();
            result.put(ENCODED_KEY, encodedKey);
            return Future.succeededFuture(result);
        } else
            return Future.failedFuture("public key is empty or null");
    }
    public Key decodePublicKey(JsonObject encodedPublicKey) {
        if (encodedPublicKey == null && encodedPublicKey.getString(ENCODED_KEY) == null && encodedPublicKey.getString(ENCODED_KEY).isEmpty()) {
            return null;
        }
        String publicKey = encodedPublicKey.getString(ENCODED_KEY);
        byte[] bytes = Base64.getUrlDecoder().decode(publicKey);
        Key key = Key.fromBytes(bytes);
        return key;
    }
}
