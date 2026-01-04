package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.atguigu.gulimall.search.config.GulimallElasticsearchConfig.COMMON_OPTIONS;
import static com.atguigu.gulimall.search.constant.EsConstant.PRODUCT_INDEX;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    /**
     * 将一批 SKU 数据写入 Elasticsearch。
     * @param skuEsModels 需要同步到 ES 的 sku 模型列表
     * @return 写入成功（全部无异常）则返回 true，任意一条失败则返回 false
     * @throws IOException 当与 ES 通信异常时抛出
     */
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModels) {
            IndexRequest indexRequest = new IndexRequest(PRODUCT_INDEX);
            System.out.println("使用的ES索引为:"+PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            indexRequest.source(JSON.toJSONString(model), XContentType.JSON);
            // 调试阶段可以打开下面这一行，确保数据立刻可见
            // indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            bulkRequest.add(indexRequest);
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, COMMON_OPTIONS);

        // 如果存在任何一条写入失败
        if (bulkResponse.hasFailures()) {
            for (BulkItemResponse itemResponse : bulkResponse.getItems()) {
                if (itemResponse.isFailed()) {
                    String failedId = itemResponse.getId();
                    String failureMsg = itemResponse.getFailureMessage();
                    log.error("ES 写入失败 → SKU ID = {}, 原因：{}", failedId, failureMsg);
                }
            }
            return false;
        } else {
            // 全部成功
            List<String> successIds = Arrays.stream(bulkResponse.getItems())
                    .map(BulkItemResponse::getId)
                    .collect(Collectors.toList());
            log.info("商品上架成功，已写入 ES 的 SKU 列表：{}", successIds);
            return true;
        }
    }

}
