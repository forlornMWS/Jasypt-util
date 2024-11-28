package xyz.mwszksnmdys.demoplugin.util;

import com.intellij.openapi.project.Project;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YmlProcessor {
    private static final String ENC_REGEX = "ENC\\((.*?)\\)";
    private static final Pattern ENC_PATTERN = Pattern.compile(ENC_REGEX);
    private static final String VARIABLE_REGEX = "\\$\\{(.*?)\\}";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_REGEX);
    private static final Logger logger = LoggerFactory.getLogger(YmlProcessor.class);

    public static void processYmlFiles(Project project) {
        // 获取项目的根路径
        Path projectPath = Paths.get(Objects.requireNonNull(project.getBasePath()));

        if (!Files.exists(projectPath)) {
            logger.error("获取项目路径失败");
            JOptionPane.showMessageDialog(null, "未找到项目路径", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 遍历项目中的所有子目录
            Files.walk(projectPath)
                    .filter(Files::isDirectory) // 筛选出目录
                    .filter(path -> path.endsWith("src/main/resources")) // 筛选 resources 目录
                    .forEach(resourcesPath -> {
                        try {
                            // 遍历每个 resources 目录中的 .yml 文件
                            Files.walk(resourcesPath)
                                    .filter(path -> path.toString().endsWith(".yml"))
                                    .forEach(ymlFile -> processSingleYmlFile(ymlFile));
                        } catch (IOException e) {
                            logger.error("处理资源目录中的YML文件时出错：{}", resourcesPath, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("处理模块中的YML文件时出错：", e);
            JOptionPane.showMessageDialog(null, "处理模块中的YML文件出错", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    //  从 yaml 文件中读取 jasypt 配置并创建 encryptor 解密
    public static void processSingleYmlFile(Path ymlPath) {
        try (InputStream inputStream = Files.newInputStream(ymlPath)) {
            // 使用 LoaderOptions 初始化 Yaml
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);

            // 加载 YAML 文件内容到 Map
            Map<String, Object> yamlContent = yaml.load(inputStream);

            if (yamlContent == null || !yamlContent.containsKey("jasypt")) {
                logger.error("未找到 Jasypt 配置: {}", ymlPath);
                return;
            }

            PooledPBEStringEncryptor encryptor = getEncryptor((Map<String, Object>) yamlContent.get("jasypt"));

            // 遍历文件内容，查找并解密 ENC()
            boolean hasEncryptedContent = false;
            String originalContent = Files.readString(ymlPath);
            StringBuilder decryptedContent = new StringBuilder();

            for (String line : originalContent.split("\n")) {
                Matcher matcher = ENC_PATTERN.matcher(line);
                String decryptedLine = line;

                while (matcher.find()) {
                    hasEncryptedContent = true;
                    String encryptedValue = matcher.group(1);
                    String decryptedValue = encryptor.decrypt(encryptedValue);
                    decryptedLine = decryptedLine.replace(matcher.group(0), decryptedValue);
                }

                decryptedContent.append(decryptedLine).append("\n");
            }

            // 如果没有加密内容，跳过生成备份文件
            if (!hasEncryptedContent) {
                logger.error("没有加密的内容 ENC() {}", ymlPath);
                return;
            }

            // 保存解密后的文件为 -bak.yml
            Path bakPath = Paths.get(ymlPath.toString().replace(".yml", "-bak.yml"));
            Files.writeString(bakPath, decryptedContent.toString());
            logger.info("解密的yml文件已创建: {}", bakPath);

        } catch (Exception e) {
            logger.error("Yaml文件解密失败 ", e);
            JOptionPane.showMessageDialog(null, "Yaml文件解密失败", "Error", JOptionPane.ERROR_MESSAGE);

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
            // 检查是否是环境变量占位符
            if (VARIABLE_PATTERN.matcher(password).matches()) {
                // 解析默认值和环境变量名
                String envKey = password.substring(2, password.indexOf('}'));
                String defaultValue = null;

                if (password.contains(":")) {
                    defaultValue = password.substring(password.indexOf(':') + 1, password.length() - 1);
                    envKey = envKey.substring(0, envKey.indexOf(':'));
                }

                // 优先从环境变量读取
                password = System.getenv(envKey);

                // 如果环境变量未设置,使用默认值
                if (password == null && defaultValue != null) {
                    password = defaultValue;
                }

                // 如果环境变量和默认值都为空,抛出异常
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
            // 加载 SaltGenerator
            Class<?> saltGenClass = Class.forName(saltGenClsName, true, YmlProcessor.class.getClassLoader());
            config.setSaltGenerator((org.jasypt.salt.SaltGenerator) saltGenClass.getDeclaredConstructor().newInstance());

            // 加载 IV Generator
            if (ivGenClsName != null) {
                Class<?> ivGenClass = Class.forName(ivGenClsName, true, YmlProcessor.class.getClassLoader());
                config.setIvGenerator((org.jasypt.iv.IvGenerator) ivGenClass.getDeclaredConstructor().newInstance());
            } else {
                // 默认处理 AES 算法的 IV 生成器
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
