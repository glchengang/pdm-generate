package tools.pdmgenerate;

import org.dom4j.Element;

public class Column {
	private String id; //英文名 (强制转小写)
	private String name; //中文名
	private String mandatory; // 必填
	private String desc; //注释
	private String type; //类型：String,Integer,Float....
	private boolean primaryKeyFlag = false; //本字段是否为主键
	private Table table;
	private String classAttrName;
	private String javaDataType;
	private String dbDataType;

	public Column() {
	}

	public Column(Table tableDTO, Element columnElement) {
		this.id = columnElement.elementText("Code").trim().toLowerCase();
		this.name = columnElement.elementText("Name").trim();
		this.mandatory = columnElement.elementText("Column.Mandatory");
		this.dbDataType = columnElement.elementText("DataType");
		desc = columnElement.elementText("Comment");
		if (desc != null) { //删除回车换行符
			desc = desc.replaceAll("\n", "");
			desc = desc.replaceAll("\r", "").trim();
		}
		this.setType(columnElement.elementText("DataType"));
		this.table = tableDTO;
		this.classAttrName = StringHelper.lowerFirstCase(StringHelper.toCamel(this.id, "_"));
		if (this.id.indexOf("_") == 1) { //G_USER_ID当第一个单词只有一个字符时,会导致无法设值的问题
			this.classAttrName = this.classAttrName.substring(0, 2).toLowerCase() + this.classAttrName.substring(2);
		}
		this.javaDataType = GenerateUtils.toJavaDataType(tableDTO.getId(), id, type, desc);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public String getJavaDataType() {
		return javaDataType;
	}

	public void setJavaDataType(String javaDataType) {
		this.javaDataType = javaDataType;
	}

	public void setClassAttrName(String classAttrName) {
		this.classAttrName = classAttrName;
	}

	public String getClassAttrName() {
		return classAttrName;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isPrimaryKeyFlag() {
		return primaryKeyFlag;
	}

	public void setPrimaryKeyFlag(boolean primaryKeyFlag) {
		this.primaryKeyFlag = primaryKeyFlag;
	}

	public String getMandatory() {
		return mandatory;
	}

	public void setMandatory(String mandatory) {
		this.mandatory = mandatory;
	}

	public String getDbDataType() {
		return dbDataType;
	}

	public void setDbDataType(String dbDataType) {
		this.dbDataType = dbDataType;
	}
}
