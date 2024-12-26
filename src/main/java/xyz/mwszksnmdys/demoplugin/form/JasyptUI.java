package xyz.mwszksnmdys.demoplugin.form;

import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.salt.RandomSaltGenerator;
import org.jetbrains.annotations.Nullable;
import xyz.mwszksnmdys.demoplugin.util.YmlProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

@Getter
public class JasyptUI {

    private JPanel rootPanel;
    // 密钥
    private JTextField keyField;
    // 文本
    private JTextField textField;
    // 算法
    private JComboBox<String> algorithmBox;


    // 加/解密结果
    private JTextArea resultField;
    private JButton encryptButton;
    private JButton decryptButton;
    private JPanel centerJpanel;
    private JPanel buttonJpanel;
    private JButton decryptYAMLButton;

    private Project project;

    private JasyptUI() {
        // 初始化算法选项
        algorithmBox.addItem("PBEWithMD5AndDES");
        algorithmBox.addItem("PBEWithHMACSHA512AndAES_256");

        // 加密按钮事件
        encryptButton.addActionListener(e -> handleEncryption(true));

        // 解密按钮事件
        decryptButton.addActionListener(e -> handleEncryption(false));

        decryptYAMLButton.addActionListener(e -> handleDecryptFromYamlFile());
    }

    public JasyptUI(Project project) {
        this();
        this.project = project;
    }

    public void handleDecryptFromYamlFile() {
        if (project == null) {
            JOptionPane.showMessageDialog(null, "获取项目失败!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        YmlProcessor.processYmlFiles(project);
        JOptionPane.showMessageDialog(null, "操作成功.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleEncryption(boolean isEncrypt) {
        String text = this.getTextField().getText().trim();
        PooledPBEStringEncryptor encryptor = getEncryptor();

        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(null, "文本不能为空!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (encryptor == null) return;

        String result;
        if (isEncrypt) {
            result = encryptor.encrypt(text);
        } else {
            result = encryptor.decrypt(text);
        }

        // 显示结果
        this.getResultField().setText(result);

        // 自动复制到剪切板
        copyToClipboard(result);
    }

    private @Nullable PooledPBEStringEncryptor getEncryptor() {
        String key = this.getKeyField().getText().trim();
        String algorithm = (String) this.getAlgorithmBox().getSelectedItem();
        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(null, "密钥不能为空!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (algorithm == null || algorithm.isEmpty()) {
            JOptionPane.showMessageDialog(null, "算法不能为空!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");

        // 加载 SaltGenerator
        config.setSaltGenerator(new RandomSaltGenerator());

        // 加载 IV Generator
        // 默认处理 AES 算法的 IV 生成器
        if (algorithm.contains("AES")) {
            config.setIvGenerator(new org.jasypt.iv.RandomIvGenerator());
        } else {
            config.setIvGenerator(new org.jasypt.iv.NoIvGenerator());
        }

        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this.getResultField(), "结果已拷贝到剪切板！", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
