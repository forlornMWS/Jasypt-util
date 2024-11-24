package xyz.mwszksnmdys.demoplugin.util;

import com.intellij.openapi.project.Project;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

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
    private static final String VARIABLE_REGEX = "\\$\\{(.*?)\\}";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_REGEX);
    private static final Pattern ENC_PATTERN = Pattern.compile(ENC_REGEX);
    private static Logger logger = LoggerFactory.getLogger(YmlProcessor.class);

    public static void processYmlFiles(Project project, PooledPBEStringEncryptor encryptor) {
        // 获取项目的根路径
        Path projectPath = Paths.get(Objects.requireNonNull(project.getBasePath()));

        if (!Files.exists(projectPath)) {
            System.out.println("Project directory not found.");
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
                                    .forEach(ymlFile -> processSingleYmlFile(ymlFile, encryptor));
                        } catch (IOException e) {
                            logger.error("处理资源目录中的YML文件时出错：{}", resourcesPath, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("处理模块中的YML文件时出错：", e);
            throw new RuntimeException(e);
        }
    }


    // todo 支持从 yaml 文件中读取 jasypt 配置并创建 encryptor
    public static void processSingleYmlFile(Path ymlPath, PooledPBEStringEncryptor encryptor) {
        try (InputStream inputStream = Files.newInputStream(ymlPath)) {
            // 使用 LoaderOptions 初始化 Yaml
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);

            // 加载 YAML 文件内容到 Map
            Map<String, Object> yamlContent = yaml.load(inputStream);

            if (yamlContent == null || !yamlContent.containsKey("jasypt")) {
                System.out.println("No Jasypt configuration found in: " + ymlPath);
                return;
            }

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
                System.out.println("No ENC() content found in: " + ymlPath);
                return;
            }

            // 保存解密后的文件为 -bak.yml
            Path bakPath = Paths.get(ymlPath.toString().replace(".yml", "-bak.yml"));
            Files.writeString(bakPath, decryptedContent.toString());
            System.out.println("Decrypted file created: " + bakPath);

        } catch (Exception e) {
            logger.error("processSingleYmlFile error ", e);
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    private  static PooledPBEStringEncryptor getEncryptor(Map<String, Object> jasyptConfig) {
        Object encryptorConfig = jasyptConfig.get("encryptor");
        if (encryptorConfig == null) {
            throw new RuntimeException("jasypt 配置错误");
        }
        Map<String, Object> encryptorConfigMap = (Map<String, Object>) encryptorConfig;
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        String password = (String) encryptorConfigMap.get("password");
        String algorithm = (String) encryptorConfigMap.getOrDefault("algorithm", "PBEWithHMACSHA512AndAES_256");
        String saltGenClsName = (String) encryptorConfigMap.getOrDefault("salt-generator-classname", "org.jasypt.salt.RandomSaltGenerator");
        String ivGenClsName = (String) encryptorConfigMap.get("iv-generator-classname");

        if (password == null) {
            throw new RuntimeException("yml文件读取密钥为空");
        }

        config.setPassword(password);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");

        System.out.println("Current Thread Context ClassLoader: " + Thread.currentThread().getContextClassLoader());
        System.out.println("Current Class's ClassLoader: " +  YmlProcessor.class.getClassLoader());
        System.out.println("RandomSaltGenerator ClassLoader: " + org.jasypt.salt.RandomSaltGenerator.class.getClassLoader());
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
