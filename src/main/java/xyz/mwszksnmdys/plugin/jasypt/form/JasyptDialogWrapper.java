package xyz.mwszksnmdys.plugin.jasypt.form;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;

import javax.swing.*;

public class JasyptDialogWrapper extends DialogWrapper {
    private final JasyptDialogForm form;

    public JasyptDialogWrapper(Project project, String text, boolean defaultIsEncryption) {
        super(project);
        setTitle(JasyptBundle.message("dialog.title"));
        this.form = new JasyptDialogForm(text, defaultIsEncryption, project);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return form.getMainPanel();
    }

    public String getProcessedText() {
        return form.getProcessedText();
    }

    /**
     * 将处理后的文本应用到目标文档
     * @param document 要修改的文档
     * @param startOffset 开始位置
     * @param endOffset 结束位置
     */
    public void applyToDocument(Document document, int startOffset, int endOffset) {
        form.applyToDocument(document, startOffset, endOffset);
    }
}