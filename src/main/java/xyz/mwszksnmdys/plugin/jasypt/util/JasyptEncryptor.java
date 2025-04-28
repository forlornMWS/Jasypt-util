package xyz.mwszksnmdys.plugin.jasypt.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;

import javax.swing.*;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Jasypt加密解密工具类
 */
public class JasyptEncryptor {
    private static final Logger logger = LoggerFactory.getLogger(JasyptEncryptor.class);
    private static final String VARIABLE_REGEX = "\\$\\{(.*?)}";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_REGEX);
    
    /**
     * 获取Jasypt加密器
     * @param jasyptConfig Jasypt配置
     * @return PooledPBEStringEncryptor实例
     */
    public static PooledPBEStringEncryptor getEncryptor(Map<String, Object> jasyptConfig) {
        Object encryptorConfig = jasyptConfig.get("encryptor");
        Map<String, Object> encryptorConfigMap = (Map<String, Object>) encryptorConfig;
        if (encryptorConfig == null || encryptorConfigMap.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.configuration"), "Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Jasypt Configuration Error");
        }
        
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        String password = (String) encryptorConfigMap.get("password");
        String algorithm = (String) encryptorConfigMap.getOrDefault("algorithm", "PBEWithHMACSHA512AndAES_256");
        String saltGenClsName = (String) encryptorConfigMap.getOrDefault("salt-generator-classname", "org.jasypt.salt.RandomSaltGenerator");
        String ivGenClsName = (String) encryptorConfigMap.get("iv-generator-classname");

        if (password == null) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.configuration.readPassword.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Password is null");
        } else {
            password = parsePasswordFromEnvironment(password);
        }

        config.setPassword(password);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");

        try {
            Class<?> saltGenClass = Class.forName(saltGenClsName, true, JasyptEncryptor.class.getClassLoader());
            config.setSaltGenerator((org.jasypt.salt.SaltGenerator) saltGenClass.getDeclaredConstructor().newInstance());

            if (ivGenClsName != null) {
                Class<?> ivGenClass = Class.forName(ivGenClsName, true, JasyptEncryptor.class.getClassLoader());
                config.setIvGenerator((org.jasypt.iv.IvGenerator) ivGenClass.getDeclaredConstructor().newInstance());
            } else {
                if (algorithm.contains("AES")) {
                    config.setIvGenerator(new org.jasypt.iv.RandomIvGenerator());
                } else {
                    config.setIvGenerator(new org.jasypt.iv.NoIvGenerator());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Loading SaltGenerator or IvGenerator fail", e);
        }

        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    public static @NotNull String parsePasswordFromEnvironment(String password) {
        if (VARIABLE_PATTERN.matcher(password).matches()) {
            String envKey = password.substring(2, password.indexOf('}'));
            String defaultValue = null;

            if (password.contains(":")) {
                defaultValue = password.substring(password.indexOf(':') + 1, password.length() - 1);
                envKey = envKey.substring(0, envKey.indexOf(':'));
            }

            password = System.getenv(envKey);

            if (password == null && defaultValue != null) {
                password = defaultValue;
            }

            if (password == null) {
                JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.configuration.env.empty", envKey), "Error", JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException("Environment value " + envKey + " not configured and no default value");
            }
        }
        return password;
    }


    /**
     * 加密字符串
     * @param encryptor Jasypt加密器
     * @param value 原始字符串
     * @return 加密后的字符串
     */
    public static String encrypt(PooledPBEStringEncryptor encryptor, String value) {
        return encryptor.encrypt(value);
    }
    
    /**
     * 解密字符串
     * @param encryptor Jasypt加密器
     * @param encryptedValue 加密的字符串
     * @return 解密后的字符串
     */
    public static String decrypt(PooledPBEStringEncryptor encryptor, String encryptedValue) {
        return encryptor.decrypt(encryptedValue);
    }
}