## 项目介绍

如果您是用 PowerDesginer 来做数据库设计，那么在完成库表设计之后，编码阶段往往需要编写相对应的 Java实体类、DAO类、ibatis的SQL文本、等等。手工来完成这些工作？别开玩笑了，这是多么枯燥的一件事啊！我们可以利用，表名字段名，和类名属性名 之间的对应关系，来自动生成代码骨架。PowerDesginer 的数据文件 *.pdm 是一个 XML 文件，可以用程序解析出它的表、字段、类型等信息。

本项目就是实现了以上目标。它解析出 *.pdm 里的库表信息，批量生成 iBatis SQL、Java实体类、DAO类、DTO、Query、Service、甚至是 JSP 文件，我也用它生成。



## 怎么用它？

一般来说，您的项目代码写法未必和我一样，您现在已经拥有源代码，可以任意修改，生成您想要的文件，只要它和数据表是有关联关系的。

在修改源代码之前，您最好先把它编译通过，本项目采用 maven3 + JDK7 编译打包。

```
cd pdm-generate
mvn package
```

找到配置文件： pdm-generate/src/main/resources/config.yml
修改如下两项配置（YML格式）：

```
# PowerDesigner 数据模型文件位置
pdm_file: /Volumes/M320/workspace/chengang/pdm-generate/db-sample.pdm

# 生成文件所在目录
generate_dir: /Users/chen/Downloads/tmp/gen/
```

然后运行：


```
java -jar target/pdm-generate-1.0.jar "/your-path/config.yml"
```

即可在 generate_dir 配置的目录里看到各种生成的文件



## 代码说明

* Starter   这是Main入口类
* Configuration   这是配置类，它读取配置文件 config.yml。   
* config.yml 是默认的配置文件。如果有多个项目，可以创建多个配置文件，然后修改 Starter 类来加载。
* DB、Table、Column 是 库、表、字段的模型实体类
* ToEntity、ToDTO、ToDAO等等，这些类负责生成各类文件，您可以增加新的ToWhatYouWant

