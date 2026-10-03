# 记一笔 📒

> 一个 UI 精美的 Android 本地记账应用：Kotlin + Jetpack Compose + Material 3，数据通过 Room 保存在手机本地，无需联网、无需账号、无广告。

![Android](https://img.shields.io/badge/平台-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/语言-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/数据库-Room-669DF6)
![License](https://img.shields.io/badge/协议-MIT-green)

## ✨ 功能特性

- **首页**：本月收支概览卡片（总支出 / 总收入 / 结余），最近记录列表（可删除）
- **记一笔**：金额输入、支出 / 收入切换、分类选择（Material 图标 + 配色）、日期选择、备注
- **统计**：按分类支出环形图（Compose Canvas 自绘）、近 6 个月收支柱状图、支持按月份切换查看
- **12 个收支分类**：
  - 支出：餐饮、交通、购物、住房、娱乐、医疗、教育、其他
  - 收入：工资、奖金、兼职、其他
- **深色 / 浅色主题跟随系统**，Material 3 动态配色

## 🛠 技术栈

| 项 | 版本 |
|---|---|
| 包名 | `com.jiyibi.app` |
| 语言 | Kotlin 2.0.21（JVM 17） |
| UI | Jetpack Compose（BOM 2024.09.00）+ Material 3 |
| 导航 | navigation-compose 2.7.0 |
| 数据库 | Room 2.6.1（KSP 生成代码） |
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.7 |
| minSdk / targetSdk / compileSdk | 26 / 34 / 34 |

## 📋 环境要求

- Android Studio Hedgehog（2023.1.1）及以上版本
- JDK 17
- Android SDK（已安装 API 34）

## 🚀 快速开始

```bash
git clone https://github.com/liujunxi120601/jiyibi.git
```

1. 用 Android Studio 选择 **Open**，打开克隆下来的 `jiyibi/` 目录
2. 等待 Gradle 同步完成（首次需要下载依赖，请保持网络畅通）
3. 点击 **Run ▶**，即可安装到模拟器或真机上运行（debug 包）

### 命令行打 debug 包

```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

cd jiyibi
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> 首次构建会自动下载 Gradle 发行版与依赖，需要较长时间，请耐心等待。

## 📦 打 release 包

1. 生成签名密钥（只需做一次，妥善保管 keystore 文件与密码，不要提交到仓库）：

   ```bash
   keytool -genkeypair -v -keystore jiyibi-release.keystore \
     -alias jiyibi -keyalg RSA -keysize 2048 -validity 10000
   ```

2. 在 `app/build.gradle.kts` 的 `android { ... }` 块中添加签名配置：

   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../jiyibi-release.keystore")
           storePassword = "你的密码"
           keyAlias = "jiyibi"
           keyPassword = "你的密码"
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
           // ...
       }
   }
   ```

   > 更安全的做法是把密码放到 `~/.gradle/gradle.properties`
   >（`JIYIBI_STORE_PASSWORD` / `JIYIBI_KEY_PASSWORD`）中读取，
   > 不要写死在构建脚本里。

3. 执行 `./gradlew assembleRelease`，产物在 `app/build/outputs/apk/release/app-release.apk`，可直接安装或上传到各大应用市场。

## 📁 目录结构

```
jiyibi/
├── app/src/main/java/com/jiyibi/app/
│   ├── MainActivity.kt            # 入口 + 底部导航
│   ├── data/
│   │   ├── Transaction.kt         # Room Entity
│   │   ├── TransactionDao.kt      # DAO
│   │   ├── AppDatabase.kt         # Room Database
│   │   ├── TransactionRepository.kt
│   │   └── Category.kt            # 12 个收支分类定义
│   └── ui/
│       ├── theme/                 # Material3 主题（跟随系统深浅色）
│       │   ├── Color.kt / Theme.kt / Type.kt
│       ├── home/                  # 首页：月度概览 + 最近记录
│       ├── add/                   # 记一笔：金额 / 分类 / 日期 / 备注
│       ├── stats/                 # 统计：环形图 + 柱状图
│       ├── components/            # 自绘图表、金额格式化工具
│       └── TransactionViewModel.kt
├── app/src/main/res/              # 字符串、颜色、主题、启动图标
├── gradle/wrapper/                # Gradle Wrapper 配置
├── .gitignore
├── LICENSE
└── README.md
```

## 🔒 数据与隐私

- 所有账单数据仅通过 Room 数据库保存在手机本地
- 应用不联网、不上传数据、无账号体系、无广告、无第三方 SDK

## ❓ 常见问题

- **`./gradlew` 提示缺少 wrapper jar？**
  用 Android Studio 打开项目一次即可自动补全；或本地安装 Gradle 后执行 `gradle wrapper` 重新生成。
- **Gradle 同步 / 首次构建很慢？**
  首次需要下载 Gradle 发行版与全部依赖，请保持网络畅通，耐心等待一次即可。
- **最低支持的安卓版本？**
  Android 8.0（API 26）及以上。

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。
