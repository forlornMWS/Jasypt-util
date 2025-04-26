package xyz.mwszksnmdys.plugin.jasypt.action;

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
import xyz.mwszksnmdys.plugin.jasypt.i18n.JasyptBundle;
import xyz.mwszksnmdys.plugin.jasypt.util.YmlProcessor;
import xyz.mwszksnmdys.plugin.jasypt.util.PropertiesProcessor;

import javax.swing.*;
import java.util.Arrays;

/**
 * 配置文件处理操作的Action类
 */
public class PopupJasyptAction extends AnAction {

    public PopupJasyptAction() {
        // 在构造函数中初始化Action的表现形式
        Presentation presentation = getTemplatePresentation();
        presentation.setText(JasyptBundle.message("popup.presentation.text"));
        presentation.setDescription(JasyptBundle.message("popup.presentation.desc"));
        presentation.setIcon(AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            JOptionPane.showMessageDialog(null, JasyptBundle.message("popup.error.getProject.message"), "Error", JOptionPane.ERROR_MESSAGE);
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
        // 只有当选中的是YAML/Properties文件或目录时才启用此Action
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
                            isSupportedFile(file)
            );
        }

        e.getPresentation().setEnabledAndVisible(enabled);
    }

    /**
     * 检查文件是否为支持的类型
     * @param file 文件对象
     * @return 是否支持
     */
    private boolean isSupportedFile(VirtualFile file) {
        String extension = file.getExtension();
        return "yml".equalsIgnoreCase(extension) ||
                "yaml".equalsIgnoreCase(extension) ||
                "properties".equalsIgnoreCase(extension);
    }

    /**
     * 处理选中的文件
     * @param project 当前项目
     * @param selectedFiles 选中的文件数组
     */
    private void processSelectedFiles(@NotNull Project project, @NotNull VirtualFile[] selectedFiles) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, JasyptBundle.message("popup.task.background.title"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                int totalFiles = selectedFiles.length;

                for (int i = 0; i < totalFiles; i++) {
                    VirtualFile file = selectedFiles[i];
                    indicator.setText(JasyptBundle.message("popup.task.background.indicator.text",file.getName()));
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
                            file.refresh(false, false);
                        });
                    } catch (Exception e) {
                        String errorMessage = JasyptBundle.message("popup.task.background.errorMessage") + file.getName() + "\n" + e.getMessage();
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("Config Processing")
                                .createNotification(errorMessage, NotificationType.ERROR)
                                .notify(project);
                        throw new RuntimeException(errorMessage, e);
                    }
                }
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
        JOptionPane.showMessageDialog(null, JasyptBundle.message("popup.task.process.directory.success"), "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 根据文件类型处理单个文件
     * @param file 文件对象
     */
    private void processFile(VirtualFile file) {
        String extension = file.getExtension();
        if ("yml".equalsIgnoreCase(extension) || "yaml".equalsIgnoreCase(extension)) {
            YmlProcessor.processYmlFileOrDirectory(file.toNioPath());
        } else if ("properties".equalsIgnoreCase(extension)) {
            // 使用属性文件处理器
            PropertiesProcessor.processPropertiesFile(file.toNioPath());
        }
    }
}