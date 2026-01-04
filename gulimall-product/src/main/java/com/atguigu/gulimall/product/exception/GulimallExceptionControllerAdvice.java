package com.atguigu.gulimall.product.exception;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


/*
* 集中处理异常
* */
@Slf4j
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
//@ResponseBody
//@ControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {


    //处理数据校验异常
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        log.error("数据校验出现问题{},异常类型:{}",e.getMessage(),e.getClass());
        BindingResult bindingResult = e.getBindingResult(); //拿到数据校验的结果

        //拿到所有的错误信息和错误出现的属性名，装到map里
        Map<String, String> map = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError) -> {
            map.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        return R.error(BizCodeEnume.VALID_EXCEPTION.getCode(),BizCodeEnume.VALID_EXCEPTION.getMsg()).put("data",map);

    }

    //总的异常处理，如果出现的异常没有和上面的异常处理方法匹配上，那么最终就会来到这个方法里
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable) {
        return R.error(BizCodeEnume.UNKNOWN_EXCEPTION.getCode(), BizCodeEnume.UNKNOWN_EXCEPTION.getMsg());
    }
}
