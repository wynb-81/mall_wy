package com.atguigu.gulimallcart.interceptor;

import com.atguigu.common.to.MemberRespVo;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.UUID;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;
import static com.atguigu.common.constant.CartConstant.TEMP_USER_COOKIE_NAME;
import static com.atguigu.common.constant.CartConstant.TEMP_USER_COOKIE_TIMEOUT;


@Component
public class CartInterceptor implements HandlerInterceptor {
    public static  ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 在执行controller方法之前，判断用户的登录状态，并封装传递给controller目标请求
     * @author wynb-81
     * @create 2025/6/6
     **/
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        if (session!=null){
            System.out.println("Cart服务中的sessionId：" + session.getId());
            MemberRespVo member = (MemberRespVo) session.getAttribute(LOGIN_USER);
            System.out.println("Cart服务中获取LOGIN_USER：" + member);
            if (member != null){
                //登陆
                userInfoTo.setUserId(member.getId());

            }
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length >0){
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if (name.equals(TEMP_USER_COOKIE_NAME)){
                        userInfoTo.setUserKey(cookie.getValue());
                    /*
                     如果将tempUser这个变量设置为了true，那么就说明获取到了临时用户信息，也就是说不是第一次登录了，
                     那么在下面post方法中就可以根据这个变量来判断，是否需要刷新临时用户key的过期时间
                     */
                        userInfoTo.setTempUser(true);
                    }
                }
            }
            //如果没有临时用户，则分配一个临时用户key
            if (StringUtils.isEmpty(userInfoTo.getUserKey())){
                String uuid = UUID.randomUUID().toString();
                userInfoTo.setUserKey(uuid);
            }

            //目标方法执行之前，将用户信息放到threadLocal中，这样目标方法就能快速获取其中的内容
            threadLocal.set(userInfoTo);
        }else {
            System.out.println("Cart服务中未获取到session");
        }


        return true;
    }

    /**
     * 业务执行之后，要让浏览器保存cookie，过期时间为一个月
     * @author wynb-81
     * @create 2025/6/6
     **/
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //从threadLocal中拿到userKey，保存这个临时用户的userKey
        UserInfoTo userInfoTo = threadLocal.get();
        if (!userInfoTo.isTempUser()){
            //没有临时用户的key
            Cookie cookie = new Cookie(TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }

    }
}
