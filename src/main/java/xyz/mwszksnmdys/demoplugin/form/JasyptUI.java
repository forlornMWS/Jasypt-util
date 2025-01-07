package xyz.mwszksnmdys.demoplugin.form;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
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
    private JTextField keyField;
    private JTextField textField;
    private JComboBox<String> algorithmBox;
    private JTextArea resultField;
    private JButton encryptButton;
    private JButton decryptButton;
    private JPanel centerJpanel;
    private JPanel buttonJpanel;
    private JButton processYmlButton;

    private Project project;

    private JasyptUI() {
        algorithmBox.addItem("PBEWithMD5AndDES");
        algorithmBox.addItem("PBEWithHMACSHA512AndAES_256");

        encryptButton.addActionListener(e -> handleEncryption(true));
        decryptButton.addActionListener(e -> handleEncryption(false));
        processYmlButton.addActionListener(e -> handleSelectFile());
    }

    public JasyptUI(Project project) {
        this();
        this.project = project;
    }

    private void handleSelectFile() {
        if (project == null) {
            JOptionPane.showMessageDialog(null, "获取项目失败!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, false);
        descriptor.setTitle("选择文件或目录");
        descriptor.setDescription("请选择一个YML文件或目录, 如不选择则处理项目resources目录下所有YML文件.");
        VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
        descriptor.setRoots(baseDir);

        VirtualFile file = FileChooser.chooseFile(descriptor, project, baseDir);
        if (file != null) {
            YmlProcessor.processYmlFileOrDirectory(file.toNioPath());
        }else{
            YmlProcessor.processYmlFiles(project);
            JOptionPane.showMessageDialog(null, "操作成功.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
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

        this.getResultField().setText(result);
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

        config.setSaltGenerator(new RandomSaltGenerator());

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