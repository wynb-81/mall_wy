package com.atguigu.gulimallauthserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.to.SocialUser;
import com.atguigu.common.utils.R;
import com.atguigu.gulimallauthserver.feign.MemberFeignService;
import com.atguigu.common.to.MemberRespVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class OAuth2Controller {
    @Autowired
    MemberFeignService memberFeignService;


    /**
     * 社交帐户登录
     * @author wynb-81
     * @create 2025/6/5
     **/
    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        Map<String,String > map =  new HashMap<>();
        map.put("client_id","");
        map.put("client_secret","");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code",code);
        //1.根据code换取access token
        HttpResponse response = HttpUtils
                .doPost("https://api.weibo.com", "/oauth2/access_token", "post", null, null, map);

        //2.处理
        if (response.getStatusLine().getStatusCode() ==200){
            //获取到了access token
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //已知了当前是哪个社交用户，判断是否是第一次登录，是则需要注册一个会员账号
            R r = memberFeignService.oauthLogin(socialUser);
            if (r.getCode() ==0){
                //成功
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登陆成功，用户信息："+data.toString());
                //第一次使用session，命令浏览器保存卡号，JSESSIONID这个cookie
                //以后浏览器访问哪个网站就会带上这个网站的cookie
                //子域之间，gulimall.com auth.gulimall.com order.gulimall.com 等待
                //发卡的时候（指定域名为父域名），即使是子域系统发的卡，也能让父域使用
                session.setAttribute("loginUser",data);
                return "redirect:http://gulimall.com";
            }else{
                //失败
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else{
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
