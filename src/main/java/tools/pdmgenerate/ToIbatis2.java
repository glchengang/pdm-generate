package tools.pdmgenerate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static tools.pdmgenerate.Configuration.*;

public class ToIbatis2 implements Handler {
	private static final Logger log = LoggerFactory.getLogger(ToIbatis2.class);
	private static final StringBuilder NULL = new StringBuilder();
	private boolean isExtend;
	private String filePath; //存放文件的目录

	public ToIbatis2(IbatisType type) {
		this.isExtend = (type == IbatisType.EXTEND);
		this.filePath = GENERATE_DIR + (isExtend ? "sqlmap_extend/" : "sqlmap_base/");
	}

	@Override
	public void handle() throws Exception {
		new File(filePath).mkdirs();
		for (Table table : DB.getTableList()) {
			new TableBuilder(table).build();
		}
	}

	private class TableBuilder {
		private Table tableDTO;
		private String tableName; //表名
		private String className; //类名
		private Column pkColumn; //主键
		private String pkAttrName;
		private String pkCode;
		private List<String> insertDBFieldList = new ArrayList<String>();
		private List<String> insertJavaAttrList = new ArrayList<String>();
		private List<String> updateDBFieldAndJavaAttrList = new ArrayList<String>();

		public TableBuilder(Table tableDTO) {
			this.tableDTO = tableDTO;
			tableName = tableDTO.getId();
			className = tableDTO.getClassName();
			pkColumn = tableDTO.getPrimaryKey();
			if (pkColumn == null) {
				pkAttrName = "";
				pkCode = "";
			} else {
				pkCode = pkColumn.getId();
				pkAttrName = pkColumn.getClassAttrName();
			}
		}

		public void build() throws IOException {
			if (!GenerateUtils.isCreateTable(className))
				return;
			buildFieldsAndValues();
			//
			StringBuilder buf = new StringBuilder();
			buf.append(createFileHead());
			buf.append(createFindOne());
			buf.append(createFindAll());
			buf.append(createFindAllCount());
			buf.append(createFindOneDetail());
			buf.append(createFindAllByQuery());
			buf.append(createFindEntityByQuery());
			buf.append(createDeleteAll());
			buf.append(createDeleteOne());
			buf.append(createInsert());
			buf.append(createUpdate());
			buf.append(createFileFooter());
			//
			FileWriter out = new FileWriter(filePath + className + ".xml");
			out.write(buf.toString());
			out.close();
		}

		private void buildFieldsAndValues() {
			for (Column columnDTO : tableDTO.getColumnList()) {
				String field = columnDTO.getId(); //数据库字段名
				String attr = columnDTO.getClassAttrName(); //JAVA类属性
				if (!(field.equals("alt_user") || field.equals("alt_time"))) {
					if (KEY_WORDS.contains(field)) {
						insertDBFieldList.add("\t\t\t`" + field + "`");
					} else {
						insertDBFieldList.add("\t\t\t" + field);
					}
				}

				if (isOnePK() && attr.equalsIgnoreCase(pkAttrName)) {
					insertJavaAttrList.add("\t\t\t#" + attr + "#");
				} else {
					String typeStr = "";
					String dataType = columnDTO.getJavaDataType();
					if ("String".equals(dataType)) {
						typeStr = ":VARCHAR";
					} else if ("Date".equals(dataType)) {
						typeStr = ":TIMESTAMP";
					} else if ("Long".equals(dataType) || "Integer".equals(dataType) || "Float".equals(dataType)) {
						typeStr = ":NUMERIC";
					}
					//alt_user,alt_time不能新增
					if (!(field.equals("alt_user") || field.equals("alt_time"))) {
						insertJavaAttrList.add("\t\t\t#" + attr + typeStr + "#");
					}
					//add_user,add_time不能更新
					if (!(field.equals("add_user") || field.equals("add_time"))) {
						if (KEY_WORDS.contains(field)) {
							updateDBFieldAndJavaAttrList.add("\t\t\t`" + field + "` = #" + attr + typeStr + "#");
						} else {
							updateDBFieldAndJavaAttrList.add("\t\t\t" + field + " = #" + attr + typeStr + "#");
						}
					}
				}
			}
		}

