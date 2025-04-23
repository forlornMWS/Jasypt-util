package xyz.mwszksnmdys.demoplugin.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YmlProcessor {
    private static final String ENC_REGEX = "ENC\\((.*?)\\)";
    private static final Pattern ENC_PATTERN = Pattern.compile(ENC_REGEX);
    private static final Logger logger = LoggerFactory.getLogger(YmlProcessor.class);
    private static final String DEFAULT_CONFIG_FILENAME = "application.yml";

    private static boolean isYamlFile(Path path) {
        return path.toString().endsWith(".yml") || path.toString().endsWith(".yaml");
    }

    public static void processYmlFileOrDirectory(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .filter(YmlProcessor::isYamlFile)
                        .forEach(YmlProcessor::processSingleYmlFile);
            } else if (isYamlFile(path)) {
                processSingleYmlFile(path);
            } else {
                logger.error("Selected file is not a YML file: {}", path);
                throw new RuntimeException("Selected file is not a YML file: " + path);
            }
        } catch (Exception e) {
            logger.error("Error processing path: {}", path, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void processSingleYmlFile(Path ymlPath) {
        try {
            String content = FileUtil.readFile(ymlPath);
            // Get jasypt configuration, either from the current file or from application.yml
            Map<String, Object> jasyptConfig = getJasyptConfig(ymlPath);

            if (jasyptConfig == null) {
                logger.error("No Jasypt configuration found for: {}", ymlPath);
                return;
            }

            PooledPBEStringEncryptor encryptor = JasyptEncryptor.getEncryptor(jasyptConfig);
            Matcher matcher = ENC_PATTERN.matcher(content);

            if (!matcher.find()) {
                logger.error("No ENC() content found in {}", ymlPath);
                return;
            }

            matcher.reset();
            StringBuilder processedContent = new StringBuilder();

            while (matcher.find()) {
                String encValue = matcher.group(1);
                String replacement;

                try {
                    // Try to decrypt
                    replacement = JasyptEncryptor.decrypt(encryptor, encValue);
                } catch (EncryptionOperationNotPossibleException e) {
                    // 解密失败，进行加密
                    replacement = "ENC(" + JasyptEncryptor.encrypt(encryptor,encValue) + ")";
                }catch (Exception e) {
                    logger.error("Error processing property:", e);
                    throw e;
                }

                matcher.appendReplacement(processedContent, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(processedContent);

            FileUtil.writeFile(ymlPath, processedContent.toString());
            logger.info("File processed successfully: {}", ymlPath);

        } catch (Exception e) {
            logger.error("Failed to process YAML file: {}", ymlPath, e);
        }
    }

    public static Map<String, Object> getJasyptConfig(Path ymlPath) {
        try {
            // First try to get config from the current file
            Map<String, Object> yamlContent = FileUtil.loadYamlFile(ymlPath);

            if (yamlContent != null && yamlContent.containsKey("jasypt")) {
                return (Map<String, Object>) yamlContent.get("jasypt");
            }

            // If not found, try to get from application.yml in the same directory
            Path parentDir = ymlPath.getParent();
            Path defaultConfigPath = parentDir.resolve(DEFAULT_CONFIG_FILENAME);

            if (Files.exists(defaultConfigPath)) {
                Map<String, Object> defaultConfig = FileUtil.loadYamlFile(defaultConfigPath);
                if (defaultConfig != null && defaultConfig.containsKey("jasypt")) {
                    logger.info("Using jasypt configuration from {}", DEFAULT_CONFIG_FILENAME);
                    return (Map<String, Object>) defaultConfig.get("jasypt");
                }
            }

            throw new RuntimeException("Error loading jasypt configuration: ");
        } catch (Exception e) {
            logger.error("Error loading jasypt configuration for file: {}", ymlPath, e);
            throw new RuntimeException("Error loading jasypt configuration: " + e.getMessage(), e);
        }
    }
}