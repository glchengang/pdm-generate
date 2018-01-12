package tools.pdmgenerate;

import java.util.ArrayList;
import java.util.List;

public class Table {
	private String id; //英文名
	private String name; //中文名
	private String desc; //注释
	private List<Column> columnList = new ArrayList<Column>(); //表中所有字段
	private Column primaryKey; //主键字段
	private String className;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id.toLowerCase();
		this.className = StringHelper.toCamel(this.id, "_");
	}

	public String getClassName() {
		return className;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public List<Column> getColumnList() {
		return columnList;
	}

	public void setColumnList(List<Column> columnList) {
		this.columnList = columnList;
	}

	public Column getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(Column primaryKey) {
		this.primaryKey = primaryKey;
	}

}
