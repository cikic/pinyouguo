package com.pinyougou.protal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.content.service.ContentService;
import com.pinyougou.pojo.TbContent;

import entity.Result;

@RestController
@RequestMapping("/content")
public class ContentController {
	//通过接口获取提供者服务
	@Reference
	private ContentService contentService;
	
	/**
	 * 根据广告分类id查询广告分类列表
	 * @param categoryId
	 * @return
	 */
	@RequestMapping("/findByCategoryId")
	public List<TbContent> findByCategoryId(Long categoryId) {
		return contentService.findByCategoryId(categoryId);
	}
}
