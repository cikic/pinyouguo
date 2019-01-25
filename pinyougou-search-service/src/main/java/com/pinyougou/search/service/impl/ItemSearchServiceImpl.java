package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;

import org.springframework.data.solr.core.query.result.HighlightPage;


import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;


@Service
public class ItemSearchServiceImpl implements ItemSearchService {
	@Autowired
	private SolrTemplate solrTemplate;

	/*
	Query query=new SimpleQuery();
	//添加查询条件
	Criteria criteria=new
	Criteria("item_keywords").is(searchMap.get("keywords"));
	query.addCriteria(criteria);
	ScoredPage<TbItem> page = solrTemplate.queryForPage(query, TbItem.class);
	map.put("rows", page.getContent());
	*/
	@Override
	public Map search(Map searchMap) {
		Map map=new HashMap();
		//将多个关键词中的空格去除
		String keywords = (String) searchMap.get("keywords");
		searchMap.put("keywords", keywords.replaceAll(" ", ""));
		System.out.println( searchMap.get("keywords"));
		//1	按关键词查询 
		map.putAll(searchList(searchMap));

		//2 根据关键字查询商品分类
		List<String> categoryList = searchCategoryList(searchMap);
		map.put("categoryList", categoryList);
		//3 查询品牌和规格列表
		String category = (String) searchMap.get("category");
		//页面初始化显示默认第一个分类的属性，点击切换则跳转对应分类
		if(!"".equals(category)) {
			//将分类集合中的第一个分类显示其对应的品牌，规格等
			map.putAll(searchBrandAndSpecList(category));
		}else {
			if(categoryList.size()>0) {
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}
		}
		return map;
	}
	
	private Map searchList(Map searchMap){
		Map map = new HashMap<>();
		HighlightQuery query =new SimpleHighlightQuery();
		
		HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");
		highlightOptions.setSimplePrefix("<em style = 'color:red'>");//前缀
		highlightOptions.setSimplePostfix("</em>");//后缀
		query.setHighlightOptions(highlightOptions);
		
		//按照关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		
		//按照商品分类过滤
		if(!"".equals(searchMap.get("category")) && searchMap.get("category") != null) {//如果用户选择了分类
			FilterQuery filterQuery=new SimpleFilterQuery();
			Criteria filterCriteria=new Criteria("item_category").is(searchMap.get("category"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);		
		}
		//按照品牌过滤
		if(!"".equals(searchMap.get("brand")) && searchMap.get("brand") != null) {
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
			filterQuery.addCriteria(filterCriteria );
			query.addFilterQuery(filterQuery);
		}
		
		//按照规格过滤
		if(searchMap.get("spec") != null && searchMap.get("spec") != null) {
			Map<String,String> specMap = (Map<String, String>) searchMap.get("spec");
			for(String key : specMap.keySet()) {
				
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_spec_"+key).is(specMap.get(key));
				filterQuery.addCriteria(filterCriteria );
				query.addFilterQuery(filterQuery );
			}
		}
		//按照价格过滤
		if(!"".equals(searchMap.get("price"))) {
			String[] price = ((String)searchMap.get("price")).split("-");
			//如果区间开始不是0
			if(!price[0].equals("0")) {
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);//大于等于
				filterQuery.addCriteria(filterCriteria );
				query.addFilterQuery(filterQuery);
			}
			//如果区间终点不是*
			if(!price[1].equals("*")) {
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
				filterQuery.addCriteria(filterCriteria );
				query.addFilterQuery(filterQuery);
			}
		}

		
		Integer pageNo = (Integer) searchMap.get("pageNo");
		
		if(searchMap.get("pageNo") == null) {
			pageNo = 1;
		}
		Integer pageSize = (Integer) searchMap.get("pageSize");
		if(pageSize == null) {
			pageSize = 20;
		}
		query.setOffset((pageNo-1) * pageSize);//开始位置
		query.setRows(pageSize);//每页查询的记录数
		
		
		String sortValue = (String) searchMap.get("sort");//排序方式
		String sortField = (String) searchMap.get("sortField");//排序字段
		
		
		//按照字段排序排序 先判断sort是否为空，在根据不同的排序方式排序字段
		if(!"".equals(sortValue) && sortValue != null) {
			//排序方式为asc
			if(sortValue.equals("ASC")) {
				Sort sort =new Sort(Sort.Direction.ASC,"item_"+sortField);
				query.addSort(sort );
			}
			if(sortValue.equals("DESC")) {
				Sort sort = new Sort(Sort.Direction.DESC,"item_"+sortField);
				query.addSort(sort );//排序
			}
		}
		

		//***********  获取高亮结果集  ***********
		//高亮页对象
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		for(HighlightEntry<TbItem> h :page.getHighlighted()) {
			TbItem item = h.getEntity();//获得原实体类
			if(h.getHighlights().size()>0 && h.getHighlights().get(0).getSnipplets().size()>0) {
				//将高亮显示后的内容赋值给title
				item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));
			}
		}
		map.put("rows", page.getContent());
		map.put("totalPage", page.getTotalPages());//将总页面数存入集合
		map.put("total", page.getTotalElements());//将总记录数存入集合
		return map;
	}

	private List<String> searchCategoryList(Map searchMap) {
		List<String> list = new ArrayList<>();//创建String类型的集合用来存储名称

		Query query = new SimpleQuery("*:*");
		//按照关键词查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		//设置分组选项
		GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
		
		query.setGroupOptions(groupOptions);
		//得到分组页
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query , TbItem.class);
		//数据列得到分组结果集
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		//获取分组结果入口页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		//得到分组入口集合
		List<GroupEntry<TbItem>> content = groupEntries.getContent();
		for(GroupEntry<TbItem> entry:content){
			list.add(entry.getGroupValue());//将分组结果的名称封装到返回值中
//			System.out.println(entry.getGroupValue());
		}
			return list;
	}
	
	@Autowired
	private RedisTemplate redisTemplate;
	
	
	private Map searchBrandAndSpecList(String category){
		Map map=new HashMap();
		//1.根据商品分类名称得到模板ID		
		Long templateId= (Long) redisTemplate.boundHashOps("itemCat").get(category);
		if(templateId!=null){
			//2.根据模板ID获取品牌列表
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(templateId);
			map.put("brandList", brandList);	
			//System.out.println("品牌列表条数："+brandList.size());
			
			//3.根据模板ID获取规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(templateId);
			map.put("specList", specList);		
			//System.out.println("规格列表条数："+specList.size());
		}	
		
		return map;
	}
	/**
	 * 导入数据
	 */
	@Override
	public void importList(List list) {
		solrTemplate.saveBeans(list);
		solrTemplate.commit();
	}

	/**
	 * 删除solr数据库中的数据
	 */
	@Override
	public void deleteByGoodsId(List goodsIds) {
		
		Query query = new SimpleQuery("*:*");
		Criteria criteria = new Criteria("item_goodsid").in(goodsIds);
		query.addCriteria(criteria);
		solrTemplate.delete(query );
		
	}
	
	
	
}
