package tools.pdmgenerate;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static tools.pdmgenerate.Configuration.PACKAGE_NAME;

public class ToService implements Handler {
	private static final String DIR = Configuration.GENERATE_DIR + "service/";

	@Override
	public void handle() throws Exception {
		new File(DIR).mkdirs();
		List<Table> tableList = DB.getTableList();
		for (Table tableDTO : tableList) {
			String className = tableDTO.getClassName();
			if (!GenerateUtils.isCreateTable(className))
				continue;
			StringBuilder buf = new StringBuilder();
			buf.append("package " + PACKAGE_NAME + ".service;\n\n");
			buf.append("import org.springframework.stereotype.Service;\n\n");
			buf.append("@Service\n");
			buf.append("public class " + className + "Service extends AdminService {\n");
			buf.append("\n");
			buf.append("}\n");
			FileWriter out = new FileWriter(DIR + className + "Service.java");
			out.write(buf.toString());
			out.close();
		}
	}

}
