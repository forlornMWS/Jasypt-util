package xyz.mwszksnmdys.plugin.jasypt.form;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;
import xyz.mwszksnmdys.plugin.jasypt.util.EncryptorFactory;
import xyz.mwszksnmdys.plugin.jasypt.util.PropertiesProcessor;
import xyz.mwszksnmdys.plugin.jasypt.util.YmlProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
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
    private JButton processConfigButton;

    private JLabel keyLabel;
    private JLabel textLabel;
    private JLabel algorithmLabel;
    private JLabel resultLabel;

    private Project project;

    private JasyptUI() {
        initAlgorithmComboBox();

        initUIText();
        encryptButton.addActionListener(e -> handleEncryption(true));
        decryptButton.addActionListener(e -> handleEncryption(false));
        processConfigButton.addActionListener(e -> handleSelectFile());
    }

    /**
     * 初始化算法下拉框，填充所有支持的PBE算法
     */
    private void initAlgorithmComboBox() {
        // 从工厂类获取所有支持的PBE算法
        List<String> algorithms = EncryptorFactory.getSupportedPBEAlgorithms();

        // 清空现有项并添加支持的算法
        algorithmBox.removeAllItems();
        for (String algorithm : algorithms) {
            algorithmBox.addItem(algorithm);
        }
    }

    /**
     * 初始化标签文本
     */
    private void initUIText() {
        keyLabel.setText(JasyptBundle.message("toolbar.dialog.ui.secretKey"));
        textLabel.setText(JasyptBundle.message("toolbar.dialog.ui.text"));
        algorithmLabel.setText(JasyptBundle.message("toolbar.dialog.ui.algorithm"));
        resultLabel.setText(JasyptBundle.message("toolbar.dialog.ui.result"));
        processConfigButton.setText(JasyptBundle.message("toolbar.dialog.ui.btn.config"));
        encryptButton.setText(JasyptBundle.message("toolbar.dialog.ui.btn.encrypt"));
        decryptButton.setText(JasyptBundle.message("toolbar.dialog.ui.btn.decrypt"));
    }

    public JasyptUI(Project project) {
        this();
        this.project = project;
    }

    private void handleSelectFile() {
        if (project == null) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("popup.error.getProject.message"), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true)
                .withTitle(JasyptBundle.message("toolbar.dialog.descriptor.title"))
                .withDescription(JasyptBundle.message("toolbar.dialog.descriptor.description"))
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
        ProgressManager.getInstance().run(new Task.Backgroundable(project, JasyptBundle.message("popup.task.background.title"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                int totalFiles = selectedFiles.length;

                for (int i = 0; i < totalFiles; i++) {
                    VirtualFile file = selectedFiles[i];
                    indicator.setText(JasyptBundle.message("popup.task.background.indicator.text", file.getName()));
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
                        String errorMessage = JasyptBundle.message("popup.task.background.errorMessage")+ file.getName() + "\n" + e.getMessage();
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("Config Processing")
                                .createNotification(errorMessage, NotificationType.ERROR)
                                .notify(project);
                        throw new RuntimeException(errorMessage, e);
                    }
                }
                JOptionPane.showMessageDialog(null, JasyptBundle.message("popup.task.process.directory.success"), "Success", JOptionPane.INFORMATION_MESSAGE);

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
            JOptionPane.showMessageDialog(null, JasyptBundle.message("toolbar.dialog.validate.text"), "Error", JOptionPane.ERROR_MESSAGE);
            return;
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
        return EncryptorFactory.createPBEEncryptor(key, algorithm, null, null, null);
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this.getResultField(), JasyptBundle.message("toolbar.dialog.resultCopied"), "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}