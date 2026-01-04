package com.atguigu.gulimallauthserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.MemberRespVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimallauthserver.feign.MemberFeignService;
import com.atguigu.gulimallauthserver.feign.ThirdPartyFeignService;
import com.atguigu.gulimallauthserver.vo.UserLoginVo;
import com.atguigu.gulimallauthserver.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;
import static com.atguigu.common.constant.AuthServerConstant.SMS_CODE_CACHE_PREFIX;
import static com.atguigu.common.exception.BizCodeEnume.SMS_CODE_EXCEPTION;

/*
25.12.9
* @Controller：Bean注册的常用注解，对应MVC的控制层，主要用于接受用户请求并调用service层返回数据给前端页面
* */
@Controller
public class LoginController {
    /*
    25.12.9
    * 1.@Autowired和@Resource都是注册Bean的注解
    * 2.区别在于，前者默认根据类型匹配，后者根据名称匹配
    * 如果遇见一个接口有两个实现类，那么前者就会发生冲突，不知道选哪个
    * 这个时候就需要一个注解来帮助它指定，是啥注解来？----》@Qualifier("")或者使用@Primary，默认使用标记这个的对象
    * 而后者会根据实现类的姓名来匹配，自动注入，不会出现这中问题

    补充：1.@Autowired支持在构造函数、方法、字段和参数上使用，@Resource主要用于字段和方法
        2.存在多个类的情况下，@Resource需要通过name属性来显示指定名称
    * */
    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 发送验证码
     * @author wynb-81
     * @create 2025/6/4
     **/
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone")String phone){
        //1.TODO 接口防刷

        /*
         *  2.验证码的再次校验：
         *    2.1点击注册的时候，要检查输入的验证码和发送的验证码对不对
         *    2.2防止60秒内频繁发送验证码
         *  这个数据不是永久存储的，验证码一般都有过期时间，所以就存到redis中,存key-phone；value-code
         */
        String redisCode = stringRedisTemplate.opsForValue().get(SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)){
            long codeTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() -codeTime <60000){
                //60秒内不能频繁发送
                return R.error(SMS_CODE_EXCEPTION.getCode(),SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //TODO 调用的这个API的验证码有格式要求
//        String code = UUID.randomUUID().toString().substring(0,7)+"_"+System.currentTimeMillis();
        String code = "123456";
        String redisCode1 = "123456"+"_"+System.currentTimeMillis();
        //redis缓存验证码用于校验
        stringRedisTemplate.opsForValue()
                .set(SMS_CODE_CACHE_PREFIX+phone,redisCode1,5, TimeUnit.MINUTES);  //默认五分钟过期，因为这个API默认五分钟过期
        thirdPartyFeignService.sendCode(phone,code);
        return R.ok();
    }


    /**
     * 注册
     * @author wynb-81
     * @create 2025/6/4
     **/
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if (result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors()
                    .stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            redirectAttributes.addFlashAttribute("errors",errors);
            /*
             * 转发到注册页-forward
            RedirectAttributes :用来重定向视图
            TODO 分布式下的session问题
            如果直接转发到注册的话，会有POST和GET请求不匹配的问题
            如果是直接渲染reg的话，那么表单提交有问题
            重定向携带数据，利用session原理，将数据放到session中，然后重定向之后再从session中取出来
             */
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //1.校验验证码
        String code = vo.getCode();
        String s = stringRedisTemplate.opsForValue().get(SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s)){
            if (code.equals(s.split("_")[0])){
                //用过一次后，立即删除redis中的验证码
                stringRedisTemplate.delete(SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过，调用远程服务进行注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() ==0){
                    //注册成功，返回登录页面
//                    return "redirect:/login.html"; 如果写成这种方式，会跳到主机端口的这个页面，而没有nginx的静态资源的渲染了就
                    return "redirect:http://auth.gulimall.com/login.html";
                }else{
                    Map<String, String> errors =new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    //注册失败，返回注册页面
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            }else{
                Map<String, String> errors =new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                //验证码不对，转发到注册页
                // TODO 这里我觉得应该让重新输入验证码，而不是转发到注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else{
            Map<String, String> errors =new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            //验证码不对，转发到注册页
            // TODO 这里我觉得应该让重新输入验证码，而不是转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }

    }

    /**
     * 登录
     * @author wynb-81
     * @create 2025/6/4
     **/
    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes, HttpSession session){
        R login = memberFeignService.login(vo);
        if (login.getCode() == 0){
            //登陆成功
            //将session存到redis
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        }else {
            //登陆失败，重新定向到登录页面
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    /**
     * auth.gulimall.com判断跳转页
     * @author wynb-81
     * @create 2025/6/5
     **/
    @GetMapping("login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(LOGIN_USER);
        if (attribute ==null){
            //没登录
            return "login";
        }else{
            return "redirect:http://gulimall.com";
        }
    }
}






