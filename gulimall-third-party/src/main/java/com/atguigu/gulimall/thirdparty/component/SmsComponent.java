package com.atguigu.gulimall.thirdparty.component;


import com.atguigu.gulimall.thirdparty.util.HttpUtils;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "alibaba.cloud.sms")
@Data
@Component
public class SmsComponent {
    private String host;
    private String path;
    private String smsSignId;   //短信前缀 默认2e65b1bb3d054466b82f0c9d125465e2
    private String templateId;  //短信模板 默认908e94ccf08b4476ba6c876d13f084ad
    private String appcode;

    public void sendCode(String phone,String code){
//        String host = "https://gyytz.market.alicloudapi.com";
//        String path = "/sms/smsSend";
        String method = "POST";
//        String appcode = "88c095fb2497432eacba91aa0d1cb961";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);
        //code:"**code**:333333,**minute**:5"
        code = "**code**:"+code+",**minute**:5";
        querys.put("param", code);

        //smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html

        querys.put("smsSignId", smsSignId);
        querys.put("templateId", templateId);
        Map<String, String> bodys = new HashMap<String, String>();


        try {
            /**
             * 重要提示如下:
             * HttpUtils请从\r\n\t    \t* https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java\r\n\t    \t* 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
