
# Jasypt Tool Plugin

一个用于 IntelliJ IDEA 的 Jasypt 加密解密工具插件，帮助开发者轻松管理配置文件中的敏感数据。

## 功能特点

- 支持单文件或目录批量处理 YAML 配置文件
- 支持多种加密算法（PBEWithMD5AndDES、PBEWithHMACSHA512AndAES_256）
- 支持从环境变量读取加密密钥
- 集成到 IDE 的右键菜单和工具菜单
- 提供简单直观的 GUI 界面

## 使用方法

### 1. 配置文件要求

在 `application.yml` 或需要处理的 YAML 文件中添加 Jasypt 配置：

```yaml
jasypt:
  encryptor:
    password: ${JASYPT_PASSWORD:your_default_password}
    algorithm: PBEWithHMACSHA512AndAES_256
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
```

### 2. 使用方式

- **方式一：通过工具菜单**
    - 点击 `Tools -> Jasypt` 打开加密解密工具窗口
    - 输入密钥和待处理文本
    - 选择算法
    - 点击 `encrypt` 或 `decrypt` 按钮对指定文本进行加密或解密
    - 或者点击 `proces yaml`按钮，选择yaml文件或目录，会根据yml文件中的jasypt配置进行加密或解密

- **方式二：通过右键菜单**
    - 在项目视图中右键选择 YAML 文件或目录
    - 选择 `Encrypt/Decrypt Yaml`
    - 插件将自动处理选中的文件

### 3. 环境变量支持

密钥支持从环境变量读取，格式为：`${ENV_KEY:default_value}`
- `ENV_KEY`: 环境变量名
- `default_value`: 可选的默认值

## 开发环境要求

- IntelliJ IDEA 2023.2.8 或更高版本
- Java 17
- Gradle 8.5

## 构建与安装

1. 克隆项目：
```bash
git clone https://github.com/yourusername/jasypt-tool-plugin.git
```

2. 构建插件：
```bash
./gradlew buildPlugin
```

3. 安装插件：
- 在 IDEA 中，进入 `Settings/Preferences -> Plugins`
- 选择 `Install Plugin from Disk`
- 选择构建生成的插件文件（位于 `build/distributions` 目录）

## 贡献指南

欢迎提交 Pull Request 或 Issue。在提交之前，请确保：
- 代码遵循项目的编码规范
- 新功能包含适当的测试
- 更新相关文档

## 许可证

[Apache License 2.0](LICENSE)
