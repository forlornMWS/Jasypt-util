package xyz.mwszksnmdys.demoplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import xyz.mwszksnmdys.demoplugin.form.JasyptUI;

import javax.swing.*;

public class JasyptAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

//        // 获取 ToolWindowManager
//        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
//
//        // 获取工具窗口实例
//        ToolWindow toolWindow = toolWindowManager.getToolWindow("Jasypt Tool");
//
//        if (toolWindow != null) {
//            // 激活并显示工具窗口
//            toolWindow.activate(null, true);
//        }
        // 创建一个非模态窗口
        JDialog dialog = new JDialog((JFrame) null, "Jasypt Encrypt/Decrypt Tool", false);
        JasyptUI jasyptUI = new JasyptUI(project);
        dialog.setContentPane(jasyptUI.getRootPanel());
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);


//         弹出对话框并传递 Project
//        new JasyptDialog(project).show();
    }

    // 内部类：实现对话框
    static class JasyptDialog extends DialogWrapper {

        private final JasyptUI jasyptUI;

        public JasyptDialog(Project project) {
            super(project);
            this.jasyptUI = new JasyptUI(project); // 将 Project 传递给 JasyptUI
            init(); // 必须调用以完成初始化
            setTitle("Jasypt Encrypt/Decrypt Tool");
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return jasyptUI.getRootPanel();
        }
    }
}
