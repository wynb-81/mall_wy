package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.atguigu.gulimall.search.config.GulimallElasticsearchConfig.COMMON_OPTIONS;
import static com.atguigu.gulimall.search.constant.EsConstant.PRODUCT_INDEX;
import static com.atguigu.gulimall.search.constant.EsConstant.PRODUCT_PAGESIZE;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) {
        SearchResult result = null;

        //1.准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, COMMON_OPTIONS);

            //3.分析返回数据response，封装成需要的格式
            result = buildSearchResult(response,param);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    /**
     * 检索请求
     * @param param 页面传递过来的检索条件
     * @return  返回检索请求给ES
     * @author wynb-81
     * @create 2025/5/26
     **/
    private SearchRequest buildSearchRequest(SearchParam param) {
        //构建检索所需的两个条件：
        //1.在那个索引构建；2.创建构建索引语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /*
        * 模糊匹配，过滤
        * must和filter
        * */
        //1.构建boolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            //1.1如果param里面的keyword有值，那么进行模糊匹配must
            if (StringUtils.hasText(param.getKeyword())) {
                boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
            }

            //1.2filter分四层，一层一层地查,这些属性都需要判断页面查询条件到底带没带这个值？带了再查，不带不查，filter里面都是term查询，所以用termQuery
                //1.2.1按照三级分类ID查询
                if (param.getCatalog3Id() != null) {
                    boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
                }

                //1.2.2按照品牌ID查询，并且可能有多个值
                if (param.getBrandId()!=null && param.getBrandId().size()>0){
                    boolQuery.filter(QueryBuilders.termQuery("brandId", param.getBrandId()));
                }

                //1.2.3按照所有指定的属性查询,并且属性是嵌入式的nested,nested需要三个查询条件：1.path；2.查询条件:bool类型，里面是must，然后是terms；3.ScoreMode，评分模式
                if (param.getAttrs()!=null && param.getAttrs().size()>0){
                    for (String attrStr : param.getAttrs()) {
                        BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                        //查询条件，如attrs=1_5寸:8寸&attrs=2_16G:8G
                        String[] attrValue = attrStr.split("_");
                        String attrId = attrValue[0];   //检索的属性id
                        String[] attrValues = attrValue[1].split(":");   //这个属性检索用的值
                        nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                        nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                        //每一个条件都得生成一个查询，如果把下面两行放外面的话，那就是一次性查id既等于1又等于2的，那就不太对了
                        NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs",nestedBoolQuery,None);
                        boolQuery.filter(nestedQuery);
                    }
                }

                //1.2.4按照是否有库存查询,这个直接进行判断就OK了
                boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock()==1));

                //1.2.5按照价格区间查询，还是先判断传没传这个参数,价格区间用rangeQuery()方法查询
                if(StringUtils.hasText(param.getSkuPrice())){
                    RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
                    //并且价格区间可能有三种情况：两边都传了参数；左边传右边没传；右边传左边没传
                    String[] priceRange = param.getSkuPrice().split("_");
                    if(priceRange.length==2){   //两边都传了参数
                        rangeQuery.gte(priceRange[0]).lte(priceRange[1]);
                    } else if (priceRange.length ==1) {
                        if(param.getSkuPrice().startsWith("_")){
                            rangeQuery.lte(priceRange[0]);  //右边传左边没传,都小于这个值
                        }
                        if(param.getSkuPrice().endsWith("_")){
                            rangeQuery.gte(priceRange[0]);  //左边传右边没传，都大于这个值
                        }
                    }
                    boolQuery.filter(rangeQuery);
                }

        //封装上面传来的所有查询条件，接下来还有两部分要封装到sourceBuilder中
        sourceBuilder.query(boolQuery);

        /*
        * 排序，分页，高亮
        * */
        //2.1排序:hotScore_asc/desc,前面是按照什么排序，后面是排序规则
        if(StringUtils.hasText(param.getSort())){
            String[] sortValue = param.getSort().split("_");
            SortOrder order =  sortValue[1].equalsIgnoreCase("asc")?ASC:DESC;
            sourceBuilder.sort(sortValue[0],order);
        }

        //2.2分页 pageSize = 5
        // pageNum:1 from:0 size:5 [0,1,2,3,4]
        // pageNum:2 from:5 size:5
        // from = (pageNum -1)*size
        sourceBuilder.from((param.getPageNum() -1)*PRODUCT_PAGESIZE);
        sourceBuilder.size(PRODUCT_PAGESIZE);

        //2.3高亮:生效条件：只有传入了模糊查询，才需要将模糊查询的部分标为高亮
        if (StringUtils.hasText(param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            //设置高亮部分，以及前后缀（负责渲染）
            builder.field("skuTitle");
            builder.preTags("<b style='color:red>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /*
        * 聚合分析
        * */
        //1.品牌聚合brand_agg
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        //品牌聚合的子聚合brand_name_agg和brand_img_agg
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        //2.分类聚合catalog_agg
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(50);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        //3.属性聚合attrs_agg
        NestedAggregationBuilder attrs_agg = AggregationBuilders.nested("attrs_agg","attrs");
        //聚合出属性对应的id
        TermsAggregationBuilder attrs_id_agg = AggregationBuilders.terms("attrs_id_agg").field("attrs.attrId").size(1);
        //聚合id对应的属性名以及值
        attrs_id_agg.subAggregation(AggregationBuilders.terms("attrs_name_agg").field("attrs.attrName").size(1));
        attrs_id_agg.subAggregation(AggregationBuilders.terms("attrs_value_agg").field("attrs.attrValue").size(50));
        attrs_agg.subAggregation(attrs_id_agg);
        sourceBuilder.aggregation(attrs_agg);



        System.out.println("构建的DSL"+ sourceBuilder);
        System.out.println("使用的ES索引为:"+PRODUCT_INDEX);
        SearchRequest searchRequest = new SearchRequest(new String[]{PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 检索结果封装
     * @param
     * @return
     * @author wynb-81
     * @create 2025/5/26
     **/
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        //1.返回查询到的所有商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if(hits.getHits() != null && hits.getHits().length>0){
            for (SearchHit hit : hits.getHits()) {
                //每一条hit记录里面的_source是数据真正的信息
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (StringUtils.hasText(param.getKeyword()) ){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highlight_skuTitle = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(highlight_skuTitle);
                }

                esModels.add(esModel);
            }
        }
        result.setProduct(esModels);

        //2.当前所有商品涉及到的所有属性信息
        ParsedNested attrs_agg = response.getAggregations().get("attrs_agg");
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedLongTerms attrs_id_agg = attrs_agg.getAggregations().get("attrs_id_agg");
        for (Terms.Bucket bucket : attrs_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo= new SearchResult.AttrVo();
            //获得id,这里之前我陷入了误区，以为只会有一个id，其实有多个属性，就会有多个id，后来反应过来了
            Long attr_id = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attr_id);
            //获得属性名
            ParsedStringTerms attrsNameAgg = bucket.getAggregations().get("attrs_name_agg");
            String attrs_name = attrsNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrs_name);
            //获得属性值,
            List<String> attrs_values = ((ParsedStringTerms) bucket.getAggregations().get("attrs_value_agg")).getBuckets().stream().map(item->{
                String keyAsString = ((Terms.Bucket)item).getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attrs_values);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        //2.当前所有商品涉及到的所有品牌信息
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //得到品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //得到品牌名字
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            String brand_name = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brand_name);
            //得到品牌图片
            String brand_img =((ParsedStringTerms) bucket.getAggregations()
                    .get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brand_img);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //2.当前所有商品涉及到的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            long catalogId = bucket.getKeyAsNumber().longValue();
            catalogVo.setCatalogId(catalogId);
            //得到分类名，分类名是子聚合，所以还要查一下bucket里面的聚合
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //5.分页信息——页码
        result.setPageNum(param.getPageNum());
        //5.分页信息——总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //5.分页信息——总页码
        int totalPages = (int)total%PRODUCT_PAGESIZE == 0 ? (int)total%PRODUCT_PAGESIZE : (int)(total%PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageNavs.add(i);
        }

        //6.面包屑导航功能
        if(param.getAttrs() !=null && param.getAttrs().size()>0){
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr->{
                //6.1分析每个attr传过来的参数值,如attrs=2_5寸
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
//                result.g().add(Long.parseLong(s[0]));

                if(r.getCode()==0){
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {});
                    navVo.setNavName(data.getAttrName());
                }else{
                    navVo.setNavName("没有查询到该属性的名字");
                }

                //6.2取消了面包屑以后，跳转的位置

                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
        }

        //品牌、分类的面包屑导航
        if (param.getBrandId() != null && param.getBrandId().size()>0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //远程查询所有品牌
            R r = productFeignService.brandInfo(param.getBrandId());
            if (r.getCode()==0){
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>(){});
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for(BrandVo brandVo:brand){
                    buffer.append(brandVo.getBrandName()+";");
                    replace = replaceQueryString(param,brandVo.getBrandId()+"","brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
            }
            navs.add(navVo);
        }

        return result;
    }

    private static String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+","%20");

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return param.get_queryString().replace("&"+key+"="+encode,"");

    }
}
