package com.lihuanghe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/*
 * ���̰߳�ȫ��ÿ�ν�����Ҫ newһ���¶���
 * 
 ${paramter#{String}}  -- ��ѡ����
 $[paramter: and column = ${} ]  --��ѡ����,���paramterֵΪ�գ���䲻������
 @{array}  --��ѡ����չ��
 @[array: and id in (@{array})]   --��ѡ���飬�������Ϊ�գ�����䲻������

 #{String}  --�ַ���ƴ�ӣ����ڴ���̬����

 */

public class SqlTemplateParser {

	/**
	 * @param in
	 *            �����ԭʼsql
	 * @param param
	 *            �û�����Ĳ������߻�������
	 * @throws UnsupportedEncodingException
	 **/
	protected SqlTemplateParser(InputStream in, String charset,
			Map<String, Object> param) throws UnsupportedEncodingException {
		this.in = new InputStreamReader(in, charset);
		this.param = param;
	}

	private InputStreamReader in;
	private Map<String, Object> param;
	private List<String> pstsParam;
	private int curChar = -1;
	private int prechar = -1;
	private int sqlpos = 0; // ���ڼ�¼��ǰ��ȡ���ĸ��ַ�
	private ParamType paramtype; // ��������ǰҪ����Ĳ��������Ƿ�������

	public class SqlParseException extends RuntimeException {

		static final long serialVersionUID = -7034897190745766939L;

		public SqlParseException(String s) {
			super(s);
		}
	}

	private enum ParamType {
		String, Array
	}

