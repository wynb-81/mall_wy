package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redisson;

    @GetMapping({"/","/index.html"})
    public String index(Model model){

        //TODO 1、查出所有的一级分类
        List<CategoryEntity> categoryEntities =  categoryService.getLevel1Categorys();

        //视图解析器进行拼串
        //classpath/templates/+返回值+.html
        //有三条黄杠的文件夹就是类路径classpath，一般都是resources
        model.addAttribute("categories",categoryEntities);
        return "index";
    }

    //index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalogJson(){
        Map<String, List<Catalog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;

    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        RLock myLock = redisson.getLock("myLock");
        //加锁
        //1）锁的自动续期：如果业务超长，运行期间会自动给锁续上新的30s，不用担心业务时间长，锁自动过期被删掉
        //2）加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s后自动删除
//        myLock.lock();
        myLock.lock(10, TimeUnit.SECONDS);  //10s的过期时间,会发现没有自动给锁续期。因此自动解锁时间一定要大于业务的执行时间
        try {
            Thread.sleep(30000);
        }catch (Exception e){

        }finally {
            myLock.unlock();
        }
        return "hello";
    }



}
