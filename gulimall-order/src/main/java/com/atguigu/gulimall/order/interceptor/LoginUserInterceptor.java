package com.atguigu.gulimall.order.interceptor;

import com.atguigu.common.to.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

//    public static ThreadLocal<MemberRespVo> loginUser =new ThreadLocal<>();
    private static final ThreadLocal<MemberRespVo> Login_User = new ThreadLocal<>();

    public static MemberRespVo getLoginUser(){
        return Login_User.get();
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //先执行一次清理，防止OOM
        Login_User.remove();

        //先判断一下， 如果是服务之间的相互调用，就不需要登录了。
        String uri = request.getRequestURI();
        //白名单现在写死，应该抽取为一个方法，方便以后添加更多的路径
//        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        if (isWhiteList(uri)){
            return true;
        }

        MemberRespVo user = (MemberRespVo) request.getSession().getAttribute(LOGIN_USER);
        if (user != null){
            Login_User.set(user);
            return true;
        }else{
            //需要登录
            request.getSession().setAttribute("msg","请先登录");    //在session中进行提示
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }

    /**
     * 用完之后清理线程，防止OOM
     * @author wynb-81
     * @create 2026/1/9
     **/
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        Login_User.remove();
    }
    /**
     * 白名单
     * @author wynb-81
     * @create 2026/1/9
     **/
    private boolean isWhiteList(String uri){
        AntPathMatcher pathMatcher = new AntPathMatcher();
        String[] whiteList = {
                "/order/order/status/**"
        };
        for(String pattern : whiteList){
            if(pathMatcher.match(pattern,uri)){
                return true;
            }
        }
        return false;

    }
}
