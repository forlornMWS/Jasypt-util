package xyz.mwszksnmdys.plugin.jasypt.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import xyz.mwszksnmdys.plugin.jasypt.form.JasyptDialogWrapper;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;
import xyz.mwszksnmdys.plugin.jasypt.util.JasyptEncryptor;
import xyz.mwszksnmdys.plugin.jasypt.util.PropertiesProcessor;
import xyz.mwszksnmdys.plugin.jasypt.util.YmlProcessor;

import java.util.Map;

/**
 * Jasypt 加密 IDEA 插件
 *
 * 功能：选中文本后按 Alt+Enter 弹出加密对话框，输入密钥和算法后加密选中文本
 */
public class JasyptIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {

    @NotNull
    @Override
    public String getText() {
        return JasyptBundle.message("action.intention.jasypt.text");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JasyptBundle.message("action.intention.jasypt.familyName");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return editor.getSelectionModel().hasSelection();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {

        // 获取当前文件
        PsiFile psiFile = element.getContainingFile();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        String extension = virtualFile.getExtension();
        String password = null;
        String algorithm = null;
        if (isConfigFile(extension)) {
            Map<String, Object> jasyptConfig;
            if ("yml".equalsIgnoreCase(extension) || "yaml".equalsIgnoreCase(extension)) {
                jasyptConfig = YmlProcessor.getJasyptConfig(virtualFile.toNioPath());
            }else{
                jasyptConfig = PropertiesProcessor.getJasyptConfig(virtualFile.toNioPath());
            }
            Map<String, Object> encryptorConfigMap = (Map<String, Object>) jasyptConfig.get("encryptor");
            if (encryptorConfigMap != null) {
                password = (String) encryptorConfigMap.get("password");
                if(password != null){
                   password = JasyptEncryptor.parsePasswordFromEnvironment(password);
                }
                algorithm = (String) encryptorConfigMap.get("algorithm");
            }
        }

        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText != null) {
            // 推断默认操作类型 如果文本看起来是加密的（ENC()包裹），则默认为解密
            boolean isEncryptedText = selectedText.startsWith("ENC(") && selectedText.endsWith(")");

            final String finalPassword = password;
            final String finalAlgorithm = algorithm;
            // 在写操作外部显示对话框
            ApplicationManager.getApplication().invokeLater(() -> {
                JasyptDialogWrapper dialog = new JasyptDialogWrapper(project, selectedText, !isEncryptedText, finalPassword, finalAlgorithm);
                if (dialog.showAndGet()) {
                    // 使用新的方法直接应用到文档
                    int startOffset = editor.getSelectionModel().getSelectionStart();
                    int endOffset = editor.getSelectionModel().getSelectionEnd();
                    dialog.applyToDocument(editor.getDocument(), startOffset, endOffset);
                }
            });
        }
    }

    private boolean isConfigFile(String extension) {
        return "yml".equalsIgnoreCase(extension) ||
                "yaml".equalsIgnoreCase(extension) ||
                "properties".equalsIgnoreCase(extension);
    }

    @Override
    public boolean startInWriteAction() {
        // 更改为false，手动控制写操作
        return false;
    }
}