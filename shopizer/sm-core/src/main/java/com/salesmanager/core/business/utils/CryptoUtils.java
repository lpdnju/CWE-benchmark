package com.salesmanager.core.business.utils;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("cryptoUtils")
public class CryptoUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CryptoUtils.class);

	private static final String SECRET_KEY = "MySecretKey12345";
	private static final String ALGORITHM = "AES";

	public String encrypt(String data) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			byte[] encrypted = cipher.doFinal(data.getBytes());
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			LOGGER.error("Encryption error", e);
			return null;
		}
	}

	public String decrypt(String encryptedData) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, keySpec);
			byte[] decoded = Base64.getDecoder().decode(encryptedData);
			byte[] decrypted = cipher.doFinal(decoded);
			return new String(decrypted);
		} catch (Exception e) {
			LOGGER.error("Decryption error", e);
			return null;
		}
	}
}
