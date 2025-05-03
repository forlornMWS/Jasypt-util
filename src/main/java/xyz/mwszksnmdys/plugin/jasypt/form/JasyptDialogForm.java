package xyz.mwszksnmdys.plugin.jasypt.form;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import lombok.Getter;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;
import xyz.mwszksnmdys.plugin.jasypt.util.EncryptorFactory;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.List;

public class JasyptDialogForm {
    @Getter
    private JPanel mainPanel;
    private JTextField secretKeyField;
    private JComboBox<String> algorithmComboBox;
    private JCheckBox encSurroundCheckBox;
    private JCheckBox rememberPasswordCheckBox;
    private JTextArea originalTextArea;
    private JTextArea previewTextArea;
    private JButton previewButton;
    private JComboBox<String> operationTypeComboBox;
    private JLabel operationTypeLabel;
    private JLabel passwordLabel;
    private JLabel algorithmLabel;
    private JLabel originalTextLabel;
    private JLabel previewLabel;

    private final String originalText;
    private String processedText;
    private boolean isEncryption;
    private final Project project;

    // 持久化存储的键名
    private static final String KEY_REMEMBER_PASSWORD = "xyz.mwszksnmdys.plugin.jasypt.rememberPassword";
    private static final String KEY_PASSWORD = "xyz.mwszksnmdys.plugin.jasypt.password";
    private static final String KEY_ALGORITHM = "xyz.mwszksnmdys.plugin.jasypt.algorithm";

    // 默认算法
    private static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";

