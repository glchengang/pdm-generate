package tools.pdmgenerate;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static tools.pdmgenerate.Configuration.PACKAGE_NAME;

/**
 * 生成元数据
 */
public class ToMeta implements Handler {

	@Override
	public void handle() throws Exception {
		new File(Configuration.GENERATE_DIR + "meta/").mkdirs();
		List<Table> tableList = DB.getTableList();
		for (Table tableDTO : tableList) {
			String className = tableDTO.getClassName();
			if (!GenerateUtils.isCreateTable(className))
				continue;
			StringBuilder buf = new StringBuilder();
			buf.append("package " + PACKAGE_NAME + ".meta;\n\n");
			buf.append("import java.util.ArrayList;\n\n");
			buf.append("import java.util.List;\n");
			buf.append("public class " + className + "Meta  extends Meta {\n\n");
			buf.append("public List<ColumnMeta> getColumnMetaList() {\n");
			buf.append("List<ColumnMeta> list = new ArrayList<ColumnMeta>();\n");
			for (Column columnDTO : tableDTO.getColumnList()) {
				String attr = columnDTO.getClassAttrName();
				String codedetial = columnDTO.getName();
				String dataType = columnDTO.getJavaDataType();
				buf.append("list.add(new ColumnMeta(\"" + attr + "\", \"" + codedetial + "\", ColumnMeta." + dataType + "));\n");
			}
			buf.append("return list;\n");
			buf.append("}\n");
			buf.append("}");

			FileWriter out = new FileWriter(Configuration.GENERATE_DIR + "meta/" + className + "Meta.java");
			out.write(buf.toString());
			out.close();
		}
	}

}
