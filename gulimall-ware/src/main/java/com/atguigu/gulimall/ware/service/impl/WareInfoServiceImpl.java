package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.feign.MemberFeignService;
import com.atguigu.gulimall.ware.vo.FareVo;
import com.atguigu.gulimall.ware.vo.MemberAddressVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareInfoDao;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;
import com.atguigu.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            queryWrapper.and(w->{
                w.eq("id", key).or()
                        .like("name", key).or()
                        .like("address", key);
            });
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据地址计算运费
     * @author wynb-81
     * @create 2025/6/18
     **/
    @Override
    public FareVo getFare(Long attrId) {
        FareVo fareVo = new FareVo();
        R r = memberFeignService.addrInfo(attrId);
        MemberAddressVo data = r.getData("memberReceiveAddress",new TypeReference<MemberAddressVo>(){});
        if (data!=null){
            String phone = data.getPhone();
            //将电话号最后一位作为运费
            String substring = phone.substring(phone.length() - 1);
            BigDecimal bigDecimal = new BigDecimal(substring);
            fareVo.setAddress(data);
            fareVo.setFare(bigDecimal);
            return fareVo;
        }
        return null;
    }

}