		private StringBuilder createFileHead() {
			StringBuilder buf = new StringBuilder();
			buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			buf.append("<!DOCTYPE sqlMap PUBLIC \"-//ibatis.apache.org//DTD SQL Map 2.0//EN\" \"http://ibatis.apache.org/dtd/sql-map-2.dtd\">\n");
			buf.append("<sqlMap namespace=\"" + className + "\">\n\n");
			if (isExtend) {
				buf.append("\t<resultMap id=\"" + className + "DTOResultMap\" class=\"" + PACKAGE_NAME + ".dto." + className + "DTO\" extends=\"" + className + "ResultMap\">\n");
				buf.append("\t</resultMap>\n\n");
			} else {
				buf.append("\t<typeAlias alias=\"" + className + "\" type=\"" + PACKAGE_NAME + ".entity." + className + "\" />\n\n");
				//
				buf.append("\t<resultMap id=\"" + className + "ResultMap\" class=\"" + PACKAGE_NAME + ".entity." + className + "\">\n");
				for (Column columnDTO : tableDTO.getColumnList()) {
					String field = columnDTO.getId();
					String attr = columnDTO.getClassAttrName();
					buf.append("\t\t<result property=\"" + attr + "\" column=\"" + field + "\" />\n");
				}
				buf.append("\t</resultMap>\n\n");
			}
			return buf;
		}

		private StringBuilder createFileFooter() {
			StringBuilder buf = new StringBuilder();
			buf.append("</sqlMap>\n");
			return buf;
		}

		//是否有主键且仅有一个
		private boolean isOnePK() {
			return pkColumn != null && !NONE_PK_TABLES.contains(className);
		}

