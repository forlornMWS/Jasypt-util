package xyz.mwszksnmdys.demoplugin.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import xyz.mwszksnmdys.demoplugin.form.JasyptUI;
import org.jetbrains.annotations.NotNull;

public class JasyptToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JasyptUI jasyptUI = new JasyptUI(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(jasyptUI.getRootPanel(), "Jasypt Tool", false);
        toolWindow.getContentManager().addContent(content);
    }
}
