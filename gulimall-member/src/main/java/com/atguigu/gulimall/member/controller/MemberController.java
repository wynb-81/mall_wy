 package com.atguigu.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.to.SocialUser;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;

import static com.atguigu.common.exception.BizCodeEnume.*;


 /**
 * 会员
 *
 * @author wynb-81
 * @email 2739884050@qq.com
 * @date 2025-04-23 14:26:23
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;

     /**
      * 社交登录功能
      * @author wynb-81
      * @create 2025/6/4
      **/
     @PostMapping("/oauth2/login")
     public R oauthLogin(@RequestBody SocialUser socialUser) throws Exception {
         MemberEntity entity =  memberService.oauthLogin(socialUser);
         if (entity ==null){
             return R.error(LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(),
                     LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMsg());
         }else{
             return R.ok().setData(entity);
         }
     }


    /**
     * 注册功能
     * RequestBody注解：将传入的json数据转换为后面的格式
     * @author wynb-81
     * @create 2025/6/4
     **/
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo){
        try {
            memberService.regist(vo);
        }catch (PhoneExistException e){
            R.error(PHONE_EXIST_EXCEPTION.getCode(),
                    PHONE_EXIST_EXCEPTION.getMsg());

        }catch (UsernameExistException e){
            R.error(USERNAME_EXIST_EXCEPTION.getCode(),
                    USERNAME_EXIST_EXCEPTION.getMsg());

        }
        return R.ok();

    }

    /**
     * 登录功能
     * @author wynb-81
     * @create 2025/6/4
     **/
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity entity =  memberService.login(vo);
        if (entity ==null){
            return R.error(LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(), LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }else{
//            return R.ok();
            return R.ok().setData(entity);
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
