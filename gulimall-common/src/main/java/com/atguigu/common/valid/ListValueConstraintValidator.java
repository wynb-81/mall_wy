package com.atguigu.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class ListValueConstraintValidator implements ConstraintValidator<ListValue, Integer> {

    private Set<Integer> set = new HashSet<>();
    //初始化方法
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        //这里最好进行非空判断，防止数组里面没有数据
        for (int val : vals) {
            set.add(val);
        }
    }

    //判断是否校验成功
    /**
    *
     * @Param value 需要校验的值
     * @Param context
    * */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        //直接返回set里面包不包含这个value，相当于先if后返回
        return set.contains(value);
    }
}
