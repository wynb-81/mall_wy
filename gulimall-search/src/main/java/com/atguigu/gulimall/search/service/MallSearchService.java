package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    /**
     *
     * @param param 检索所有的参数SearchParam封装的Vo
     * @return  返回检索的结果SearchResult封装的Vo
     */
    SearchResult search(SearchParam param);
}
