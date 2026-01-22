package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
 public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    //分页查询
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    //查找所有分类，并按树形结构排列
    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2.组装成父子的树形结构

        //2.1找到所有的一级分类
        List<CategoryEntity> levle1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() ==0
        ).map((menu)->{ //不能将menu理解成levle1Menus中的CategoryEntity对象。这是还在流中间处理过程中的每个一级分类对象，最终在收集到List中才会成为levle1Menus的对象
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))
        //.sorted((menu1,menu2)->{
        //            return (menu1.getSort()==null?0: menu1.getSort()) - (menu2.getSort()==null?0: menu2.getSort());
        //        }
        ).collect(Collectors.toList());
        return levle1Menus;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> categoryEntity.getParentCid() == root.getCatId()
        ).map(categoryEntity -> {
            //1.找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))
        ).collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> aslist) {

        baseMapper.deleteBatchIds(aslist);

    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);//递归查找

        Collections.reverse(parentPath); //因为查找出来的顺序是反的，即三级-二级-一级这样，需要给它倒过来，成正确的一级-二级-三级这样
        return  paths.toArray(new Long[parentPath.size()]);
    }


    @CacheEvict(value ="category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

    }

    /*
     * 查询一级分类
     * 一级分类有两个查询条件可以满足
     * 1.parent_id == 0
     * 2.cat_level == 1
     * */
    // 每一个需要缓存的数据我们都来指定要放到那个名字的缓存 【即缓存的分区（按照业务类型分）】
    @Cacheable(value = {"category"},key = "#root.method.name") //代表当前方法的结果需要缓存，如果缓存中有，方法不用调用。如果缓存中没有，会调用方法，最后将方法的返回保存到缓存中
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0L));
    }

    @Cacheable(value = {"category"},key = "#root.method.name",sync = true)
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        List<CategoryEntity> level1Categorys = getParentCid(selectList,0L);
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> categoryEntities = getParentCid(selectList,v.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (categoryEntities != null) {
                catalog2Vos = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(
                            v.getCatId().toString(),
                            null,
                            l2.getCatId().toString(),
                            l2.getName());

                    List<CategoryEntity> level3Catalog = getParentCid(selectList,l2.getCatId());

                    if (level3Catalog != null ) {
                        List<Catalog2Vo.Catalog3Vo> catalog3VoList = level3Catalog.stream().map(l3 -> {
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(
                                    l2.getCatId().toString(),
                                    l3.getCatId().toString(),
                                    l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(catalog3VoList);

                    }

                    return catalog2Vo;
                }).collect(Collectors.toList());

            }
            return catalog2Vos;
        }));

//        无需像之前那样，将数据手动放到缓存里
//        String s = JSON.toJSONString(parent_cid);
//        redisTemplate.opsForValue().set("catalogJson", s,1, TimeUnit.DAYS);
        return parent_cid;
    }

    /*
     * 使用redis缓存渲染二级三级分类数据
     * */
    //TODO 进行压力测试时产生堆外内存溢出异常
    /*
    * 异常产生原因：
    * springboot2.0以后默认使用lettuce作为操作redis的客户端，它使用netty进行网络通信
    * lettuce的bug导致netty的堆外内存溢出  在启动product模块的时候设置了最大JVM内存为300 -Xmx300m
    * 如果没有指定堆外内存大小，netty会默认使用-Xmx的值作为其的值
    * 解决方案：
    * 不能单独去调大堆外内存
    * 1）升级lettuce客户端
    * 2）切换使用jedis
    * 使用第二种方式
    * */
//    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        //这个方法就是先调用从数据库中查找三级分类数据的方法getCatalogJsonFromDb
        //        然后将结果放到redis中，以后每次查询都去redis中查，这样就节省了查询和计算的时间
        //给缓存中放json字符串，拿出的json字符串，还要逆转为能用的对象类型，这就是序列化和反序列化
        //1.加入缓存逻辑，缓存中存放的是json字符串，因为json跨语言跨平台兼容
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if(!StringUtils.hasText(catalogJson)){  //这里由于StringUtils的isEmpty()方法已经弃用，所以换成了hasText
            //使用这个hasText需要注意一下，这个的判断和isEmpty()是正好相反的
            //缓存中没有，就调用下面的方法从数据库中查，然后再加入缓存
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();
            return catalogJsonFromDb;
        }
        Map<String, List<Catalog2Vo>> result =
                JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {});

        return result;
    }

    //使用redisson锁来查三级分类数据
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        //获取分布式锁,锁的名字涉及到了锁的粒度，粒度越细，锁的越少，运行速度也就越快
        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catalog2Vo>> dataFromDb;
        try {
            dataFromDb= getDataFromDb();
        }finally {
            lock.unlock();
        }
        return dataFromDb;
    }


    //使用分布式锁来查三级分类数据
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        //1.加分布式锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,300, TimeUnit.SECONDS);
        if(lock){
            //给lock设置过期时间
            //redisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            //加锁成功，执行业务
            Map<String, List<Catalog2Vo>> dataFromDb;
            try {
                dataFromDb= getDataFromDb();
            }finally {
                //使用lua脚本，将查询uuid和删除锁改为原子操作
                String script = "if redis.call('get',KEYS[1])== ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDb;
        }else {
            //自旋方式重新申请锁
            try {
                Thread.sleep(300);
            }catch (InterruptedException e) {

            }
            return getCatalogJsonFromDbWithRedisLock();
        }

    }

    //从数据库中查询三级分类数据
    private Map<String, List<Catalog2Vo>> getDataFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if(StringUtils.hasText(catalogJson)) {
            Map<String, List<Catalog2Vo>> result =
                    JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {});

            return result;
        }
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        List<CategoryEntity> level1Categorys = getParentCid(selectList,0L);
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> categoryEntities = getParentCid(selectList,v.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (categoryEntities != null) {
                catalog2Vos = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(
                            v.getCatId().toString(),
                            null,
                            l2.getCatId().toString(),
                            l2.getName());

                    List<CategoryEntity> level3Catalog = getParentCid(selectList,l2.getCatId());

                    if (level3Catalog != null ) {
                        List<Catalog2Vo.Catalog3Vo> catalog3VoList = level3Catalog.stream().map(l3 -> {
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(
                                    l2.getCatId().toString(),
                                    l3.getCatId().toString(),
                                    l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(catalog3VoList);

                    }

                    return catalog2Vo;
                }).collect(Collectors.toList());

            }
            return catalog2Vos;
        }));

        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJson", s,1, TimeUnit.DAYS);
        return parent_cid;
    }

    //从数据库查询，渲染二级三级分类数据，加入本地锁，已弃用
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDb() {
        //本地锁只能锁住当前进程，即product进程，但是锁不住其他进程
        synchronized (this){
            //得到锁之后，再去缓存中查一遍，如果缓存中没有再去数据库中查
            return getDataFromDb();
        }

    }

    //查找父分类的id
    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid().equals(parent_cid)).collect(Collectors.toList());
        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1.收集当前节点id
        paths.add(catelogId);

        CategoryEntity byId = this.getById(catelogId);  //先查出当前分类的信息

        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);  //继续查找父节点有没有父节点

        }
        return paths;
    }
}