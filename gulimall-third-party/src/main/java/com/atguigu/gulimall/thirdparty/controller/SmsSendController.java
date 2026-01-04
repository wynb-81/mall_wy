package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sms")
public class SmsSendController {
    @Autowired
    SmsComponent smsComponent;
    /**
     * 提供给别的服务进行调用
     * @param phone
     * @param code
     * @return R
     * @author wynb-81
     * @create 2025/6/3
     **/
    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone") String phone,@RequestParam("code") String code){
        smsComponent.sendCode(phone,code);
        return R.ok();
    }
}
