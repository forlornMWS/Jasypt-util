package xyz.mwszksnmdys.demoplugin.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YmlProcessor {
    private static final String ENC_REGEX = "ENC\\((.*?)\\)";
    private static final Pattern ENC_PATTERN = Pattern.compile(ENC_REGEX);
    private static final String VARIABLE_REGEX = "\\$\\{(.*?)\\}";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_REGEX);
    private static final Logger logger = LoggerFactory.getLogger(YmlProcessor.class);

    public static void processYmlFiles(Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            try {
                VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();

                for (VirtualFile contentRoot : contentRoots) {
                    Path modulePath = Paths.get(contentRoot.getPath());

                    Files.walk(modulePath)
                            .filter(Files::isDirectory)
                            .filter(path -> path.endsWith("src/main/resources"))
                            .forEach(resourcesPath -> {
                                try {
                                    Files.walk(resourcesPath)
                                            .filter(path -> path.toString().endsWith(".yml"))
                                            .forEach(YmlProcessor::processSingleYmlFile);
                                } catch (IOException e) {
                                    logger.error("Error processing YML files in resources directory: {}", resourcesPath, e);
                                }
                            });
                }
            } catch (IOException e) {
                logger.error("Error processing module: {}", module.getName(), e);
                JOptionPane.showMessageDialog(null,
                        "Error processing module: " + module.getName(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void processYmlFileOrDirectory(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .filter(p -> p.toString().endsWith(".yml"))
                        .forEach(YmlProcessor::processSingleYmlFile);
                JOptionPane.showMessageDialog(null, "操作成功.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else if (path.toString().endsWith(".yml")) {
                processSingleYmlFile(path);
                JOptionPane.showMessageDialog(null, "操作成功.", "Success", JOptionPane.INFORMATION_MESSAGE);

            } else {
                logger.error("Selected file is not a YML file: {}", path);
                JOptionPane.showMessageDialog(null,
                        "Selected file is not a YML file: " + path,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            logger.error("Error processing path: {}", path, e);
            JOptionPane.showMessageDialog(null,
                    "Error processing path: " + path,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void processSingleYmlFile(Path ymlPath) {
        try (InputStream inputStream = Files.newInputStream(ymlPath)) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);
            Map<String, Object> yamlContent = yaml.load(inputStream);

            if (yamlContent == null || !yamlContent.containsKey("jasypt")) {
                logger.error("No Jasypt configuration found: {}", ymlPath);
                return;
            }

            PooledPBEStringEncryptor encryptor = getEncryptor((Map<String, Object>) yamlContent.get("jasypt"));
            String content = Files.readString(ymlPath);
            Matcher matcher = ENC_PATTERN.matcher(content);

            if (!matcher.find()) {
                logger.error("No ENC() content found in {}", ymlPath);
                return;
            }

            matcher.reset();
            StringBuffer processedContent = new StringBuffer();

            while (matcher.find()) {
                String encValue = matcher.group(1);
                String replacement;

                try {
                    // Try to decrypt
                    replacement = encryptor.decrypt(encValue);
                } catch (Exception e) {
                    // If decryption fails, encrypt the content
                    replacement = "ENC(" + encryptor.encrypt(encValue) + ")";
                }

                matcher.appendReplacement(processedContent, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(processedContent);

            Files.writeString(ymlPath, processedContent.toString());
            logger.info("File processed successfully: {}", ymlPath);

        } catch (Exception e) {
            logger.error("Failed to process YAML file", e);
            JOptionPane.showMessageDialog(null, "Failed to process YAML file", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static PooledPBEStringEncryptor getEncryptor(Map<String, Object> jasyptConfig) {
        Object encryptorConfig = jasyptConfig.get("encryptor");
        if (encryptorConfig == null) {
            JOptionPane.showMessageDialog(null, "Jasypt配置错误", "Error", JOptionPane.ERROR_MESSAGE);
        }
        Map<String, Object> encryptorConfigMap = (Map<String, Object>) encryptorConfig;
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        String password = (String) encryptorConfigMap.get("password");
        String algorithm = (String) encryptorConfigMap.getOrDefault("algorithm", "PBEWithHMACSHA512AndAES_256");
        String saltGenClsName = (String) encryptorConfigMap.getOrDefault("salt-generator-classname", "org.jasypt.salt.RandomSaltGenerator");
        String ivGenClsName = (String) encryptorConfigMap.get("iv-generator-classname");

        if (password == null) {
            JOptionPane.showMessageDialog(null, "yml文件读取密钥为空", "Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("yml文件读取密钥为空");
        } else {
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
                    JOptionPane.showMessageDialog(null, "环境变量 " + envKey + " 未设置且无默认值", "Error", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException("环境变量 " + envKey + " 未设置且无默认值");
                }
            }
        }

        config.setPassword(password);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");

        try {
            Class<?> saltGenClass = Class.forName(saltGenClsName, true, YmlProcessor.class.getClassLoader());
            config.setSaltGenerator((org.jasypt.salt.SaltGenerator) saltGenClass.getDeclaredConstructor().newInstance());

            if (ivGenClsName != null) {
                Class<?> ivGenClass = Class.forName(ivGenClsName, true, YmlProcessor.class.getClassLoader());
                config.setIvGenerator((org.jasypt.iv.IvGenerator) ivGenClass.getDeclaredConstructor().newInstance());
            } else {
                if (algorithm.contains("AES")) {
                    config.setIvGenerator(new org.jasypt.iv.RandomIvGenerator());
                } else {
                    config.setIvGenerator(new org.jasypt.iv.NoIvGenerator());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("加载 SaltGenerator 或 IvGenerator 失败", e);
        }

        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}