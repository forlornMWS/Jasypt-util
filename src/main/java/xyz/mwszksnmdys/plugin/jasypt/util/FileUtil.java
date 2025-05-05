package xyz.mwszksnmdys.plugin.jasypt.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FileUtil {

    public static String readFile(Path filePath) {
        // 获取 VirtualFile
        VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString());
        if (vFile == null) {
            throw new RuntimeException("Cannot find virtual file for path: " + filePath);
        }

        // 确保所有文档都保存
        ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());

        // 强制刷新文件
        vFile.refresh(true, false);

        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            try {
                // 优先从 Document 获取内容
                Document document = FileDocumentManager.getInstance().getCachedDocument(vFile);
                if (document != null) {
                    return document.getText();
                }
                // 如果没有打开的文档，则从文件读取
                return Files.readString(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void writeFile(Path filePath, String content) throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(filePath.toString());
        if (vFile == null) {
            throw new RuntimeException("Cannot find virtual file for path: " + filePath);
        }

        ApplicationManager.getApplication().invokeAndWait(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        // 获取或创建 Document
                        Document document = FileDocumentManager.getInstance().getDocument(vFile);
                        if (document != null) {
                            document.setText(content);
                            FileDocumentManager.getInstance().saveDocument(document);
                        } else {
                            Files.writeString(filePath, content);
                        }
                        vFile.refresh(true, false);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }));

        try {
            boolean ignored = latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("write operation interrupted", e);
        }
    }


    public static Map<String, Object> loadYamlFile(Path path) {
        // 使用 FileUtil 获取最新内容
        String content = FileUtil.readFile(path);
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(loaderOptions);
        return yaml.load(content);
    }
}
