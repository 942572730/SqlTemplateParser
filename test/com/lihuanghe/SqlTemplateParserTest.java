package com.lihuanghe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.lihuanghe.SqlTemplateParser.SqlParseException;


public class SqlTemplateParserTest {

	@Test
	public void testsql() throws SqlParseException, IOException
	{
		String sql = "select * from shops \nwhere 1=1 @[Ids: \nand  id in ('Nouse',@{Ids})  ] \nand status = 1";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Ids", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql = SqlTemplateParser.parseString(sql, map, param);
		String expect = "select * from shops \nwhere 1=1  \nand  id in ('Nouse',?,?,?)   \nand status = 1";
		System.out.println(pstsSql);
		System.out.println(Arrays.toString(param.toArray()));
		Assert.assertEquals(expect, pstsSql);
		Assert.assertEquals(3, param.size());
		Assert.assertArrayEquals(new String[]{"2" ,"3" ,"4" }, param.toArray());
		
	}
	@Test
	public void testall() throws SqlParseException, IOException
	{
		String sql = "begin ${p1},@{p2},p#{p1},$[p1: midle1:${p1}],\n $[p5: midle2:${p1}],@[p2:midle3:@{p2}],@[p5:midle4:@{p2}],\\${,\\a,\\\\ end";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("p1", "1");
		map.put("p2", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql = SqlTemplateParser.parseString(sql, map, param);
		String expect = "begin ?,?,?,?,p1, midle1:?,\n ,midle3:?,?,?,,${,\\a,\\ end";
		Assert.assertEquals(expect, pstsSql);
		Assert.assertEquals(8, param.size());
		Assert.assertArrayEquals(new String[]{"1" ,"2" ,"3" ,"4" ,"1" ,"2" ,"3" ,"4" }, param.toArray());
	}

	@Test
	public void testlost() throws SqlParseException, IOException
	{
		String sql = "begin ${p1,@{p2},p#{p1},$[p1: midle1:${p1}], $[p5: midle2:${p1}],@[p2:midle3:@{p2}],@[p5:midle4:@{p2}],\\${,\\a,\\\\ end";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("p1", "1");
		map.put("p2", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql = SqlTemplateParser.parseString(sql, map, param);
		String expect = "begin ?,p1, midle1:?, ,midle3:?,?,?,,${,\\a,\\ end";
		Assert.assertEquals(expect, pstsSql);
		Assert.assertEquals(5, param.size());
		Assert.assertArrayEquals(new String[]{"","1" ,"2" ,"3" ,"4" }, param.toArray());
	}
	
	//����ȱ�ٴ�����
	@Test
	public void testlost1() throws SqlParseException, IOException
	{
		String sql = "begin ${p1,@{p2},p#{p1},$[p1: midle1:${p1}], $[p5: midle2:${p1}],@[p2:midle3:@{p2}],@[p5:midle4:@{p2}],\\${,\\a,\\\\ end";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("p1", "1");
		map.put("p2", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql = SqlTemplateParser.parseString(sql, map, param);
		String expect = "begin ?,p1, midle1:?, ,midle3:?,?,?,,${,\\a,\\ end";
		Assert.assertEquals(expect, pstsSql);
		Assert.assertEquals(5, param.size());
		Assert.assertArrayEquals(new String[]{"","1" ,"2" ,"3" ,"4" }, param.toArray());
	}
	
	//����ȱ�ٴ�����
	@Test
	public void testlost2() throws IOException 
	{
		String sql = "begin ${p1},@{p2},p#{p1},$[p1: midle1:${p1}], $[p5: midle2:${p1}],@[p5:midle3:@{p2}],@[p2:midle4:@{p2],\\${,\\a,\\\\ end";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("p1", "1");
		map.put("p2", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql=null;
		try {
		    pstsSql = SqlTemplateParser.parseString(sql, map, param);
			Assert.assertTrue(false);
		} catch (SqlParseException e) {
			Assert.assertTrue(true);
		}
		Assert.assertEquals((String)null, pstsSql);
	}
	
	@Test
	public void testconcat() throws SqlParseException, IOException
	{
		String sql = "begin ${p1},@{p2},${p#{p1}},#{p#{p1}},$[p1: midle1:${p1},#{p#{p1}} ],\n $[p5: midle2:${p1}],@[p2:midle3:@{p2}],@[p5:midle4:@{p2}],\\${,\\a,\\\\ end";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("p1", "3");
		map.put("p3", "1");
		map.put("p2", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql = SqlTemplateParser.parseString(sql, map, param);
		String expect = "begin ?,?,?,?,?,1, midle1:?,1 ,\n ,midle3:?,?,?,,${,\\a,\\ end";
		Assert.assertEquals(expect, pstsSql);
		Assert.assertEquals(9, param.size());
		Assert.assertArrayEquals(new String[]{"3" ,"2" ,"3" ,"4" ,"1","3" ,"2" ,"3" ,"4" }, param.toArray());
	}
	@Test
	public void testCNcode() throws SqlParseException, IOException
	{
		String sql = "��ʼ ${����1},@{����2},����#{����1},$[����1: �м�1:${����1}], $[����5: �м�2:${����1}],@[����2:�м�3:@{����2}],@[����5:midle4:@{����2}],\\${,\\a,\\\\ ����";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("����1", "1");
		map.put("����2", new String[]{"2","3","4"});
		List<String> param = new ArrayList<String>();
		String pstsSql = SqlTemplateParser.parseString(sql, map, param);
		String expect = "��ʼ ?,?,?,?,����1, �м�1:?, ,�м�3:?,?,?,,${,\\a,\\ ����";
		Assert.assertEquals(expect, pstsSql);
		Assert.assertEquals(8, param.size());
		Assert.assertArrayEquals(new String[]{"1" ,"2" ,"3" ,"4" ,"1" ,"2" ,"3" ,"4" }, param.toArray());
	}
}
