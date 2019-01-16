package com.pinyougou.sellergoods.service;

import java.util.List;
import java.util.Map;

import com.pinyougou.pojo.TbBrand;

import entity.PageResult;


public interface BrandService {
	
//	查询所有
	public List<TbBrand> findAll();
	//查询分页
	public PageResult findPage(int pageNum,int pageSize);
	
	//增加品牌
	public void add(TbBrand brand);
	
	//根据id查询
	public TbBrand findOne(long id);
	
	//根据id修改对应品牌
	public void update(TbBrand brand);
	
	//删除选中条目
	public void del(long[] ids);
	
	//多条件模糊查询
	public PageResult findPage(TbBrand brand, int pageNum, int pageSize);
	
	public List<Map> selectOptionList();
}
