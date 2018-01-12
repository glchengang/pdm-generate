package tools.pdmgenerate;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ToJsp implements Handler {

	@Override
	public void handle() throws Exception {
		String dir = Configuration.GENERATE_DIR + "jsp/";
		new File(dir).mkdirs();
		List<Table> tableList = DB.getTableList();
		for (Table tableDTO : tableList) {
			String className = tableDTO.getClassName();
			if (!GenerateUtils.isCreateTable(className))
				continue;
			createJsp(tableDTO);
			createJspDetail(tableDTO);
			createJspTable(tableDTO);
		}
	}

	private static void createJsp(Table tableDTO) throws Exception {
		String className = tableDTO.getClassName();
		StringBuilder buf = new StringBuilder();
		buf.append("<%--\n");
		buf.append("	Document   : " + tableDTO.getDesc() + "\n");
		buf.append("	Created on : " + DateHelper.formatDate(new Date()) + "\n");
		buf.append("	Author     : 陈刚\n");
		buf.append("--%>\n");
		buf.append("<%@page contentType=\"text/html\" pageEncoding=\"UTF-8\"%>\n");
		buf.append("<%@include file=\"/common/_tags.jsp\"%>\n");
		buf.append("<page:applyDecorator name=\"admin\">\n");
		buf.append("<body>\n");
		buf.append("<div class=\"my-module-title\">" + tableDTO.getDesc() + "</div>\n");
		buf.append("\n");
		buf.append("	<admin:query-form>\n");
		for (Column columnDTO : tableDTO.getColumnList()) {
			String attr = columnDTO.getClassAttrName();
			buf.append("		<div class=\"control-group\">\n");
			buf.append("		<label class=\"control-label\">" + columnDTO.getName() + "：</label>\n");
			if (!columnDTO.getJavaDataType().equals("Date")) {//Date自己手动处理
				buf.append("		<input class=\"span2\" name=\"" + attr + "\" type=\"text\" value=\"${query." + attr + "}\">\n");
			}
			buf.append("		</div>\n");
		}
		buf.append("	</admin:query-form>\n");
		buf.append("\n");
		buf.append("	<div class=\"mc_table_toolbar\">\n");
		buf.append("		<button class=\"mc_to_add_button\">添加</button>\n");
		buf.append("		<button class=\"mc_toggle_realdelete_list\" data=\"true\">删除</button>\n");
		buf.append("	</div>\n");
		buf.append("	<%@include file=\"" + className + "Table.jsp\" %>\n");
		buf.append("	<mc:paging-bar/>\n");
		buf.append("\n");
		buf.append("<admin:edit-form>\n");
		List<Column> columnList = tableDTO.getColumnList();
		for (int i = 0; i < columnList.size(); i++) {
			Column columnDTO = columnList.get(i);
			String attr = columnDTO.getClassAttrName();
			buf.append("	<div class=\"control-group\">\n");
			buf.append("		<label class=\"control-label\">" + columnDTO.getName() + "：</label>\n");
			buf.append("		");
			if (columnDTO.getJavaDataType().equals("Date")) {
				buf.append("<admin:input-date name=\"" + attr + "\" style=\"width: 120px\" format=\"yyyy-MM-dd HH:mm:ss\"/>\n");
			} else {
				boolean no = true;
				if (columnDTO.getJavaDataType().equals("String")) {
					int len = getStringTypeLen(columnDTO.getDbDataType());
					if (len > 800) {
						buf.append("<textarea class=\"span6 " + getValidateMeta(columnDTO) + "\" rows=\"8\" name=\"" + attr + "\"></textarea>\n");
						no = false;
					}
				}
				if (no) {
					buf.append("<input type=\"text\" class=\"span2 " + getValidateMeta(columnDTO) + "\" name=\"" + attr + "\">\n");
				}
			}
			buf.append("	</div>\n");
		}
		buf.append("</admin:edit-form>\n");
		buf.append("<page:param name=\"script\">\n");
		buf.append("	<script>\n");
		buf.append("		$(function () {\n");
		buf.append("			Admin.init();\n");
		buf.append("			new ToDetailButton();\n");
		buf.append("			new ToFindButton();\n");
		buf.append("			new ToAddButton();\n");
		buf.append("			new ToEditButton();\n");
		buf.append("			new RealdeleteButton();\n");
		buf.append("		});\n");
		buf.append("	</script>\n");
		buf.append("</page:param>\n");
		buf.append("</body>\n");
		buf.append("</page:applyDecorator>\n");

		FileWriter out = new FileWriter(Configuration.GENERATE_DIR + "jsp/" + className + ".jsp");
		out.write(buf.toString());
		out.close();
	}

	private static String getValidateMeta(Column col) {
		String jtype = col.getJavaDataType();
		StringBuilder buf = new StringBuilder();
		if (StringUtils.isNotBlank(col.getMandatory())) {
			buf.append(", required:true");
		}
		if (jtype.equals("String")) {
			buf.append(", maxlength:" + getStringTypeLen(col.getDbDataType()));
		}
		if (jtype.equals("Integer") || jtype.equals("Long")) {
			buf.append(", digits:true");
		}
		if (jtype.equals("Float")) {
			buf.append(", number:true");
		}
		buf.delete(0, 2);
		return "{" + buf.toString() + "}";
	}

	//取得字符型字段的长度
	private static int getStringTypeLen(String dbDataType) {
		try {
			String s = dbDataType;
			int i1 = s.indexOf("(");
			int i2 = s.indexOf(")");
			if (i1 != -1 && i2 != -1) {
				s = s.substring(i1 + 1, i2);
				return Integer.parseInt(s);
			}
		} catch (Exception e) {
		}
		return 0;
	}

	private static void createJspDetail(Table tableDTO) throws Exception {
		String className = tableDTO.getClassName();
		Column pkColumn = tableDTO.getPrimaryKey();
		String pkCodeAttr = "";
		if (pkColumn != null) {
			pkCodeAttr = pkColumn.getClassAttrName();
		}
		StringBuilder buf = new StringBuilder();
		buf.append("<%@page contentType=\"text/html\" pageEncoding=\"UTF-8\"%>\n");
		buf.append("<%@include file=\"/common/_tags.jsp\" %>\n");
		buf.append("<admin:detail-dialog item=\"${o." + pkCodeAttr + "}\" clazz=\"h-split\">\n");
		for (Iterator<Column> it = tableDTO.getColumnList().iterator(); it.hasNext(); ) {
			buf.append("<tr>\n");
			for (int i = 0; i < 2; i++) {
				if (it.hasNext()) {
					Column columnDTO = it.next();
					String name = columnDTO.getName();
					String attr = columnDTO.getClassAttrName();
					buf.append("<th>" + name + ":</th>");
					if (columnDTO.getJavaDataType().equals("Date")) {
						buf.append("<td><fmt:formatDate value=\"${o." + attr + "}\" pattern=\"yyyy-MM-dd HH:mm:ss\" /></td>\n");
					} else {
						buf.append("<td>${o." + attr + "}</td>\n");
					}
				}
			}
			buf.append("</tr>\n");
		}
		buf.append("</admin:detail-dialog>\n");

		FileWriter out = new FileWriter(Configuration.GENERATE_DIR + "jsp/" + className + "Detail.jsp");
		out.write(buf.toString());
		out.close();
	}

	private static void createJspTable(Table tableDTO) throws Exception {
		String className = tableDTO.getClassName();
		Column pkColumn = tableDTO.getPrimaryKey();
		String pkCodeAttr = "";
		if (pkColumn != null) {
			pkCodeAttr = pkColumn.getClassAttrName();
		}
		StringBuilder buf = new StringBuilder();
		buf.append("<%@page contentType=\"text/html\" pageEncoding=\"UTF-8\"%>\n");
		buf.append("<%@include file=\"/common/_tags.jsp\"%>\n");
		buf.append("<table class=\"table table-bordered table-condensed mc-table-list\" fixed-row=\"20\">\n");
		buf.append("<thead>\n");
		buf.append("	<tr>\n");
		buf.append("		<th class=\"ckb\"><input type=\"checkbox\" name=\"selectAll\" for=\"checkbox\"></th>\n");
		buf.append("		<th class=\"orderNum\">序号</th>\n");
		for (Column columnDTO : tableDTO.getColumnList()) {
			String codedetial = columnDTO.getName();
			String attr = columnDTO.getClassAttrName();
			if (columnDTO.getJavaDataType().equals("Date")) {
				buf.append("<th data-order=\"" + attr + "\" style=\"width:120px;\">" + codedetial + "</th>\n");
			} else {
				buf.append("<th data-order=\"" + attr + "\">" + codedetial + "</th>\n");
			}
		}
		buf.append("<th class=\"btn3\">操作</th>\n");
		buf.append("</tr>\n");
		buf.append("</thead>\n");
		buf.append("<tbody>\n");
		buf.append("	<c:forEach items=\"${page.content}\" var=\"o\" varStatus=\"status\">\n");
		buf.append("		<tr item=\"${o." + pkCodeAttr + "}\">\n");
		buf.append("			<td><input type=\"checkbox\" name=\"checkbox\" value=\"${o." + pkCodeAttr + "}\"></td>\n");
		buf.append("			<td class=\"m-order-num\">${status.count}</td>\n");
		List<Column> columnList = tableDTO.getColumnList();
		for (Iterator<Column> it = columnList.iterator(); it.hasNext(); ) {
			Column columnDTO = it.next();
			String attr = columnDTO.getClassAttrName();
			if (columnDTO.getJavaDataType().equals("Date")) {
				buf.append("<td><fmt:formatDate value=\"${o." + attr + "}\" pattern=\"yyyy-MM-dd HH:mm:ss\"/></td>\n");
			} else {
				buf.append("<td>${o." + attr + "}</td>\n");
			}
		}
		buf.append("<td class=\"my-item-buttons\">\n");
		buf.append("<a href=\"javascript:void(0);\" class=\"mc_to_detail_button\" title=\"详细\"></a>\n");
		buf.append("<a href=\"javascript:void(0);\" class=\"mc_to_edit_button\" title=\"编辑\"></a>\n");
		buf.append("<a href=\"javascript:void(0);\" class=\"mc_toggle_realdelete_one\" title=\"删除\"></a>\n");
		buf.append("</td>\n");
		buf.append("</tr>\n");
		buf.append("</c:forEach>\n");
		buf.append("<c:if test=\"${page.totalElements==0}\">\n");
		buf.append("<tr><td colspan=\"7\">没有符合要求的数据！</td></tr>\n");
		buf.append("</c:if>\n");
		buf.append("</tbody>\n");
		buf.append("</table>\n");
		FileWriter out = new FileWriter(Configuration.GENERATE_DIR + "jsp/" + className + "Table.jsp");
		out.write(buf.toString());
		out.close();

	}

}
