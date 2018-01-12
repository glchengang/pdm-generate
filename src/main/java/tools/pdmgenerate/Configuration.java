package tools.pdmgenerate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Configuration {
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	public static String PDM_FILE; //文件路径
	public static String GENERATE_DIR;  //生成的文件路径
	public static String PACKAGE_NAME; //包名
	//当设置ONLY_TABLES之后,则只生成其指定表的相关文件
	public static List<String> GENERATE_TABLES = null;
	public static List<String> EXCLUDE_TABLES = null; //需要忽略的表 (另注: 所有TMP_和ETL_开头的表都在程序里过滤掉了)
	public static List<String> NONE_PK_TABLES = null; //没有主键的表
	public static List<String> LONG_FIELDS; // java类型为long的字段
	public static List<String> KEY_WORDS = null; //字段如果是数据库关键字，则在SQL中需要加反引号，比如`key`, 否则会报错
	public static Map<String, String> JAVA_TYPE_MAP; //<表名.字段, 类型>, 指定数据表字段的的JAVA类型

	public static boolean IBATIS2_GENERATE_ORACLE = true; //是否产生oracle的专用SQL
	public static boolean IBATIS2_GENERATE_MYSQL = true; //是否产生mysql的专用SQL
	/**
	 *  因为开发中经常会有数据表结构的变化,所以把 ibatis SQL 文件分成了两个文件.
	 *  这样在重新 ibatis SQL 文件之后, BASE 文件就可以直接覆盖, 而不用辛苦的检查比对了.
	 *  一个文件称为BASE, 基本不会去修改它, 是一些基本经典的增删改查SQL, 比如:findOne,deleteOne等
	 *  一个文件称为EXTEND, 开发常因为涉及表与表的关联查询, 而需要修改和扩展的SQL, 比如:findOneDetail,findAllByQuery等
	 *  但在特殊情况下,也可能要把findOne移入EXTEND文件, 而把findOneDetail移入BASE文件.
	 */
	public static Map<String, List<String>> IBATIS2_EXTEND_TO_BASE_SQLS_MAP;
	public static Map<String, List<String>> IBATIS2_BASE_TO_EXTEND_SQLS_MAP;
	public static List<String> DEFAULT_BASE_SQLS;

	public static void clear() {
		DB.clear();

		PDM_FILE = null;
		GENERATE_DIR = null;
		PACKAGE_NAME = "commons"; //包名
		GENERATE_TABLES = null;
		EXCLUDE_TABLES = null;
		NONE_PK_TABLES = null;
		LONG_FIELDS = null;
		KEY_WORDS = null;

		IBATIS2_GENERATE_ORACLE = true;
		IBATIS2_GENERATE_MYSQL = true;
		IBATIS2_BASE_TO_EXTEND_SQLS_MAP = null;
		IBATIS2_EXTEND_TO_BASE_SQLS_MAP = null;
		JAVA_TYPE_MAP = null;
	}

	public static void load(String filename) throws FileNotFoundException {
		Yaml yaml = new Yaml();

		FileInputStream fis = null;
		if (filename.indexOf("/") == -1 && filename.indexOf("\\") == -1) {
			log.info("class-path filename = {}", filename);
			URL url = Configuration.class.getClassLoader().getResource(filename);
			if (url == null) {
				throw new FileNotFoundException("file not found: " + filename);
			}
			fis = new FileInputStream(url.getFile());
		} else {
			log.info("filename = {}", filename);
			fis = new FileInputStream(filename);
		}
		Map m = yaml.load(fis);

		for (Object o : m.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			System.out.println(e.getKey() + "=" + e.getValue());
		}

		clear();

		PDM_FILE = (String) m.get("pdm_file");
		GENERATE_DIR = (String) m.get("generate_dir");
		if (PDM_FILE == null)
			throw new RuntimeException("必须配置 PowerDesigner 数据模型文件 *.pdm 的位置: pdm_file");
		if (GENERATE_DIR == null)
			throw new RuntimeException("必须配置输出目录: generate_dir");

		PACKAGE_NAME = (String) m.get("package_name");
		GENERATE_TABLES = getList(m, "generate_tables");
		EXCLUDE_TABLES = getList(m, "exclude_tables");
		NONE_PK_TABLES = getList(m, "none_pk_tables");

		/**
		 * 转为小写
		 */
		{
			LONG_FIELDS = new ArrayList<>();
			List<String> list = (List) m.get("long_fields");
			if (list != null) {
				for (String s : list) {
					LONG_FIELDS.add(s.toLowerCase());
				}
			}
			System.out.println("LONG_FIELDS=" + LONG_FIELDS);
		}
		/**
		 * 转为小写
		 */
		{
			KEY_WORDS = new ArrayList<>();
			List<String> list = (List) m.get("key_words");
			if (list != null) {
				for (String s : list) {
					KEY_WORDS.add(s.toLowerCase());
				}
			}
		}

		JAVA_TYPE_MAP = getMap(m, "java_type_map");
		/**
		 * ibatis
		 */
		{
			Map ibatis2 = (Map) m.get("ibatis2");
			if (ibatis2 != null) {
				Boolean b = (Boolean) ibatis2.get("generate_oracle");
				IBATIS2_GENERATE_ORACLE = b == null ? true : b.booleanValue();
				b = (Boolean) ibatis2.get("generate_mysql");
				IBATIS2_GENERATE_MYSQL = b == null ? true : b.booleanValue();

				IBATIS2_EXTEND_TO_BASE_SQLS_MAP = (Map<String, List<String>>) ibatis2.get("extend_to_base_sqls");
				IBATIS2_BASE_TO_EXTEND_SQLS_MAP = (Map<String, List<String>>) ibatis2.get("base_to_extend_sqls");
				DEFAULT_BASE_SQLS = (List<String>) ibatis2.get("default_base_sqls");
				;
			}
			if (IBATIS2_EXTEND_TO_BASE_SQLS_MAP == null) {
				IBATIS2_EXTEND_TO_BASE_SQLS_MAP = Collections.emptyMap();
			}
			if (IBATIS2_BASE_TO_EXTEND_SQLS_MAP == null) {
				IBATIS2_BASE_TO_EXTEND_SQLS_MAP = Collections.emptyMap();
			}
			if (DEFAULT_BASE_SQLS == null) {
				DEFAULT_BASE_SQLS = Collections.emptyList();
			}

//			System.out.println("IBATIS2_EXTEND_TO_BASE_SQL_MAP="+IBATIS2_EXTEND_TO_BASE_SQL_MAP);
//			System.out.println("IBATIS2_BASE_TO_EXTEND_SQL_MAP="+IBATIS2_BASE_TO_EXTEND_SQL_MAP);
//			System.out.println(IBATIS2_EXTEND_TO_BASE_SQL_MAP.get("Topic"));
//			System.out.println(IBATIS2_EXTEND_TO_BASE_SQL_MAP.get("Topic").getClass());
//			System.out.println(IBATIS2_EXTEND_TO_BASE_SQL_MAP.get("Topic").contains("findOneDetail"));
//			System.out.println(IBATIS2_EXTEND_TO_BASE_SQL_MAP.get("Topic").contains("findOneDetail1"));

			System.out.println("DEFAULT_BASE_SQLS=" + DEFAULT_BASE_SQLS);
		}
	}

	private static List getList(Map m, String key) {
		List list = (List) m.get(key);
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}

	private static Map getMap(Map m, String key) {
		Map result = (Map) m.get(key);
		if (result == null) {
			result = Collections.emptyMap();
		}
		return result;
	}

}