	private static SqlTemplateParser createParser(InputStream in, Map map,
			String charset) {
		try {
			return new SqlTemplateParser(in, charset == null ? "utf-8"
					: charset, map);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * @param sql
	 *            ��������sql
	 * @param map
	 *            �洢��������
	 * @param pstsParam
	 *            �����洢����֮��Ĳ���
	 * @return sql ������ɵ�sql
	 */
	public static String parseString(String sql, Map map, List<String> pstsParam)
			throws SqlParseException, IOException {
		return parseString(sql, map, pstsParam, null);
	}

	/**
	 * @param sql
	 *            ��������sql
	 * @param map
	 *            �洢��������
	 * @param pstsParam
	 *            �����洢����֮��Ĳ���
	 * @return sql ������ɵ�sql
	 */
	public static String parseString(String sql, Map map,
			List<String> pstsParam, String charset) throws SqlParseException,
			IOException {
		InputStream in = null;

		try {
			in = new ByteArrayInputStream(
					sql.getBytes(charset == null ? "utf-8" : charset));
			return parseString(in, map, pstsParam, charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}
		}
	}

	/**
	 * @param in
	 *            ��InputStream��ȡsql��
	 * @param map
	 *            �洢��������
	 * @param pstsParam
	 *            �����洢����֮��Ĳ���
	 * @return sql ������ɵ�sql
	 */
	public static String parseString(InputStream in, Map map,
			List<String> pstsParam) throws SqlParseException, IOException {
		return parseString(in, map, pstsParam, null);
	}

	/**
	 * @param in
	 *            ��InputStream��ȡsql��
	 * @param map
	 *            �洢��������
	 * @param pstsParam
	 *            �����洢����֮��Ĳ���
	 * @return sql ������ɵ�sql
	 * @throws IOException
	 */
	public static String parseString(InputStream in, Map map,
			List<String> pstsParam, String charset) throws SqlParseException,
			IOException {
		SqlTemplateParser parser = createParser(in, map, charset);
		return parser.parse(pstsParam);
	}

	/**
	 * @param pstsParam
	 *            �����洢����֮��Ĳ���
	 * @return sql ������ɵ�sql
	 */
	protected String parse(List<String> pstsParam) throws SqlParseException,
			IOException {
		this.pstsParam = pstsParam;
		return statmentUntilEnd();
	}

	protected String statmentUntilEnd() throws IOException, SqlParseException {
		StringBuilder sqlbuf = new StringBuilder();
		while ((curChar = readandsavepre()) != -1) {
			sqlbuf.append(statment());
		}
		return sqlbuf.toString();
	}

	protected String statment() throws IOException, SqlParseException {

		switch (curChar) {
		case '$':
			paramtype = ParamType.String;
			return paramter();
		case '@':
			paramtype = ParamType.Array;
			return paramter();
		case '#':
			return parseConcat();
		case '\\':
			return escape();
		default:
			return String.valueOf((char) curChar);
		}
	}

	protected String escape() throws IOException, SqlParseException {
		curChar = readandsavepre();
		StringBuilder escapestr = new StringBuilder();
		switch (curChar) {
		case -1:
			throw new SqlParseException("expect escape char '\\' ");
		case '\\':
		case '|':
		case '@':
		case '$':
		case '#':
		case ':':
		case '{':
		case '}':
		case '[':
		case ']':
			escapestr.append((char) curChar);
			break;
		default:
			escapestr.append((char) prechar).append((char) curChar);
		}
		return escapestr.toString();
	}

	protected String paramter() throws IOException, SqlParseException {
		curChar = readandsavepre();
		switch (curChar) {
		case -1:
			return String.valueOf((char) prechar);
		case '[':
			return optionalParameter();
		case '{':
			return requiredParameter();
		default:
			return new StringBuilder().append((char) prechar)
					.append((char) curChar).toString();
		}
	}

	protected String optionalParameter() throws IOException, SqlParseException {
		// ��ȡ����
		ParameterEval paramName = readParamerUtil(':');

		if (paramName.isArray()) {
			Object obj = paramName.getValueFromMap(param);

			if (obj == null) {
				// ������ȡ��String
				readUntil(']', false);
				return "";
			}

			Collection<String> set = null;
			if (obj instanceof Collection) {
				set = (Collection<String>) obj;
			} else if (obj instanceof String[]) {
				set = Arrays.asList((String[]) obj);
			}
			// ����Ǽ�ȫ���ͣ��ҳ���Ϊ0
			if (set != null && set.size() == 0) {
				// ������ȡ��String
				readUntil(']', false);
				return "";
			}
			String statement = statmentUntil(']');
			return statement;
		} else {
			Object obj = paramName.getValueFromMap(param);
			String str = String.valueOf(obj == null ? "" : obj);
			if ("".equals(str)) {
				// ������ȡ��String
				readUntil(']', false);
				return "";
			} else {
				String statement = statmentUntil(']');
				return statement;
			}
		}
	}

	protected String statmentUntil(int until) throws IOException,
			SqlParseException {

		StringBuilder sqlbuf = new StringBuilder();

		curChar = readandsavepre();

		while (curChar != -1 && curChar != until) {
			sqlbuf.append(statment());
			curChar = readandsavepre();
		}
		if (curChar == -1) {
			throw new SqlParseException("\texpect '" + (char) until + "'");
		}
		return sqlbuf.toString();
	}

	// �����ѡ����
	protected String requiredParameter() throws IOException, SqlParseException {

		// ��ȡ������
		ParameterEval paramName = readParamerUtil('}');
		return addpstsParam(paramName);
	}

	private String addpstsParam(ParameterEval paramName) {
		StringBuilder sqlbuf = new StringBuilder();

		Object obj = paramName.getValueFromMap(param);
		if (obj == null) {
			return "";
		}

		Collection<String> set = null;
		if (obj instanceof Collection) {
			set = (Collection<String>) obj;
/*			set = new ArrayList<String>();
			set.addAll((Collection<String>)obj);*/
		} else if (obj instanceof String[]) {
			set = Arrays.asList((String[]) obj);
		}

		// ������Ǽ�������.
		if (set == null) {
			pstsParam.add(String.valueOf(obj));
			return "?";
		}

		if (set != null && set.size() > 0) {
			for (String p : set) {
				pstsParam.add(p);
				sqlbuf.append('?').append(',');
			}
			sqlbuf.deleteCharAt(sqlbuf.length() - 1);
			return sqlbuf.toString();
		} else // ����Ϊ��
		{
			// do Nothing
			return "";
		}
	}

	private String parseConcat() throws IOException, SqlParseException {
		curChar = readandsavepre();
		StringBuilder sqlbuf = new StringBuilder();
		ParameterEval paramName = null;
		switch (curChar) {
		case -1:
			sqlbuf.append((char) prechar);
			break;
		case '{':
			paramName = readParamerUtil('}');
			break;
		default:
			sqlbuf.append((char) prechar).append((char) curChar);
		}

		// �ѻ�ȡ������Ĳ�����
		if(paramName!=null){
			String tmp = (String) paramName.getValueFromMap(param);
			if(tmp!=null)
			{
				sqlbuf.append(tmp);
			}
		}
		return sqlbuf.toString();
	}

	private String readUntil(int c, boolean isparseParamName)
			throws IOException, SqlParseException {
		curChar = readandsavepre();
		StringBuilder strbuf = new StringBuilder();
		while (curChar != -1 && curChar != c) {

			if (isparseParamName) {
				strbuf.append(parseParameter());
			} else {
				// ����Ҫ�������ַ������ٽ���
				strbuf.append((char) curChar);
			}

			curChar = readandsavepre();
		}
		if (curChar == -1) {
			throw new SqlParseException(strbuf.append(
					" :position[" + (sqlpos - strbuf.length()) + "]\t expect '"
							+ (char) c + "'").toString());
		} else {
			return strbuf.toString();
		}
	}

	private String parseParameter() throws SqlParseException, IOException {
		switch (curChar) {
		case '\r':
		case '\n':
			throw new SqlParseException("parameter contains '\\r' ,'\\n' ");
		case '\\':
			return escape();
		case '#':
			return parseConcat();
		default:
			return String.valueOf((char) curChar);
		}
	}

	private ParameterEval readParamerUtil(int c) throws IOException,
			SqlParseException {
		curChar = readandsavepre();
		StringBuilder strbuf = new StringBuilder();
		while (curChar != -1 && curChar != '|' && curChar != c) {
			
			String tmp = parseParameter();
			strbuf.append(tmp);
			curChar = readandsavepre();
		}
		//ȥ���������Ŀո�
		String name = strbuf.toString().trim();
		// ������Ϊ��
		if (StringUtils.isEmpty(name)) {
			throw new SqlParseException(" after \"" + (char) prechar
					+ (char) curChar + "\", Parameter Name is null at position : "
					+ sqlpos);
		}

		if (curChar == -1) {
			throw new SqlParseException(strbuf.append(
					" :position[" + (sqlpos - strbuf.length()) + "]\t expect '"
							+ (char) c + "'").toString());
		} else if (curChar == '|') {
			
			// ��ȡfilter���ݣ�Ӧ����һ�ο�ִ�е�js�ű�
			String jsCode = readUntil(c, true);
			return new ParameterEval(name, jsCode,
					ParamType.Array.equals(paramtype));
		} else {
			return new ParameterEval(name, null,
					ParamType.Array.equals(paramtype));
		}
	}
	
	private int readandsavepre() throws IOException {
		prechar = curChar;
		curChar = in.read();
		sqlpos++;
		return curChar;
	}

}