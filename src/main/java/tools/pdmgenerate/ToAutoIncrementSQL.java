package tools.pdmgenerate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ToAutoIncrementSQL implements Handler {
	private static final Logger log = LoggerFactory.getLogger(ToAutoIncrementSQL.class);
	private String filePath; //存放文件的目录
	private FileWriter out1;
	private FileWriter out2;

	public ToAutoIncrementSQL() {
		this.filePath = Configuration.GENERATE_DIR + "sql/";
	}

	@Override
	public void handle() throws Exception {
		new File(filePath).mkdirs();
		out1 = new FileWriter(filePath + "alert_mysql_auto_increment.sql");
		out2 = new FileWriter(filePath + "no_auto_increment.txt");
		out2.write("common.entity.EntityContext 类中的非自增 ID 配置");
		out2.write("common.entity.EntityContext 类中的非自增 ID 配置\n\n\n");
		for (Table table : DB.getTableList()) {
			new TableBuilder(table).build();
		}
		out1.close();
		out2.close();
	}

	private class TableBuilder {
		private Table tableDTO;
		private String tableName; //表名
		private String className; //类名
		private Column pkColumn; //主键

		public TableBuilder(Table tableDTO) {
			this.tableDTO = tableDTO;
			this.tableName = tableDTO.getId();
			this.className = tableDTO.getClassName();
			this.pkColumn = tableDTO.getPrimaryKey();
		}

		public void build() throws IOException {
			if (GenerateUtils.isAutoIncrementPK(tableDTO)) {
				String pk = pkColumn.getId().toUpperCase();
				out1.write("ALTER TABLE " + tableName.toUpperCase() + " CHANGE " + pk + " " + pk + " INT AUTO_INCREMENT;");
				out1.write("\n");
			} else {
				out2.write("NOT_AUTO_INCREMENT_SET.add(\"" + tableDTO.getClassName() + "\");");
				out2.write("\n");
			}
		}

	}
}
