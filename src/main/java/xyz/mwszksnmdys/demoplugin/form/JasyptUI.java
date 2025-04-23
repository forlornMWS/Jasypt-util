package xyz.mwszksnmdys.demoplugin.form;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Getter;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.salt.RandomSaltGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.mwszksnmdys.demoplugin.util.PropertiesProcessor;
import xyz.mwszksnmdys.demoplugin.util.YmlProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import java.util.stream.Stream;

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

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true)
                .withTitle("选择文件或目录")
                .withDescription("请选择.yml|.yaml|.properties文件和目录, 支持多选")
                .withFileFilter(file -> {
                    String extension = file.getExtension();
                    return file.isDirectory() ||
                            "yml".equalsIgnoreCase(extension) ||
                            "yaml".equalsIgnoreCase(extension)
                            || "properties".equalsIgnoreCase(extension);
                });

        VirtualFile defaultDir = findResourcesDirectory(project);

        VirtualFile[] selectedFiles = FileChooser.chooseFiles(descriptor, project, defaultDir);
        if (selectedFiles.length > 0) {
            processSelectedFiles(selectedFiles);
        }

        closeDialog();
    }

    /**
     * 处理选中的文件
     * @param selectedFiles 选中的文件数组
     */
    private void processSelectedFiles(@NotNull VirtualFile[] selectedFiles) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "处理Config文件", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                int totalFiles = selectedFiles.length;

                for (int i = 0; i < totalFiles; i++) {
                    VirtualFile file = selectedFiles[i];
                    indicator.setText("正在处理: " + file.getName());
                    indicator.setFraction((double) i / totalFiles);

                    try {
                        if (file.isDirectory()) {
                            // 处理目录
                            processDirectory(file);
                        } else {
                            // 根据文件类型处理单个文件
                            processFile(file);
                        }
                        // 在EDT中刷新文件
                        ApplicationManager.getApplication().invokeLater(() -> {
                            reloadFromDisk(file);
                        });
                    } catch (Exception e) {
                        String errorMessage = "处理文件失败: " + file.getName() + "\n" + e.getMessage();
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("Config Processing")
                                .createNotification(errorMessage, NotificationType.ERROR)
                                .notify(project);
                        throw new RuntimeException(errorMessage, e);
                    }
                }
                JOptionPane.showMessageDialog(null, "操作成功.", "Success", JOptionPane.INFORMATION_MESSAGE);

            }
        });
    }

    /**
     * 处理目录中的所有支持的文件
     * @param directory 目录文件对象
     * @throws Exception 处理异常
     */
    private void processDirectory(VirtualFile directory) throws Exception {
        YmlProcessor.processYmlFileOrDirectory(directory.toNioPath());
        PropertiesProcessor.processPropertiesDirectory(directory.toNioPath());
    }

    /**
     * 根据文件类型处理单个文件
     * @param file 文件对象
     * @throws Exception 处理异常
     */
    private void processFile(VirtualFile file) throws Exception {
        String extension = file.getExtension();
        if ("yml".equalsIgnoreCase(extension) || "yaml".equalsIgnoreCase(extension)) {
            YmlProcessor.processYmlFileOrDirectory(file.toNioPath());
        } else if ("properties".equalsIgnoreCase(extension)) {
            // 使用属性文件处理器
            PropertiesProcessor.processPropertiesFile(file.toNioPath());
        }
    }

    /**
     * 查找项目的resources目录
     * @param project 当前项目
     * @return resources目录的VirtualFile，如果未找到则返回项目根目录
     */
    private VirtualFile findResourcesDirectory(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir == null) {
            return null;
        }

        // 尝试查找标准Maven/Gradle项目结构中的resources目录
        VirtualFile[] resourcesPaths = {
                findPath(projectDir, "src", "main", "resources"),
                findPath(projectDir, "src", "test", "resources")
        };

        return Stream.of(resourcesPaths)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(projectDir);
    }

    /**
     * 安全地查找文件路径
     * @param base 基础目录
     * @param paths 子路径数组
     * @return 找到的VirtualFile，如果路径无效则返回null
     */
    private VirtualFile findPath(VirtualFile base, String... paths) {
        VirtualFile current = base;
        for (String path : paths) {
            if (current == null) {
                return null;
            }
            current = current.findChild(path);
        }
        return current;
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Window window = SwingUtilities.getWindowAncestor(rootPanel);
        if (window != null) {
            window.dispose();
        }
    }

    private void reloadFromDisk(VirtualFile file) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            if(file.isDirectory()){
                VirtualFile[] children = file.getChildren();
                for (VirtualFile child : children) {
                    fileDocumentManager.reloadFiles(child);
                }
            }else{
                fileDocumentManager.reloadFiles(file);
            }
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
        });
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