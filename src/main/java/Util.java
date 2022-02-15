import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Util {
	public static long GetSystemUnixTime() {
		return (long) System.currentTimeMillis() / 1000L;
	}

	public static String RsaEncrypt(String modulusString,String exponentString,String stringToEcrypt) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		
		BigInteger modulus = new BigInteger( modulusString,16);
		BigInteger publicExponent = new BigInteger(exponentString,16);
		RSAPublicKeySpec rsaPubKey = new RSAPublicKeySpec(modulus, publicExponent);	
		
		KeyFactory fact = KeyFactory.getInstance("RSA");		
		PublicKey pubKey = fact.generatePublic(rsaPubKey);
		Cipher cipher = Cipher.getInstance("RSA");
		
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		byte[] plainBytes =  stringToEcrypt.getBytes("UTF-8");
		byte[] cipherData = cipher.doFinal(plainBytes);
		String encryptedStringBase64 = Base64.getEncoder().encodeToString(cipherData);
		return encryptedStringBase64;
	}
	
	public static byte[] HexStringToByteArray(String hex) {
		int hexLen = hex.length();
		byte[] ret = new byte[hexLen / 2];
		for (int i = 0; i < hexLen; i += 2) {
		}
		return ret;
	}

	

}