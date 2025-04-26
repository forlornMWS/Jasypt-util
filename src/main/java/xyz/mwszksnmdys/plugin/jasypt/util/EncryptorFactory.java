package xyz.mwszksnmdys.plugin.jasypt.util;

import org.jasypt.encryption.pbe.PooledPBEByteEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.iv.IvGenerator;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.salt.SaltGenerator;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.StrongTextEncryptor;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Jasypt 加密工具工厂类
 * 提供多种类型的加密器支持，包括 PBE、标准对称加密、摘要和非对称加密
 */
public class EncryptorFactory {

    /**
     * 创建 PBE 字符串加密器
     * 
     * @param key 密钥
     * @param algorithm PBE算法名称
     * @param iterations 密钥获取迭代次数，默认1000
     * @param poolSize 池大小，默认1
     * @param outputType 输出类型，默认base64
     * @return 配置好的 PBE 加密器，或配置失败时返回 null
     */
    public static PooledPBEStringEncryptor createPBEEncryptor(
            String key, 
            String algorithm, 
            Integer iterations, 
            Integer poolSize, 
            String outputType) {
            
        if (key == null || key.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.password.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.algorithm.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        config.setPassword(key);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations(String.valueOf(iterations != null ? iterations : 1000));
        config.setPoolSize(String.valueOf(poolSize != null ? poolSize : 1));
        config.setSaltGenerator(new RandomSaltGenerator());
        
        // 根据算法类型选择合适的IV生成器
        if (algorithm.contains("AES")) {
            config.setIvGenerator(new RandomIvGenerator());
        } else {
            config.setIvGenerator(new NoIvGenerator());
        }
        
        config.setStringOutputType(outputType != null ? outputType : "base64");
        encryptor.setConfig(config);
        
        return encryptor;
    }
    
    /**
     * 创建高级PBE加密器，可自定义盐和IV生成器
     * 
     * @param key 密钥
     * @param algorithm 算法名称
     * @param iterations 密钥获取迭代次数
     * @param poolSize 池大小
     * @param saltGenerator 盐生成器
     * @param ivGenerator IV生成器
     * @param outputType 输出类型
     * @return 配置好的高级PBE加密器
     */
    public static PooledPBEStringEncryptor createAdvancedPBEEncryptor(
            String key, 
            String algorithm, 
            Integer iterations, 
            Integer poolSize,
            SaltGenerator saltGenerator,
            IvGenerator ivGenerator,
            String outputType) {
            
        if (key == null || key.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.password.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.algorithm.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        config.setPassword(key);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations(String.valueOf(iterations != null ? iterations : 1000));
        config.setPoolSize(String.valueOf(poolSize != null ? poolSize : 1));
        
        // 使用自定义盐生成器或默认随机盐生成器
        config.setSaltGenerator(saltGenerator != null ? saltGenerator : new RandomSaltGenerator());
        
        // 使用自定义IV生成器或根据算法选择
        if (ivGenerator != null) {
            config.setIvGenerator(ivGenerator);
        } else if (algorithm.contains("AES")) {
            config.setIvGenerator(new RandomIvGenerator());
        } else {
            config.setIvGenerator(new NoIvGenerator());
        }
        
        config.setStringOutputType(outputType != null ? outputType : "base64");
        encryptor.setConfig(config);
        
        return encryptor;
    }
    
    /**
     * 创建标准的强文本加密器（使用AES算法）
     * 
     * @param key 密钥
     * @return 标准AES加密器
     */
    public static StrongTextEncryptor createStandardEncryptor(String key) {
        if (key == null || key.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.password.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        StrongTextEncryptor encryptor = new StrongTextEncryptor();
        encryptor.setPassword(key);
        return encryptor;
    }
    
    /**
     * 创建基本文本加密器（使用PBEWithMD5AndDES算法）
     * 
     * @param key 密钥
     * @return 基本文本加密器
     */
    public static BasicTextEncryptor createBasicEncryptor(String key) {
        if (key == null || key.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.password.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(key);
        return encryptor;
    }
    

    
    /**
     * 创建不使用盐的简单摘要
     * 
     * @param algorithm 摘要算法名称
     * @return 摘要结果
     */
    public static String createSimpleDigest(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm); // "MD5", "SHA-256" 等
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.algorithm.unsupported", algorithm), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    /**
     * 创建用于大二进制数据的加密器
     * 
     * @param key 密钥
     * @param algorithm PBE算法名称
     * @return 二进制数据加密器
     */
    public static PooledPBEByteEncryptor createByteEncryptor(String key, String algorithm) {
        if (key == null || key.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.password.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("encryptor.error.algorithm.empty"), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        PooledPBEByteEncryptor encryptor = new PooledPBEByteEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        config.setPassword(key);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setSaltGenerator(new RandomSaltGenerator());
        
        if (algorithm.contains("AES")) {
            config.setIvGenerator(new RandomIvGenerator());
        } else {
            config.setIvGenerator(new NoIvGenerator());
        }
        
        encryptor.setConfig(config);
        return encryptor;
    }
    
    /**
     * 获取所有支持的PBE算法列表
     * 
     * @return 支持的PBE算法名称列表
     */
    public static List<String> getSupportedPBEAlgorithms() {
        List<String> algorithms = new ArrayList<>();
        
        // 基于密码的加密算法
        algorithms.add("PBEWithMD5AndDES");
        algorithms.add("PBEWithMD5AndTripleDES");
        algorithms.add("PBEWithSHA1AndDESede");
        algorithms.add("PBEWithSHA1AndRC2_40");
        algorithms.add("PBEWithSHA1AndRC2_128");
        algorithms.add("PBEWithSHA1AndRC4_40");
        algorithms.add("PBEWithSHA1AndRC4_128");
        algorithms.add("PBEWithHMACSHA1AndAES_128");
        algorithms.add("PBEWithHMACSHA1AndAES_256");
        algorithms.add("PBEWithHMACSHA224AndAES_128");
        algorithms.add("PBEWithHMACSHA224AndAES_256");
        algorithms.add("PBEWithHMACSHA256AndAES_128");
        algorithms.add("PBEWithHMACSHA256AndAES_256");
        algorithms.add("PBEWithHMACSHA384AndAES_128");
        algorithms.add("PBEWithHMACSHA384AndAES_256");
        algorithms.add("PBEWithHMACSHA512AndAES_128");
        algorithms.add("PBEWithHMACSHA512AndAES_256");
        
        return algorithms;
    }
    
    /**
     * 获取所有支持的摘要算法列表
     * 
     * @return 支持的摘要算法名称列表
     */
    public static List<String> getSupportedDigestAlgorithms() {
        List<String> algorithms = new ArrayList<>();
        
        // 摘要算法
        algorithms.add("MD5");
        algorithms.add("SHA-1");
        algorithms.add("SHA-256");
        algorithms.add("SHA-384");
        algorithms.add("SHA-512");
        
        return algorithms;
    }
    
    /**
     * 测试指定的PBE算法是否在当前JVM中可用
     * 
     * @param algorithm 要测试的算法名称
     * @return 是否可用
     */
    public static boolean isPBEAlgorithmAvailable(String algorithm) {
        try {
            PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
            SimpleStringPBEConfig config = new SimpleStringPBEConfig();
            config.setPassword("test");
            config.setAlgorithm(algorithm);
            encryptor.setConfig(config);
            
            // 尝试加密一些数据来验证算法是否可用
            String encrypted = encryptor.encrypt("test");
            encryptor.decrypt(encrypted);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}