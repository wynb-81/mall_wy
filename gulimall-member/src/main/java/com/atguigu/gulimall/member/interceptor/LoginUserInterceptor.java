package com.atguigu.gulimall.member.interceptor;

import com.atguigu.common.to.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser =new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //先判断一下， 如果是服务之间的相互调用，就不需要登录了。
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/member/**", uri);
        boolean match1 = antPathMatcher.match("/payed/notify", uri);
        if (match || match1){
            return true;
        }
        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(LOGIN_USER);
        if (attribute != null){
            loginUser.set(attribute);
            return true;

        }else{
            //需要登录
            request.getSession().setAttribute("msg","请先登录");    //在session中进行提示
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