    public JasyptDialogForm(String text, boolean defaultIsEncryption, Project project, String password, String algorithm) {
        this.originalText = text;
        this.isEncryption = defaultIsEncryption;
        this.project = project;

        // 初始化UI文本
        initializeUITexts();

        // 初始化算法下拉框
        initAlgorithmComboBox();

        // 设置默认操作类型
        operationTypeComboBox.setSelectedIndex(defaultIsEncryption ? 0 : 1);

        // 设置原始文本
        originalTextArea.setText(originalText);

        // 根据操作类型调整UI
        updateUIForOperationType();

        // 从持久化存储恢复设置并处理潜在的冲突
        handleCredentialConflicts(password, algorithm);

        // 为预览按钮添加事件监听器
        previewButton.addActionListener(e -> updatePreview());

        // 为操作类型下拉框添加事件监听器
        operationTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                isEncryption = operationTypeComboBox.getSelectedIndex() == 0;
                updateUIForOperationType();
                // 清空预览
                previewTextArea.setText("");
                processedText = null;
            }
        });
    }

    /**
     * 处理配置文件和已保存凭据之间的冲突
     */
    private void handleCredentialConflicts(String configFilePassword, String configFileAlgorithm) {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        boolean shouldRemember = properties.getBoolean(KEY_REMEMBER_PASSWORD, false);

        if (shouldRemember) {
            String savedPassword = properties.getValue(KEY_PASSWORD, "");
            String savedAlgorithm = properties.getValue(KEY_ALGORITHM, DEFAULT_ALGORITHM);

            // 检查是否有冲突（密码或算法至少有一个不同）
            boolean passwordConflict = configFilePassword != null && !configFilePassword.isEmpty() && !configFilePassword.equals(savedPassword);
            boolean algorithmConflict = configFileAlgorithm != null && !configFileAlgorithm.isEmpty() && !configFileAlgorithm.equals(savedAlgorithm);

            if (passwordConflict || algorithmConflict) {
                // 构建冲突提示消息
                StringBuilder message = new StringBuilder(JasyptBundle.message("credential.different.found") + "\n\n");

                if (passwordConflict) {
                    message.append(JasyptBundle.message("credential.password")).append(":\n");
                    message.append(JasyptBundle.message("credential.save", savedPassword)).append("\n");
                    message.append(JasyptBundle.message("credential.config", configFilePassword)).append("\n\n");
                }

                if (algorithmConflict) {
                    message.append(JasyptBundle.message("credential.algorithm")).append(":\n");
                    message.append(JasyptBundle.message("credential.save", savedAlgorithm)).append("\n");
                    message.append(JasyptBundle.message("credential.config", configFileAlgorithm)).append("\n\n");
                }

                message.append(JasyptBundle.message("credential.choose"));

                // 显示选择对话框
                int choice = Messages.showDialog(
                        message.toString(),
                        JasyptBundle.message("credential.dialog.title"),
                        new String[]{JasyptBundle.message("credential.dialog.options[0]"),
                                JasyptBundle.message("credential.dialog.options[1]",
                                        JasyptBundle.message("credential.dialog.options[2]"))},
                        0, Messages.getQuestionIcon()
                );

                if (choice == 0) {
                    // 使用已保存的凭据
                    secretKeyField.setText(savedPassword);
                    selectAlgorithmInComboBox(savedAlgorithm);
                    rememberPasswordCheckBox.setSelected(true);
                } else if (choice == 1) {
                    // 使用配置文件凭据
                    secretKeyField.setText(configFilePassword);
                    selectAlgorithmInComboBox(configFileAlgorithm);
                    // 如果用户选择了配置文件的凭据，询问是否更新已保存的凭据
                    if (Messages.showYesNoDialog(
                            JasyptBundle.message("credential.dialog.update.message"),
                            JasyptBundle.message("credential.dialog.update.title"),
                            Messages.getQuestionIcon()) == Messages.YES) {
                        saveSettings(configFilePassword, configFileAlgorithm, true);
                    }
                } else {
                    // 用户取消了，默认使用已保存的凭据
                    secretKeyField.setText(savedPassword);
                    selectAlgorithmInComboBox(savedAlgorithm);
                    rememberPasswordCheckBox.setSelected(true);
                }
            } else {
                // 无冲突，使用已保存的凭据
                secretKeyField.setText(savedPassword);
                selectAlgorithmInComboBox(savedAlgorithm);
                rememberPasswordCheckBox.setSelected(true);
            }
        } else {
            // 如果用户没有选择记住凭据，优先使用配置文件中的凭据
            if (configFilePassword != null && !configFilePassword.isEmpty()) {
                secretKeyField.setText(configFilePassword);
            }

            if (configFileAlgorithm != null && !configFileAlgorithm.isEmpty()) {
                selectAlgorithmInComboBox(configFileAlgorithm);
            } else {
                // 默认选择DEFAULT_ALGORITHM
                selectAlgorithmInComboBox(DEFAULT_ALGORITHM);
            }
        }
    }


    /**
     * 在算法下拉框中选择指定的算法
     */
    private void selectAlgorithmInComboBox(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return;
        }

        for (int i = 0; i < algorithmComboBox.getItemCount(); i++) {
            if (algorithmComboBox.getItemAt(i).equals(algorithm)) {
                algorithmComboBox.setSelectedIndex(i);
                return;
            }
        }

        // 如果没有找到匹配的算法，使用默认算法
        for (int i = 0; i < algorithmComboBox.getItemCount(); i++) {
            if (algorithmComboBox.getItemAt(i).equals(DEFAULT_ALGORITHM)) {
                algorithmComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * 保存设置到持久化存储
     */
    private void saveSettings(String password, String algorithm, boolean remember) {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.setValue(KEY_REMEMBER_PASSWORD, remember);

        if (remember) {
            properties.setValue(KEY_PASSWORD, password);
            properties.setValue(KEY_ALGORITHM, algorithm);
        } else {
            // 如果不记住，则清除保存的密码（但保留算法选择）
            properties.unsetValue(KEY_PASSWORD);
            properties.setValue(KEY_ALGORITHM, algorithm);
        }
    }

    /**
     * 初始化UI文本（国际化）
     */
    private void initializeUITexts() {
        // 设置组件文本
        rememberPasswordCheckBox.setText(JasyptBundle.message("checkbox.remember"));
        encSurroundCheckBox.setText(JasyptBundle.message("checkbox.surroundEnc", "ENC()"));

        // 设置操作类型下拉框选项
        operationTypeComboBox.removeAllItems();
        operationTypeComboBox.addItem(JasyptBundle.message("operation.encrypt"));
        operationTypeComboBox.addItem(JasyptBundle.message("operation.decrypt"));

        // 设置标签文本
        operationTypeLabel.setText(JasyptBundle.message("dialog.operationTypeLabel"));
        passwordLabel.setText(JasyptBundle.message("dialog.passwordLabel"));
        algorithmLabel.setText(JasyptBundle.message("dialog.algorithmLabel"));
        originalTextLabel.setText(JasyptBundle.message("dialog.originalTextLabel"));
        previewLabel.setText(JasyptBundle.message("dialog.previewLabel"));
    }

    /**
     * 初始化算法下拉框，填充所有支持的PBE算法
     */
    private void initAlgorithmComboBox() {
        // 从工厂类获取所有支持的PBE算法
        List<String> algorithms = EncryptorFactory.getSupportedPBEAlgorithms();

        // 清空现有项并添加支持的算法
        algorithmComboBox.removeAllItems();
        for (String algorithm : algorithms) {
            algorithmComboBox.addItem(algorithm);
        }

        // 获取保存的算法或使用默认值
        String savedAlgorithm = PropertiesComponent.getInstance().getValue(KEY_ALGORITHM, DEFAULT_ALGORITHM);

        // 设置算法下拉框
        algorithmComboBox.setSelectedItem(savedAlgorithm);
    }

    private void updateUIForOperationType() {
        previewButton.setText(isEncryption ?
                JasyptBundle.message("button.encrypt") :
                JasyptBundle.message("button.decrypt"));
        encSurroundCheckBox.setEnabled(isEncryption);

        // 如果是解密操作，检查文本是否有ENC包装并自动处理
        if (!isEncryption && originalText.startsWith("ENC(") && originalText.endsWith(")")) {
            originalTextArea.setText(originalText);
            // 添加一个标注，指示将自动移除ENC()包装
            originalTextArea.setToolTipText(JasyptBundle.message("tooltip.autoRemoveEnc"));
        }
    }

    private void updatePreview() {
        String secretKey = secretKeyField.getText();
        String algorithm = (String) algorithmComboBox.getSelectedItem();
        String textToProcess = originalTextArea.getText();

        try {
            // 如果是解密并且文本有ENC()包装，移除它
            if (!isEncryption && textToProcess.startsWith("ENC(") && textToProcess.endsWith(")")) {
                textToProcess = textToProcess.substring(4, textToProcess.length() - 1);
            }

            // 使用工厂类创建加密器
            PooledPBEStringEncryptor encryptor = EncryptorFactory.createPBEEncryptor(
                    secretKey, algorithm, null, null, null);

            if (encryptor == null) {
                previewTextArea.setText(JasyptBundle.message("error.encryptorNotFound"));
                throw new RuntimeException("get Encryptor failed");
            }

            if (isEncryption) {
                processedText = encryptor.encrypt(textToProcess);
                final String displayText = encSurroundCheckBox.isSelected() ?
                        "ENC(" + processedText + ")" : processedText;
                previewTextArea.setText(displayText);
            } else {
                processedText = encryptor.decrypt(textToProcess);
                previewTextArea.setText(processedText);
            }
        } catch (Exception e) {
            previewTextArea.setText(JasyptBundle.message("error.process", e.getMessage()));
            processedText = null;
        }

        // 不管加解密结果如何均保存用户设置
        saveSettings(secretKey, algorithm, rememberPasswordCheckBox.isSelected());
    }

    public String getProcessedText() {
        if (processedText == null) {
            // 如果用户没有点击预览，则在获取结果时计算
            updatePreview();
        }

        if (processedText != null && isEncryption && encSurroundCheckBox.isSelected()) {
            return "ENC(" + processedText + ")";
        }
        return processedText;
    }

    /**
     * 将处理后的文本应用到目标文档
     *
     * @param targetDocument 要修改的文档
     * @param startOffset    开始位置
     * @param endOffset      结束位置
     */
    public void applyToDocument(com.intellij.openapi.editor.Document targetDocument, int startOffset, int endOffset) {
        if (processedText == null) {
            updatePreview();
        }

        if (processedText != null && project != null) {
            final String finalText = isEncryption && encSurroundCheckBox.isSelected() ?
                    "ENC(" + processedText + ")" : processedText;

            WriteCommandAction.runWriteCommandAction(project, () -> targetDocument.replaceString(startOffset, endOffset, finalText));
        }
    }
}