package xyz.mwszksnmdys.plugin.jasypt.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import xyz.mwszksnmdys.plugin.jasypt.form.JasyptDialogWrapper;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;

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
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText != null) {
            // 推断默认操作类型（如果文本看起来是加密的，则默认为解密）
            boolean isEncryptedText = selectedText.startsWith("ENC(") && selectedText.endsWith(")") ||
                    selectedText.matches("^[A-Za-z0-9+/=]+$");

            // 在写操作外部显示对话框
            ApplicationManager.getApplication().invokeLater(() -> {
                JasyptDialogWrapper dialog = new JasyptDialogWrapper(project, selectedText, !isEncryptedText);
                if (dialog.showAndGet()) {
                    // 使用新的方法直接应用到文档
                    int startOffset = editor.getSelectionModel().getSelectionStart();
                    int endOffset = editor.getSelectionModel().getSelectionEnd();
                    dialog.applyToDocument(editor.getDocument(), startOffset, endOffset);
                }
            });
        }
    }

    @Override
    public boolean startInWriteAction() {
        // 更改为false，手动控制写操作
        return false;
    }
}