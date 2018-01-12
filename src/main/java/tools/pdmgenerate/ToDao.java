package tools.pdmgenerate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static tools.pdmgenerate.Configuration.PACKAGE_NAME;

public class ToDao implements Handler {

	@Override
	public void handle() throws Exception {
		new File(Configuration.GENERATE_DIR + "dao/").mkdirs();
		new File(Configuration.GENERATE_DIR + "daotest/").mkdirs();
		List<Table> tableList = DB.getTableList();
		for (Table tableDTO : tableList) {
			String className = tableDTO.getClassName();
			if (!GenerateUtils.isCreateTable(className))
				continue;
			createDao(className);
			createDaoTest(className);
		}
	}

	private void createDao(String className) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append("package " + PACKAGE_NAME + ".dao; \n\n");
		buf.append("import org.springframework.stereotype.Repository;\n");
		buf.append("import " + PACKAGE_NAME + ".entity." + className + ";\n\n");
		buf.append("@Repository \n");
		buf.append("public class " + className + "Dao " + "extends Dao<" + className + "> { \n");
		buf.append("} \n");

		FileWriter out = new FileWriter(Configuration.GENERATE_DIR + "dao/" + className + "Dao.java");
		out.write(buf.toString());
		out.close();
	}

	private void createDaoTest(String className) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append("package " + PACKAGE_NAME + ".dao; \n");
		buf.append("import static org.junit.Assert.*;\n");
		buf.append("import org.junit.Test;\n");
		buf.append("import " + PACKAGE_NAME + ".entity.*;\n");
		buf.append("import " + PACKAGE_NAME + ".query.*;\n");
		buf.append("import java.util.*;\n");
		buf.append("import javax.annotation.Resource;\n");
		buf.append("public class " + className + "DaoTest extends CommonPrivateBaseTestCase {\n");
		buf.append("	@Resource\n");
		buf.append("	private " + className + "Dao dao;\n");
		buf.append("\n");
		buf.append("	@Test\n");
		buf.append("	public void test() {\n");
		buf.append("		dao.findAll();\n");
		buf.append("		dao.deleteAll();\n");
		buf.append("	}\n");
		buf.append("}\n");

		FileWriter out = new FileWriter(Configuration.GENERATE_DIR + "daotest/" + className + "DaoTest.java");
		out.write(buf.toString());
		out.close();
	}
}
