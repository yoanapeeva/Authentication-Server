package bg.sofia.uni.fmi.mjt.authenticationserver.database.cipher;

import bg.sofia.uni.fmi.mjt.authenticationserver.troubleshootlog.TroubleshootLog;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;

public class CipherPassword {
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final Integer KEY_SIZE_IN_BITS = 128;
    private static final SecretKey SECRET_KEY = generateSecretKey();

    private static SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            keyGenerator.init(KEY_SIZE_IN_BITS);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("The algorithm of encryption: " + ENCRYPTION_ALGORITHM + " is incorrect." +
                "Try again later or contact administrator by providing the logs in " +
                TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        }
        return null;
    }

    public static String encryptPassword(String password) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
            byte[] dataBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return new String(dataBytes, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException | BadPaddingException e) {
            System.out.println(
                "The padding of password encryption scheme is incorrect." +
                    "Try again later or contact administrator by providing the logs in " +
                    TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("The algorithm of password encryption: " + ENCRYPTION_ALGORITHM +
                " is incorrect.Try again later or contact administrator by providing the logs in " +
                TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        } catch (InvalidKeyException e) {
            System.out.println(
                "The secretKey of password encryption is incorrect." +
                    "Try again later or contact administrator by providing the logs in " +
                    TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        } catch (IllegalBlockSizeException e) {
            System.out.println(
                "The length of the password is incorrect size during the encryption. " +
                    "Try again later or contact administrator by providing the logs in " +
                    TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        }
        return null;
    }
}
