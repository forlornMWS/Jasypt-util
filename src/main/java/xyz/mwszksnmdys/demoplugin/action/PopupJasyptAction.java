package xyz.mwszksnmdys.demoplugin.action;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import xyz.mwszksnmdys.demoplugin.util.YmlProcessor;

import javax.swing.*;
import java.util.Arrays;

/**
 * YAML文件处理操作的Action类
 */
public class PopupJasyptAction extends AnAction {

    public PopupJasyptAction() {
        // 在构造函数中初始化Action的表现形式
        Presentation presentation = getTemplatePresentation();
        presentation.setText("Encrypt/Decrypt Yaml");
        presentation.setDescription("Process(Encrypt/Decrypt) YAML files");
        presentation.setIcon(AllIcons.FileTypes.Yaml);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            JOptionPane.showMessageDialog(null, "获取项目失败!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 获取选中的文件
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles != null && selectedFiles.length > 0) {
            processSelectedFiles(project, selectedFiles);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 返回EDT（Event Dispatch Thread）以确保UI更新的线程安全
        return ActionUpdateThread.BGT;

    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只有当选中的是YAML文件或目录时才启用此Action
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        boolean enabled = false;

        if (files != null && files.length > 0) {
            enabled = Arrays.stream(files).allMatch(file ->
                file.isDirectory() || 
                "yml".equalsIgnoreCase(file.getExtension()) || 
                "yaml".equalsIgnoreCase(file.getExtension())
            );
        }

        e.getPresentation().setEnabledAndVisible(enabled);
    }

    /**
     * 处理选中的文件
     * @param project 当前项目
     * @param selectedFiles 选中的文件数组
     */
    private void processSelectedFiles(@NotNull Project project, @NotNull VirtualFile[] selectedFiles) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "处理YAML文件", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                int totalFiles = selectedFiles.length;

                for (int i = 0; i < totalFiles; i++) {
                    VirtualFile file = selectedFiles[i];
                    indicator.setText("正在处理: " + file.getName());
                    indicator.setFraction((double) i / totalFiles);

                    try {
                        YmlProcessor.processYmlFileOrDirectory(file.toNioPath());

                        // 在EDT中刷新文件
                        ApplicationManager.getApplication().invokeLater(() -> {
                            file.refresh(false, false);
                        });
                    } catch (Exception e) {
                        String errorMessage = "处理文件失败: " + file.getName() + "\n" + e.getMessage();
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("YAML Processing")
                                .createNotification(errorMessage, NotificationType.ERROR)
                                .notify(project);
                    }
                }
            }
        });
    }
}