package tools.pdmgenerate;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static tools.pdmgenerate.Configuration.PACKAGE_NAME;

public class ToDTO implements Handler {
	private static final String DIR = Configuration.GENERATE_DIR + "dto/";

	@Override
	public void handle() throws Exception {
		new File(DIR).mkdirs();
		List<Table> tableList = DB.getTableList();
		for (Table tableDTO : tableList) {
			String className = tableDTO.getClassName();
			if (!GenerateUtils.isCreateTable(className))
				continue;
			StringBuilder buf = new StringBuilder();
			buf.append("package " + PACKAGE_NAME + ".dto;\n\n");
			buf.append("import " + PACKAGE_NAME + ".entity." + className + ";\n\n");
			buf.append("public class " + className + "DTO extends " + className + " {\n");
			//buf.append("\tprivate static final long serialVersionUID = 1L;\n");
			buf.append("}\n");
			FileWriter out = new FileWriter(DIR + className + "DTO.java");
			out.write(buf.toString());
			out.close();
		}
	}

}
