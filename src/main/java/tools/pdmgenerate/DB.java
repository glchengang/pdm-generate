package tools.pdmgenerate;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DB {
	private static final Logger log = LoggerFactory.getLogger(DB.class);
	private static List<Table> TABLE_LIST;

	public static void clear() {
		TABLE_LIST = null;
	}

	public static List<Table> getTableList() {
		if (TABLE_LIST == null) {
			TABLE_LIST = new ArrayList<Table>();
			Element root = GenerateUtils.getDocument(Configuration.PDM_FILE).getRootElement();
			// 选择有id属性的o:Table
			List<Element> tableList = root.selectNodes("//o:Table[@Id]");
			log.info("table count = " + tableList.size());
			for (Element tableElement : tableList) {
				Table table = getTable(tableElement);
				TABLE_LIST.add(table);
			}
		}
		Collections.sort(TABLE_LIST, new Comparator<Table>() {
			@Override
			public int compare(Table o1, Table o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		return TABLE_LIST;
	}

	private static void fillPrimaryKeyColumn(Table tableDTO, Element tableElement) {
		Element e = GenerateUtils.getPrimaryKeyColumn(tableElement);
		String eId = null;
		if (e == null || e.equals("")) {
			log.warn("表无主键：{}, {}", tableDTO.getId(), tableDTO.getClassName());
		} else {
			eId = GenerateUtils.getPrimaryKeyColumn(tableElement).attributeValue("Id");
		}

		List<Element> pk = tableElement.selectNodes("c:Columns/o:Column[@Id='" + eId + "']");
		String pkCode = null;
		for (Element pkElement : pk) {
			pkCode = pkElement.elementText("Code");
		}
		if (pkCode != null) {
			List<Column> columnList = tableDTO.getColumnList();
			for (Column column : columnList) {
				if (column.getId().equals(pkCode)) {
					column.setPrimaryKeyFlag(true);
					tableDTO.setPrimaryKey(column);
					break;
				}
			}
		}
	}

	private static Table getTable(Element tableElement) {
		Table tableDTO = new Table();
		String tableName = tableElement.elementText("Code");
		tableDTO.setId(tableName);
		tableDTO.setName(tableElement.elementText("Name"));
		tableDTO.setDesc(tableElement.elementText("Comment"));
		List<Element> columnList = tableElement.selectNodes("c:Columns/o:Column");
		for (Element columnElement : columnList) {
			Column columnDTO = new Column(tableDTO, columnElement);
			tableDTO.getColumnList().add(columnDTO);
		}
		fillPrimaryKeyColumn(tableDTO, tableElement);
		return tableDTO;
	}

}
