package xyz.mwszksnmdys.demoplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import xyz.mwszksnmdys.demoplugin.form.JasyptUI;

import javax.swing.*;
import java.awt.*;

public class JasyptAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        // 创建一个非模态窗口
        JDialog dialog = new JDialog((JFrame) null, "Jasypt Encrypt/Decrypt Tool", false);
        dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);

        // 设置对话框背景与系统颜色一致
        Color backgroundColor = UIManager.getColor("Panel.background");
        dialog.getContentPane().setBackground(backgroundColor);
        dialog.getRootPane().setBackground(backgroundColor);

        JasyptUI jasyptUI = new JasyptUI(project);
        dialog.setContentPane(jasyptUI.getRootPanel());

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }
}