		private StringBuilder createFindOne() {
			if (!isOnePK() || isPassGenerate("findOne"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<select id=\"findOne\" resultMap=\"" + className + "ResultMap\">\n");
			buf.append("\t\tselect * from " + tableName + " where " + pkCode + " = #" + pkAttrName + "#\n");
			buf.append("\t</select>\n\n");
			return buf;
		}

		private StringBuilder createFindAll() {
			if (isPassGenerate("findAll"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<select id=\"findAll\" resultMap=\"" + className + "ResultMap\">\n");
			buf.append("\t\tselect * from " + tableName + "\n");
			buf.append("\t</select>\n\n");
			return buf;
		}

		/**
		 * 是否忽略生成
		 * @param sqlId 此SQL的ID
		 * @return
		 */
		private boolean isPassGenerate(String sqlId) {
			boolean isBaseSQL = Configuration.DEFAULT_BASE_SQLS.contains(sqlId);
			boolean isGenerate;
			if (ToIbatis2.this.isExtend && !isExtendToBase(sqlId) && (!isBaseSQL || isBaseToExtend(sqlId))) {
				isGenerate = true;
			} else if (!ToIbatis2.this.isExtend && !isBaseToExtend(sqlId) && (isBaseSQL || isExtendToBase(sqlId))) {
				isGenerate = true;
			} else {
				isGenerate = false;
			}
			return !isGenerate;
		}

		private StringBuilder createFindAllCount() {
			if (isPassGenerate("findAllCount"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<select id=\"findAllCount\" resultClass=\"long\">\n");
			buf.append("\t\tselect count(*) from " + tableName + "\n");
			buf.append("\t</select>\n\n");
			return buf;
		}

		private StringBuilder createFindOneDetail() {
			if (isPassGenerate("findOneDetail"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<select id=\"findOneDetail\" resultMap=\"" + className + "ResultMap\">\n");
			buf.append("\t\tselect a.*\n");
			buf.append("\t\tfrom " + tableName + " a\n");
			buf.append("\t\twhere a." + pkCode + " = #" + pkAttrName + "#\n");
			buf.append("\t</select>\n\n");
			return buf;
		}

		private StringBuilder createFindAllByQuery() {
			if (isPassGenerate("findAllByQuery"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<!-- ################### 用于表与表之间的关联查询, 返回DTO类 ################### -->\n");
			buf.append("\t<sql id=\"findAllByQueryFragment\">\n");
			buf.append("\t\tfrom " + tableName + " a\n");
			buf.append("\t\t<dynamic prepend=\"where\">\n");
			List<Column> columnList = tableDTO.getColumnList();
			for (Iterator<Column> it = columnList.iterator(); it.hasNext(); ) {
				Column columnDTO = it.next();
				String code = columnDTO.getId();
				String attr = columnDTO.getClassAttrName();
				if (columnDTO.getJavaDataType().equals("Date")) {
					buf.append("\t\t\t<isNotEmpty prepend=\"and\" property=\"" + attr + "1\">\n");
					buf.append("\t\t\t\ta." + code + " >= #" + attr + "1#\n");
					buf.append("\t\t\t</isNotEmpty>\n");
					buf.append("\t\t\t<isNotEmpty prepend=\"and\" property=\"" + attr + "2\">\n");
					buf.append("\t\t\t\t<![CDATA[ a." + code + " < #" + attr + "2# ]]>\n");
					buf.append("\t\t\t</isNotEmpty>\n");
				} else {
					buf.append("\t\t\t<isNotEmpty prepend=\"and\" property=\"" + attr + "\">\n");
					buf.append("\t\t\t\ta." + code + " = #" + attr + "#\n");
					buf.append("\t\t\t</isNotEmpty>\n");
				}
			}
			buf.append("\t\t</dynamic>\n");
			buf.append("\t</sql>\n");

			buf.append("\t<select id=\"findAllByQueryCount\" resultClass=\"long\">\n");
			buf.append("\t\tselect count(*) <include refid=\"findAllByQueryFragment\"/>\n");
			buf.append("\t</select>\n");
			buf.append("\t<select id=\"findAllByQuery\" resultMap=\"" + className + "DTOResultMap\">\n");
			buf.append("\t\tselect a.* <include refid=\"findAllByQueryFragment\"/> $orderStr$\n");
			buf.append("\t</select>\n");
			buf.append("\t<!-- ############################## end ############################## -->\n\n");
			return buf;
		}

		private StringBuilder createFindEntityByQuery() {
			if (isPassGenerate("findEntityByQuery"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<!-- ############ 仅用于查找实体类, 而非DTO, 无表与表之间关联查询 ############ -->\n");
			buf.append("\t<sql id=\"findEntityByQueryFragment\">\n");
			buf.append("\t\tfrom " + tableName + " a\n");
			buf.append("\t\t<dynamic prepend=\"where\">\n");
			List<Column> columnList = tableDTO.getColumnList();
			for (Iterator<Column> it = columnList.iterator(); it.hasNext(); ) {
				Column columnDTO = it.next();
				String code = columnDTO.getId();
				String attr = columnDTO.getClassAttrName();
				if (columnDTO.getJavaDataType().equals("Date")) {
					buf.append("\t\t\t<isNotEmpty prepend=\"and\" property=\"" + attr + "1\">\n");
					buf.append("\t\t\t\ta." + code + " >= #" + attr + "1#\n");
					buf.append("\t\t\t</isNotEmpty>\n");
					buf.append("\t\t\t<isNotEmpty prepend=\"and\" property=\"" + attr + "2\">\n");
					buf.append("\t\t\t\t<![CDATA[ a." + code + " < #" + attr + "2# ]]>\n");
					buf.append("\t\t\t</isNotEmpty>\n");
				} else {
					buf.append("\t\t\t<isNotEmpty prepend=\"and\" property=\"" + attr + "\">\n");
					buf.append("\t\t\t\ta." + code + " = #" + attr + "#\n");
					buf.append("\t\t\t</isNotEmpty>\n");
				}
			}
			buf.append("\t\t</dynamic>\n");
			buf.append("\t</sql>\n");

			buf.append("\t<select id=\"findEntityByQueryCount\" resultClass=\"long\">\n");
			buf.append("\t\tselect count(*) <include refid=\"findEntityByQueryFragment\"/>\n");
			buf.append("\t</select>\n");
			buf.append("\t<select id=\"findEntityByQuery\" resultMap=\"" + className + "ResultMap\">\n");
			buf.append("\t\tselect a.* <include refid=\"findEntityByQueryFragment\"/> $orderStr$\n");
			buf.append("\t</select>\n");
			buf.append("\t<!-- ############################## end ############################## -->\n\n");
			return buf;
		}

		private StringBuilder createDeleteAll() {
			if (isPassGenerate("deleteAll"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<delete id=\"deleteAll\">\n");
			buf.append("\t\tdelete from " + tableName + "\n");
			buf.append("\t</delete>\n\n");
			return buf;
		}

		private StringBuilder createDeleteOne() {
			if (!isOnePK() || isPassGenerate("deleteOne"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<delete id=\"deleteOne\">\n");
			buf.append("\t\tdelete from " + tableName + " where " + pkCode + " = #" + pkAttrName + "#\n");
			buf.append("\t</delete>\n\n");
			return buf;
		}

		//insert和insertWithId
		private StringBuilder createInsert() {
			if (isPassGenerate("insert"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			boolean isAutoIncrementPK = GenerateUtils.isAutoIncrementPK(tableDTO); //是否含有自递增主键
			/**
			 * insertWithId
			 */
			if (isAutoIncrementPK) {
				/**
				 * insertForOracle
				 */
				if (IBATIS2_GENERATE_ORACLE) {
					buf.append("\t<insert id=\"insertForOracle\">\n");
					buf.append("\t\t<selectKey resultClass=\"long\" keyProperty=\"" + pkAttrName + "\" >\n");
					buf.append("\t\t\tselect SEQ_COMMON.nextval from DUAL\n");
					buf.append("\t\t</selectKey>\n");
					buf.append("\t\tinsert into " + tableName + "(\n");
					appendItem(insertDBFieldList, buf);
					buf.append("\t\t)values(\n");
					appendItem(insertJavaAttrList, buf);
					buf.append("\t\t)\n");
					buf.append("\t</insert>\n\n");
				}
				/**
				 * insertForMysql
				 */
				if (IBATIS2_GENERATE_MYSQL) {
					ArrayList<String> dbFieldList2 = new ArrayList(insertDBFieldList);
					ArrayList<String> javaAttrList2 = new ArrayList(insertJavaAttrList);
					dbFieldList2.remove(0); //删除主健
					javaAttrList2.remove(0);
					buf.append("\t<insert id=\"insertForMysql\">\n");
					buf.append("\t\tinsert into " + tableName + "(\n");
					appendItem(dbFieldList2, buf);
					buf.append("\t\t)values(\n");
					appendItem(javaAttrList2, buf);
					buf.append("\t\t)\n");
					buf.append("\t\t<selectKey resultClass=\"long\" keyProperty=\"" + pkAttrName + "\" >\n");
					buf.append("\t\t\tSELECT LAST_INSERT_ID() AS ID\n");
					buf.append("\t\t</selectKey>\n");
					buf.append("\t</insert>\n\n");
				}
			}
			buf.append("\t<insert id=\"insertWithId\">\n");
			buf.append("\t\tinsert into " + tableName + "(\n");
			appendItem(insertDBFieldList, buf);
			buf.append("\t\t)values(\n");
			appendItem(insertJavaAttrList, buf);
			buf.append("\t\t)\n");
			buf.append("\t</insert>\n\n");
			return buf;
		}

		private StringBuilder createUpdate() {
			if (!isOnePK() || isPassGenerate("update"))
				return NULL;
			StringBuilder buf = new StringBuilder();
			buf.append("\t<update id=\"update\">\n");
			buf.append("\t\tupdate " + tableName + " set\n");
			appendItem(updateDBFieldAndJavaAttrList, buf);
			buf.append("\t\twhere " + pkCode + " = #" + pkAttrName + "#\n");
			buf.append("\t</update>\n\n");
			return buf;
		}

		/**
		 * 加入新字段(用于insert和update的字段列表)
		 */
		private void appendItem(List<String> itemList, StringBuilder buf) {
			for (Iterator<String> it = itemList.iterator(); it.hasNext(); ) {
				buf.append(it.next());
				appendCommaAndEnter(buf, it);
			}
		}

		/**
		 * 如果是最后一个则不要逗号
		 */
		private void appendCommaAndEnter(StringBuilder buf, Iterator it) {
			if (it.hasNext()) {
				buf.append(",");
			}
			buf.append("\n");
		}

		/**
		 * 判断本sql是否不在排除生成的列表中
		 */
		private boolean isBaseToExtend(String sqlId) {
			List<String> list = IBATIS2_BASE_TO_EXTEND_SQLS_MAP.get(className);
			if (list == null) {
				return false;
			}
			return list.contains(sqlId);
		}

		/**
		 * 判断本sql是否在要生成的列表中
		 */
		private boolean isExtendToBase(String sqlId) {
			List<String> list = IBATIS2_EXTEND_TO_BASE_SQLS_MAP.get(className);
			if (list == null) {
				return false;
			}
			return list.contains(sqlId);
		}

	}

}
