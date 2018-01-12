package tools.pdmgenerate;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static tools.pdmgenerate.Configuration.PACKAGE_NAME;

public class ToQuery implements Handler {
	private static final String DIR = Configuration.GENERATE_DIR + "query/";

	@Override
	public void handle() throws Exception {
		new File(DIR).mkdirs();
		List<Table> tableList = DB.getTableList();
		for (Table tableDTO : tableList) {
			String className = tableDTO.getClassName();
			if (!GenerateUtils.isCreateTable(className))
				continue;
			StringBuilder buf = new StringBuilder();
			buf.append("package " + PACKAGE_NAME + ".query;\n\n");
			buf.append("import java.util.Date;\n");
			buf.append("public class " + className + "Query extends Query {\n");
			for (Column columnDTO : tableDTO.getColumnList()) {
				String attr = columnDTO.getClassAttrName();
				String codedetial = columnDTO.getName();
				String mandatory = columnDTO.getMandatory();
				String dataType = columnDTO.getJavaDataType();
				String dbDataType = columnDTO.getDbDataType();
				String codeMandatory = StringUtils.isNotBlank(mandatory) ? "|必填|" : "|";
				String desc = columnDTO.getDesc();
				String codeDesc = StringUtils.isNotBlank(desc) ? "   注：" + desc : "";
				String pkDesc = columnDTO.isPrimaryKeyFlag() ? "[主键] " : "";
				if (dataType.equals("Date")) {
					buf.append("\tprivate " + dataType + " " + attr + "1; //" + pkDesc + codedetial + codeMandatory + dbDataType + codeDesc + "\n");
					buf.append("\tprivate " + dataType + " " + attr + "2; \n");
				} else {
					buf.append("\tprivate " + dataType + " " + attr + "; //" + pkDesc + codedetial + codeMandatory + dbDataType + codeDesc + "\n");
				}
			}
			/**
			 * 主键
			 */
			//buf.append("\tprivate static final long serialVersionUID = 1L;\n");
			Column pkColumn = tableDTO.getPrimaryKey();
			if (pkColumn != null) {
				String attr = pkColumn.getClassAttrName();
				String dataType = pkColumn.getJavaDataType();
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
				buf.append("\n");
				buf.append("\t@Override\n");
				buf.append("\tpublic Object getId() {\n");
				buf.append("\t\treturn this." + attr + ";\n");
				buf.append("\t}\n");
			}
			for (Column columnDTO : tableDTO.getColumnList()) {
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
				buf.append("\t\tthis." + attr + " = " + attr + "; \n");
				buf.append("\t}\n");
			}
			buf.append("}\n");
			FileWriter out = new FileWriter(DIR + className + "Query.java");
			out.write(buf.toString());
			out.close();
		}
	}

}
