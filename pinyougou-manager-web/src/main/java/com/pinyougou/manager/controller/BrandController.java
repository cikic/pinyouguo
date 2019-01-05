package com.pinyougou.manager.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbBrand;
import com.pinyougou.sellergoods.service.BrandService;

import entity.PageResult;
import entity.Result;



@RestController
@RequestMapping("/brand")
public class BrandController {
	
	@Reference
	private BrandService brandService;
	
	@RequestMapping("/findAll")
	public List<TbBrand> findAll(){
		
		return brandService.findAll();
	}
	
	//分页
	@RequestMapping("/findPage")
	public PageResult findPage(int page,int size) {
		
		return brandService.findPage(page, size);
	}
	
	//add增加
	
	@RequestMapping("/add")
	public Result add(@RequestBody TbBrand brand) {
		try {
			brandService.add(brand);
			return new Result(true,"增加成功");
		}catch(Exception e) {
			e.printStackTrace();
			return new Result(false,"增加失败");
		}
		
	}
	
	//	根据id查询brand
	@RequestMapping("/findOne")
	public TbBrand findOne(long id) {
		return brandService.findOne(id);
	}
	
	//	根据id修改对应品牌
	@RequestMapping("/update")
	public Result update(@RequestBody TbBrand brand) {
		try {
			brandService.update(brand);
			return new Result(true,"修改成功");
		}catch(Exception e) {
			e.printStackTrace();
			return new Result(true,"修改失败");
		}
	}
	
	
	//删除选中条目
	@RequestMapping("/dele")
	public Result del(long[] ids) {
		try {
			brandService.del(ids);
			return new Result(true,"删除成功");
		}catch(Exception e) {
			e.printStackTrace();
			return new Result(false,"删除失败");
		}
	}
	
	//模糊查询
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbBrand brand,int page,int size) {
		PageResult result =  brandService.findPage(brand,page,size);
		System.out.println(result.getTotal());
		return result;
	}
	
	
}
