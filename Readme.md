# SudokuFx —— 基于 JavaFx 的数独游戏软件

​		SudokuFx 是使用 JavaFx 框架实现的 GUI 数独游戏软件。软件功能包括 **生成题目，解题计时，解题提示，自动解题，保存、读取题目，切换主题，背景音乐等**。

### 软件特点

+ 只包含一个 Java 源代码文件（SudokuFx.java）
+ 题解可以保存、共享
+ 现代风格的界面设计和配色
+ 针对易用性与速度各有一套键位设置
+ 多种颜色主题可供切换
+ 附上数首开发时单曲循环的背景音乐

### 使用方法

​		SudokuFx 在 **OpenJDK13.0.2 + OpenJFX14.0.1，Ubuntu18.04.4LTS 环境** 下开发测试，暂未进行打包，需要自行搭建兼容环境并从源代码启动。

​		Splash screen -> （新建、打开、设置） -> （保存，关闭，设置）-> （键位，主题，音乐）

### 代码结构

```java
class SudokuFx extends Application { //主类
    ...
	class Core { //数独核心逻辑
        ...
        class Storage {...} //序列化类
    }
    class Config { //用户数据持久化
        ...
        class Storage {...} //序列化类
    }
	class UI extends Pane{ //图形界面
        ...
        class Splash extends Pane {...} //启动屏幕
        class Tool extends Pane {...} //左侧边栏
        class Game extends Pane {...} //游戏区域
    }
}
```

