package tools.pdmgenerate;

import org.apache.commons.lang3.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static tools.pdmgenerate.Configuration.LONG_FIELDS;

public class GenerateUtils {
	private static final Logger log = LoggerFactory.getLogger(GenerateUtils.class);

	public static boolean isCreateTable(String className) {
		{
			//所有TMP和ETL开头都过滤掉
			String tableName = StringHelper.converCamel(className, "_");
			String s = tableName.toUpperCase();
			if (s.startsWith("TMP_") || s.startsWith("ETL_")) {
				return false;
			}
		}
		if (Configuration.EXCLUDE_TABLES != null) {
			for (String str : Configuration.EXCLUDE_TABLES) {
				if (className.equalsIgnoreCase(str)) {
					log.info("不生成表:" + str);
					return false;
				}
			}
		}
		if (Configuration.GENERATE_TABLES != null && !Configuration.GENERATE_TABLES.isEmpty()) {
			for (String tbl : Configuration.GENERATE_TABLES) {
				if (className.equalsIgnoreCase(tbl)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public static Element getPrimaryKeyColumn(Element tableElement) {
		List<Element> pkList = tableElement.selectNodes("c:PrimaryKey/o:Key");
		if (pkList == null || pkList.isEmpty()) {
			return null;
		} else if (pkList.size() > 1) {
			throw new RuntimeException("tow pk is error");
		}
		String pkEid = pkList.get(0).attributeValue("Ref");

		List<Element> tblKeys = tableElement.selectNodes("c:Keys/o:Key[@Id='" + pkEid + "']");
		Element pkElement = tblKeys.get(0);
		List<Element> keyColumnList = pkElement.selectNodes("c:Key.Columns/o:Column");
		if (keyColumnList.isEmpty()) {
			log.info("c:Key.Columns/o:Column is null. id=" + pkEid);
			return null;
		} else {
			String pkColumnEid = keyColumnList.get(0).attributeValue("Ref");
			List<Element> pkColumnList = tableElement.selectNodes("c:Columns/o:Column[@Id='" + pkColumnEid + "']");
			return pkColumnList.get(0);
		}
	}

	// 从文件读取XML，输入文件名，返回XML文档
	public static Document getDocument(String fileName) {
		SAXReader reader = new SAXReader();
		try {
			return reader.read(new File(fileName));
		} catch (DocumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toJavaDataType(String tableName, String dbFieldName, String dataType, String desc) {
		{
			//如果JAVA_TYPE_MAP有指定字段类型,则使用指定的
			String className = StringHelper.toCamel(tableName, "_");
			String attrName = StringHelper.lowerFirstCase(StringHelper.toCamel(dbFieldName, "_"));
			String key = className + "." + attrName;
			String javaType = Configuration.JAVA_TYPE_MAP.get(key);
			if (javaType != null) {
				return javaType;
			}
		}

		if (desc != null) {
			//如果注释的提定类型则以注释为准
			if (desc.indexOf("#Integer#") != -1) {
				log.info(tableName + " ----- " + desc);
				return "Integer";
			} else if (desc.indexOf("#int#") != -1) {
				log.info(tableName + " ----- " + desc);
				return "int";
			} else if (desc.indexOf("#Long#") != -1) {
				log.info(tableName + " ----- " + desc);
				return "Long";
			}
		}
		{
			dataType = (dataType == null) ? null : dataType.toLowerCase();
			if (dataType == null) {
				log.info(tableName + "---dataType is null----" + dbFieldName);
			} else if (dataType.equals("date") || dataType.equals("datetime") || dataType.startsWith("timestamp")) {
				dataType = "Date";
			} else if (dataType.matches(".*char.*") || dataType.equals("bfile") || dataType.equals("xmltype")) {//varchar.*
				dataType = "String";
			} else if (dataType.equals("integer") || dataType.equals("int") || dataType.equals("number") || dataType.equals("smallint")) {  //注: powerdesigner中的long不是指数值
				if (isPK(dbFieldName) || LONG_FIELDS.contains(dbFieldName.toLowerCase())) {
					dataType = "Long";
				} else {
					dataType = "Integer";
				}
			} else if (dataType.startsWith("decimal(") || dataType.startsWith("dec(") || dataType.startsWith("numeric(") || dataType.startsWith("number(")) {
				dataType = "Double";
			} else {
				log.info(tableName + "---------" + dbFieldName + "----------------" + dataType);
			}
		}
		return dataType;
	}

	/**
	 * 判断字段时是否主键的命名格式
	 */
	private static boolean isPK(String field) {
		int len = field.length();
		//最后两位字符是id
		String s = field.substring(len - 2);
		if (s.equalsIgnoreCase("id"))
			return true;
		//dm_statkeyid1, 最后一位是数字的id. 有些是 orderidx不是id
		if (len > 2) {
			String s1 = field.substring(len - 3, len - 1);
			String s2 = field.substring(len - 1);
			if (s1.equalsIgnoreCase("_id") && NumberUtils.isDigits(s2))
				return true;
		}
		return false;
	}

	/**
	 * 判断字段时是否数字主键
	 */
	public static boolean isNumberPK(String javaDataType) {
		return "Long".equalsIgnoreCase(javaDataType) || "Integer".equalsIgnoreCase(javaDataType);
	}

	//是否有主键且仅有一个
	public static boolean isOnePK(Column pkColumn, String className) {
		return pkColumn != null && !Configuration.NONE_PK_TABLES.contains(className);
	}

	/**
	 * 是否含有自递增主键
	 */
	public static boolean isAutoIncrementPK(Table tableDTO) {
		String s = tableDTO.getDesc();
		if (s != null && s.indexOf("#noseq#") != -1) { //不用自增ID
			log.info("因为有 #noseq# 标注在注释里, 非自增ID:" + tableDTO.getId());
			return false;
		}
		Column pkColumn = tableDTO.getPrimaryKey();
		if (pkColumn == null) {
			log.info("无主键, 非自增ID:" + tableDTO.getId());
			return false;
		}
		if (!isOnePK(pkColumn, tableDTO.getClassName())) {
			log.info("复合主键, 非自增ID. 表=" + tableDTO.getId());
			return false;
		}
		if (GenerateUtils.isNumberPK(pkColumn.getJavaDataType())) {
			return true;
		} else {
			log.info("不是数字主键, 非自增ID. 表=" + tableDTO.getId() + ", 主键名=" + pkColumn.getId() + ", 主键类型=" + pkColumn.getJavaDataType());
			return false;
		}
	}

}
