package com.famora.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * The type Password security.
 */
@UtilityClass
@Slf4j
public class PasswordSecurity {
  
  private static final String ALGORITHM = "MD5";
  private static final byte[] DEFAULT_SALT = {(byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
      (byte) 0x56, (byte) 0x34, (byte) 0xE3, (byte) 0x03};
  
  /**
   * Encrypt password string.
   *
   * @param password   the password
   * @param customSalt the custom salt
   * @return the string
   * @throws NoSuchAlgorithmException the no such algorithm exception
   */
  public static String encryptPassword(
      String password, String customSalt) throws NoSuchAlgorithmException {
    byte[] saltedPassword = bindSaltedPassword(password, customSalt.getBytes());
    
    MessageDigest md = MessageDigest.getInstance(ALGORITHM);
    
    byte[] digest = md.digest(saltedPassword);
    
    return Base64.getEncoder().encodeToString(digest);
  }
  
  private static byte[] bindSaltedPassword(String password, byte[] customSaltBytes) {
    byte[] saltedPassword = new byte[password.length() + customSaltBytes.length
        + DEFAULT_SALT.length];
    System.arraycopy(password.getBytes(), 0, saltedPassword, 0, password.length());
    System.arraycopy(customSaltBytes, 0, saltedPassword, password.length(),
        customSaltBytes.length);
    System.arraycopy(DEFAULT_SALT, 0, saltedPassword,
        password.length() + customSaltBytes.length,
        DEFAULT_SALT.length);
    return saltedPassword;
  }
  
  /**
   * Check passwords match boolean.
   *
   * @param password   the password
   * @param storedHash the stored hash
   * @param customSalt the custom salt
   * @return the boolean
   */
  public static boolean checkPasswordsMatch(String password, String storedHash,
      String customSalt) {
    try {
      byte[] saltedPassword = bindSaltedPassword(password, customSalt.getBytes());
      
      MessageDigest md = MessageDigest.getInstance(ALGORITHM);
      byte[] digest = md.digest(saltedPassword);
      
      String encryptedPassword = Base64.getEncoder().encodeToString(digest);
      return encryptedPassword.equals(storedHash);
    } catch (NoSuchAlgorithmException nsa) {
      log.error(nsa.getMessage());
      return false;
    }
  }
}
