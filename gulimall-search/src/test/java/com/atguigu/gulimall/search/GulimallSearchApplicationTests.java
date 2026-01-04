package com.atguigu.gulimall.search;

import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.alibaba.fastjson.JSON;

import java.io.IOException;

import static com.atguigu.gulimall.search.config.GulimallElasticsearchConfig.COMMON_OPTIONS;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Test
    public void search() throws IOException {
        //1.创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");
        //指定DSL，检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构造检索条件
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        System.out.println(searchSourceBuilder.toString());

        searchRequest.source(searchSourceBuilder);

        //2.执行检索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, COMMON_OPTIONS);

        //3.分析结果
        System.out.println(searchResponse.toString());


    }








    @Test
    public void contextLoads() {
        System.out.println(restHighLevelClient);
    }

    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("user");
        indexRequest.id("1");
        User user = new User();
        user.setAge(18);
        user.setUsername("www");
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        //这里API接口不会的（不知道需要传多少，传哪些参数，就去看官方文档）
        indexRequest.source(jsonString, XContentType.JSON);
        IndexResponse index = restHighLevelClient.index(indexRequest, COMMON_OPTIONS);
        System.out.println(index);

    }

    @Data
    class User{
        private String username;
        private String gender;
        private Integer age;
    }

}
