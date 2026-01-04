package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id = "9021000149688764";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key ="MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDPhgq3+hXWu9+DZq5QSXE/w7912dYmsRPhawySaA6Le73bIzXfhtvSVl4pFMuffL/gQ9j4QQPQQQaGrxHNyRZtlc/mqC/YLZs24n2+02YQcPQKKIW3WPmXv5bpYY+UsxwLh4NhaWR5+lo4rf1j8H2mVLeLGKztFQLH6fCFhycATerMFyIiVpxNSruGxeAZ2QVu49O8Ny6mzVkFV7D8En04BnQOCNZwYRHctP3kCpPrjNZdZnDG5SxjnKIPpaV2ExeKzB0/n7kLNE4Zx798jeGbU/1e/VyXEf6ChXyOb3Dq3fyYQThRAMvkSXzZO3sjSywflxsvgHp6d2I6idqyQkmDAgMBAAECggEBALLM2Nte/AYB5Th/9dxzCsJX09SklaDMnUZxR2m1giKnwRLYKYin1ypJA+P9aNvHTkLZ8k0jJzf9brQIjxxxD4PnujMP4fkugjctug36ckvkJj/CXkN0w8f9aPc3+c+y6oz8ezQo7Es0axu+vT2obXosL+AGqKe0nKrGY1eI9Q+KhH26WlyLcSSC/2getnKFr9qXRcr65RJ75uKdDwH7ay12ayG5MXLK+1Jwy+RkNZ5SHDixDWjSmPHr7BKka+mshI1gOqBP3ySPvDnk6WaVDTGon8RXBJU7Y4oI2WVvHicy57/XqqGX5W8coQBcOwamYns3dCYL0T5+DOJYqDr3HLECgYEA93Focq41hn0zSXirmqUYTHd8z5TVapO9Wmpapj4pkCvfX3a+ymO/2kjFzwnt2kzLsDYlgMW8ZN0BuP7rvDOGC3TY2+H9rYpL3VPcIhac/9uILjrsTfArlB6ceIewMwEkup0hyrtD6iQ8xm7xJTnkmAZkCEIXlqMh38Y9GVyAggUCgYEA1rM7FaqZF/0/Iki5DExylkX1EH5RKcugA9XBKZyTfyfWV3G9WhewpjiPwZNJSEHORrOhIhWY4XlvcPldyKmHCyb7/LtsiBZz4nGbQ0Cbv75DumiO6Jqfd/52XGoDv3nFz9GmrzMzMEvqAITC+Jqpe1P+KyO95Byct/7aEW3Xy+cCgYBQ1BrqWz3g0MUQGvrzaTqmv+FlbZjJV7li75rs8yFglvRAmul3812YUG1NVFD+HlfrF8Toa68+w3Lsm1kmAS6oT2MHcKsNJT/i7KHcAMcITQ4PC/BlBS83E0jJlolYH+d/jhSbxNBKi8vOhi7/mgmyH2RNzkkmS+Ok1Xzf6/eOgQKBgBVaYgf3nZ5LK5pHTVAx0jur3YG0bnIPtGxtN/bhgbHlzmWIBGMPdBw0B+gis+kh0lgpyV7QcxIJt9Gum2s3oRy01d7+7P5j7UaaMezwYg0h6S6C86OirTWL802hpZnHjKrsmP1XxEGLa0x9574985k2c1LrDRnA1r1d9WZCXHIFAoGBAKp3HoAIE17JxSdjmDG2UYCIPPKPZlQQ8cOt+KlBK3NjKOrPzFe0Ei3fljZMkDFcThfIAldnDaWmj5mJWR3UA3WS0thRtExUVAlS27Ubw/Hnh8KIQOe7IeizVCP1weG3jik1G0khHpMEkwhv2RfPSjCPrdMZeuEFQhRpv3MLOUcb";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhOgQi+l2+i0rxax/MUVW4ydiUIWs5s4GQexYCIIM7AOy1Ka3rALuT2rjaXD5eaLlVpktdsbGFXte51XEHLaWyjsIKfDpVZMS4UxlkzCgKbIm/fseY0hpMWog+yvB16SoG95udAIecE4SO3UX821S3yFU4bltooriXd0JtVo7EhuTYs/jVaHTMuvhz0LaUcyOaKOZvQOekmwxsDorJHvxUno52PR4gRXEVuyNpGPTB7TTDzmgm1yR9P5WeVQdbmIPmWJUpb6ESslWnMUxShWLvmoYHbx3+8o8nGxZGbNDaE3cuSTPF+hrrf5GugXBv5IIITHHwAYwAu3y/T4LcgrZKwIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    public String notify_url = "http://member.gulimall.com/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    //订单超时时间
    private String timeout = "1m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    public String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
