package tools.pdmgenerate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static tools.pdmgenerate.Configuration.GENERATE_DIR;
import static tools.pdmgenerate.Configuration.NONE_PK_TABLES;
import static tools.pdmgenerate.Configuration.PACKAGE_NAME;

public class ToEntity implements Handler {
	private static final Logger log = LoggerFactory.getLogger(ToEntity.class);
	private String filePath; //存放文件的目录

	public ToEntity() {
		this.filePath = GENERATE_DIR + "entity/";
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

		public TableBuilder(Table tableDTO) {
			this.tableDTO = tableDTO;
			tableName = tableDTO.getId();
			className = tableDTO.getClassName();
			pkColumn = tableDTO.getPrimaryKey();
		}

		public void build() throws IOException {
			if (!GenerateUtils.isCreateTable(className))
				return;
			StringBuilder buf = new StringBuilder();
			buf.append(createFileHead());
			buf.append(createBody());
			buf.append(createFileFooter());
			FileWriter out = new FileWriter(filePath + className + ".java");
			out.write(buf.toString());
			out.close();
		}

		private StringBuilder createFileHead() {
			String className = tableDTO.getClassName();
			String pkCodeAttr = "该实体没有主键！";
			Column pkColumn = tableDTO.getPrimaryKey();
			if (NONE_PK_TABLES.contains(className)) {
				pkColumn = null;
				pkCodeAttr = "该实体的主键为联合主键！";
			}
			if (pkColumn != null) {
				pkCodeAttr = "主键为" + pkColumn.getClassAttrName();
			}
			StringBuilder buf = new StringBuilder();
			buf.append("package " + PACKAGE_NAME + ".entity;\n\n");
			buf.append("import java.util.Date;\n\n");
			buf.append("/**\n");
			buf.append(" * " + tableDTO.getName() + "\n");
			buf.append(" * " + tableDTO.getDesc() + "\n");
			buf.append(" * " + pkCodeAttr + "\n");
			buf.append(" */\n");
			buf.append("public class " + className + " extends Entity {\n");
			//buf.append("\tprivate static final long serialVersionUID = 1L;\n");
			List<Column> columnList = tableDTO.getColumnList();
			for (Iterator<Column> it = columnList.iterator(); it.hasNext(); ) {
				Column columnDTO = it.next();
				String attr = columnDTO.getClassAttrName();
				String codedetial = columnDTO.getName();
				String mandatory = columnDTO.getMandatory();
				String dataType = columnDTO.getJavaDataType();
				String dbDataType = columnDTO.getDbDataType();
				String codeMandatory = StringUtils.isNotBlank(mandatory) ? "|必填|" : "|";
				String desc = columnDTO.getDesc();
				String codeDesc = StringUtils.isNotBlank(desc) ? "   注：" + desc : "";
				buf.append("\tprivate " + dataType + " " + attr + "; //" + codedetial + codeMandatory + dbDataType + codeDesc + "\n");
			}
			return buf;
		}

		//是否有主键且仅有一个
		private boolean isOnePK() {
			return pkColumn != null && !NONE_PK_TABLES.contains(className);
		}

		private StringBuilder createBody() {
			StringBuilder buf = new StringBuilder();
			//增加主键的setId/getId别名方法
			if (isOnePK()) {
				String attr = pkColumn.getClassAttrName();
				String dataType = pkColumn.getJavaDataType();
				buf.append("\n");
				buf.append("\t@Override\n");
				buf.append("\tpublic Object getId() {\n");
				buf.append("\t\treturn " + attr + ";\n");
				buf.append("\t}\n\n");
				buf.append("\t@Override\n");
				buf.append("\tpublic void setId(Object id) {\n");
				if ("String".equals(dataType)) {
					buf.append("\t\tthis." + attr + " = id == null ? null : id.toString();\n");
				} else if ("Long".equals(dataType)) {
					buf.append("\t\tthis." + attr + " = id == null ? null : Long.valueOf(id.toString());\n");
				} else {
					buf.append("\t\tthis." + attr + " = id;\n");
					System.err.println("不支持此类型的主键：" + className + ":" + dataType);
				}
				buf.append("\t}\n");
			}
			List<Column> columnList = tableDTO.getColumnList();
			for (Iterator<Column> it = columnList.iterator(); it.hasNext(); ) {
				Column columnDTO = it.next();
				String attr = columnDTO.getClassAttrName();
				if (attr.equals("id")) {
					continue;
				}
				String methodAttr = StringHelper.upperFirstCase(attr);
				String dataType = columnDTO.getJavaDataType();
				buf.append("\n\tpublic " + dataType + " get" + methodAttr + "() {\n");
				buf.append("\t\treturn " + attr + ";\n");
				buf.append("\t}\n\n");
				buf.append("\tpublic void set" + methodAttr + "(" + dataType + " " + attr + ") {\n");
				buf.append("\t\tthis." + attr + " = " + attr + ";\n");
				buf.append("\t}\n");
			}
			return buf;
		}

		private StringBuilder createFileFooter() {
			StringBuilder buf = new StringBuilder();
			buf.append("}");
			return buf;
		}
	}
}
