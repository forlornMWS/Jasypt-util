package xyz.mwszksnmdys.plugin.jasypt.i18n;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class JasyptBundle extends DynamicBundle {
    private static final String BUNDLE = "messages.messages";

    private JasyptBundle() {
        super(BUNDLE);
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        try {
            // 使用 IDE 的语言设置
            Locale ideLocale = DynamicBundle.getLocale();
//            Locale ideLocale = Locale.getDefault();

            // 首先尝试强制加载 IDE 语言
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, ideLocale, JasyptBundle.class.getClassLoader(),
                    new ResourceBundle.Control() {
                        @Override
                        public Locale getFallbackLocale(String baseName, Locale locale) {
                            // 禁用回退
                            return null;
                        }
                    });


            String value = bundle.getString(key);
            if (params.length > 0) {
                return MessageFormat.format(value, params);
            }
            return value;
        } catch (MissingResourceException e) {
            // 尝试加载默认资源
            try {
                ResourceBundle defaultBundle = ResourceBundle.getBundle(BUNDLE, Locale.ROOT);
                String value = defaultBundle.getString(key);
                if (params.length > 0) {
                    return MessageFormat.format(value, params);
                }
                return value;
            } catch (Exception ex) {
                return "[" + key + "]";
            }
        } catch (Exception e) {
            System.out.println("加载资源失败: " + e.getMessage());
            return "[" + key + "]";
        }
    }
}