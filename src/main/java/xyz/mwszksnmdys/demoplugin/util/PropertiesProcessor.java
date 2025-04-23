package xyz.mwszksnmdys.demoplugin.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Properties文件处理工具类
 */
public class PropertiesProcessor {
    private static final String ENC_REGEX = "ENC\\((.*?)\\)";
    private static final Pattern ENC_PATTERN = Pattern.compile(ENC_REGEX);
    private static final Logger logger = LoggerFactory.getLogger(PropertiesProcessor.class);
    private static final String DEFAULT_CONFIG_FILENAME = "application.properties";
    private static final String YML_CONFIG_FILENAME = "application.yml";

    /**
     * 处理Properties文件或目录
     * @param path 文件或目录路径
     */
    public static void processPropertiesFile(Path path) {
        try {
            if (Files.isDirectory(path)) {
                processPropertiesDirectory(path);
            } else if (isPropertiesFile(path)) {
                processSinglePropertiesFile(path);
            } else {
                logger.error("Selected file is not a Properties file: {}", path);
                JOptionPane.showMessageDialog(null,
                        "Selected file is not a Properties file: " + path,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException("Selected file is not a Properties file: " + path);
            }
        } catch (Exception e) {
            logger.error("Error processing path: {}", path, e);
            JOptionPane.showMessageDialog(null,
                    "Error processing path: " + path,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e.getMessage(), e);
        }
        JOptionPane.showMessageDialog(null, "操作成功.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 处理目录中的所有Properties文件
     * @param directory 目录路径
     * @throws IOException IO异常
     */
    public static void processPropertiesDirectory(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(PropertiesProcessor::isPropertiesFile)
                    .forEach(PropertiesProcessor::processSinglePropertiesFile);
        } catch (Exception e) {
            logger.error("Error processing directory: {}", directory, e);
            throw e;
        }
    }

    /**
     * 判断文件是否为Properties文件
     * @param path 文件路径
     * @return 是否为Properties文件
     */
    private static boolean isPropertiesFile(Path path) {
        return !Files.isDirectory(path) &&
                path.toString().toLowerCase().endsWith(".properties");
    }

    /**
     * 处理单个Properties文件
     * @param propertiesPath Properties文件路径
     */
    public static void processSinglePropertiesFile(Path propertiesPath) {
        try {

            // 使用 ReadAction 确保在正确的线程中读取文件内容
            String content = FileUtil.readFile(propertiesPath);

            Map<String, Object> jasyptConfig = getJasyptConfig(propertiesPath);

            if (jasyptConfig == null) {
                logger.error("No Jasypt configuration found for: {}", propertiesPath);
                return;
            }

            PooledPBEStringEncryptor encryptor = JasyptEncryptor.getEncryptor(jasyptConfig);

            StringBuilder processedContentBuilder = new StringBuilder();
            boolean hasEncValues = false;
            int lastEnd = 0;

            // 使用正则匹配所有ENC()值
            Matcher matcher = ENC_PATTERN.matcher(content);
            while (matcher.find()) {
                hasEncValues = true;
                // 添加ENC()前的内容
                processedContentBuilder.append(content, lastEnd, matcher.start());

                String encValue = matcher.group(1);
                String replacement;

                try {
                    // 尝试解密
                    replacement = JasyptEncryptor.decrypt(encryptor, encValue);
                    // 解密成功，直接使用解密后的值
                    processedContentBuilder.append(replacement);
                } catch (EncryptionOperationNotPossibleException e) {
                    // 解密失败，说明是需要加密的值
                    replacement = "ENC(" + encryptor.encrypt(encValue) + ")";
                    processedContentBuilder.append(replacement);
                } catch (Exception e) {
                    logger.error("Error while processing value: {}", encValue, e);
                    throw e;
                }

                lastEnd = matcher.end();
            }

            if (!hasEncValues) {
                logger.info("No ENC() content found in {}", propertiesPath);
                return;
            }

            // 添加剩余的内容
            processedContentBuilder.append(content.substring(lastEnd));

            FileUtil.writeFile(propertiesPath, processedContentBuilder.toString());
            logger.info("File processed successfully: {}", propertiesPath);

        } catch (Exception e) {
            logger.error("Failed to process Properties file: {}", propertiesPath, e);
            JOptionPane.showMessageDialog(null,
                    "Failed to process Properties file: " + propertiesPath,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e.getMessage(), e);
        }
    }



    /**
     * 获取Jasypt配置
     * @param propertiesPath Properties文件路径
     * @return Jasypt配置Map
     */
    private static Map<String, Object> getJasyptConfig(Path propertiesPath) {
        try {
            // 首先尝试从当前properties文件获取配置
            Properties currentProperties = loadPropertiesFile(propertiesPath);
            Map<String, Object> jasyptConfig = convertPropertiesToJasyptConfig(currentProperties);

            if (jasyptConfig != null) {
                return jasyptConfig;
            }

            // 如果当前文件没有配置，尝试从同目录下的application.properties获取
            Path parentDir = propertiesPath.getParent();
            Path defaultPropertiesPath = parentDir.resolve(DEFAULT_CONFIG_FILENAME);

            if (Files.exists(defaultPropertiesPath)) {
                Properties defaultProperties = loadPropertiesFile(defaultPropertiesPath);
                jasyptConfig = convertPropertiesToJasyptConfig(defaultProperties);

                if (jasyptConfig != null) {
                    logger.info("Using jasypt configuration from application.properties");
                    return jasyptConfig;
                }
            }

            // 如果properties文件中没有找到配置，尝试从application.yml获取
            Path ymlConfigPath = parentDir.resolve(YML_CONFIG_FILENAME);

            if (Files.exists(ymlConfigPath)) {
                Map<String, Object> ymlConfig = FileUtil.loadYamlFile(ymlConfigPath);

                if (ymlConfig != null && ymlConfig.containsKey("jasypt")) {
                    logger.info("Using jasypt configuration from {}", YML_CONFIG_FILENAME);
                    return (Map<String, Object>) ymlConfig.get("jasypt");
                }
            }

            throw new RuntimeException("Error loading jasypt configuration: ");
        } catch (Exception e) {
            logger.error("Error loading jasypt configuration for file: {}", propertiesPath, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 加载Properties文件
     * @param path 文件路径
     * @return Properties对象
     * @throws IOException IO异常
     */
    private static Properties loadPropertiesFile(Path path) throws IOException {
        String content = FileUtil.readFile(path);
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        return properties;
    }

    /**
     * 将Properties转换为Jasypt配置Map
     * @param properties Properties对象
     * @return Jasypt配置Map，如果没有配置则返回null
     */
    private static Map<String, Object> convertPropertiesToJasyptConfig(Properties properties) {
        // 检查是否包含jasypt.encryptor.password属性
        if (!properties.containsKey("jasypt.encryptor.password")) {
            return null;
        }

        // 创建jasypt配置结构
        java.util.HashMap<String, Object> jasyptConfig = new java.util.HashMap<>();
        java.util.HashMap<String, Object> encryptorConfig = new java.util.HashMap<>();
        jasyptConfig.put("encryptor", encryptorConfig);

        // 解析所有jasypt.encryptor.*属性
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("jasypt.encryptor.")) {
                String configKey = key.substring("jasypt.encryptor.".length());
                encryptorConfig.put(configKey, properties.getProperty(key));
            }
        }

        return jasyptConfig;
    }
}