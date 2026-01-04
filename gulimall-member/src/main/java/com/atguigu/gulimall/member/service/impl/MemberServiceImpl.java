package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.to.SocialUser;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    MemberLevelDao memberLevelDao;
    @Autowired
    MemberDao memberDao;

    /**
     * 社交帐号登录或者注册会员账号
     * @author wynb-81
     * @create 2025/6/5
     **/
    @Override
    public MemberEntity oauthLogin(SocialUser socialUser) throws Exception {
        String uid = socialUser.getUid();
        //1.判断当前用户是否登陆过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null){
            //已经注册过了，重新更换令牌信息
            MemberEntity updateEntity = new MemberEntity();
            updateEntity.setId(memberEntity.getId());
            updateEntity.setAccessToken(socialUser.getAccess_token());
            updateEntity.setExpiresIn(socialUser.getExpires_in());

            memberDao.updateById(updateEntity);

            memberEntity.setExpiresIn(socialUser.getExpires_in());
            memberEntity.setAccessToken(socialUser.getAccess_token());
            return memberEntity;
        }else {
            //进行注册
            MemberEntity regist = new MemberEntity();
            try {
                //1.查出当前社交用户的社交帐号信息
                Map<String,String> query = new HashMap<>();
                query.put("access_token",socialUser.getAccess_token());
                query.put("uid",socialUser.getUid());
                HttpResponse response = HttpUtils.
                        doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<>(), query);
                if (response.getStatusLine().getStatusCode() ==200){
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String name = jsonObject.getString("name"); //获取微博的名称当作默认名称
                    String gender = jsonObject.getString("gender");

                    regist.setNickname(name);
                    regist.setGender("m".equals(gender)?1:0);
                }
            }catch (Exception e){
                System.out.println("远程查询用户的微博信息失败，插入数据库失败");
            }
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());
            regist.setSocialUid(socialUser.getUid());

            memberDao.insert(regist);

            return regist;
        }
    }

    /**
     * 登录
     * @author wynb-81
     * @create 2025/6/4
     **/
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        MemberDao memberDao1 = this.baseMapper;
        MemberEntity entity = memberDao1.selectOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginacct).or().eq("mobile", loginacct));
        if (entity == null){
            //登陆失败
            return null;
        }else {
            //继续验证密码
            //1.获取数据库中的加密密码
            String passwordFromDb = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //2.进行密码匹配
            boolean matches = passwordEncoder.matches(password, passwordFromDb);
            if (matches){
                //匹配成功
                return  entity;
            }else{
                //匹配失败
                return  null;
            }
        }
    }

    /**
     * 注册
     * @author wynb-81
     * @create 2025/6/4
     **/
    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        //存入用户名和手机号，存入之前检查用户名和手机号是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        entity.setNickname(vo.getUserName());

        //对密码进行加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodePassword = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encodePassword);


        //设置会员默认等级
        // TODO 我认为这里有点啰嗦，直接设置为默认值就得了，没必要从数据库再查一遍\
        MemberLevelEntity levelEntity =  memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        //TODO 其他默认信息

        //保存用户信息
        memberDao.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        MemberDao memberDao = this.baseMapper;
        //去数据库查询是否有这个手机号
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count >0){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException{
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count >0){
            throw new UsernameExistException();
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

}