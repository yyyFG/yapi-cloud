package cn.y.yapiinterface.controller;



import cn.y.yapiclientsdk.model.User;
import cn.y.yapiclientsdk.utils.SignUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

/**
 * 名称 API
 */
@RestController
@RequestMapping("/name")
public class NameController {

    // hashSet 用来存随机数
    public static final Set<String> NONCE_SET = new HashSet<>();

    @GetMapping("/get")
    public String getNameByGet(@RequestParam String name) {
        return "GET 你的名字是：" + name;
    }


    @PostMapping("/post")
    public String getNameByPost(@RequestParam String name, HttpServletRequest request) throws UnsupportedEncodingException {
        // 1. 拿到五个参数进行校验，比如 accessKey 先去数据库中查一下
        // 从请求头中获取名为 "accessKey" 的值
        String accessKey = request.getHeader("accessKey");
        // 从请求头中获取名为 "secretKey" 的值
//        String secretKey = request.getHeader("secretKey");
        String nonce = request.getHeader("nonce");
        // URL 解码，防止中文乱码
        String body = URLDecoder.decode(request.getHeader("body"), "UTF-8");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");

        // todo 2. 校验权限，从数据库中判断是否与用户的 accessKey 相同
        if (!accessKey.equals("a6b072c7a80671c2c1e0a9fae4da16e8")) {
            // 抛出一个运行时异常，表示权限不足
            throw new RuntimeException("无权限");
        }

        // todo 3. 校验随机数，随机数可以用 hashMap 或 redis 存储
        if (NONCE_SET.contains(nonce)) {
            throw new RuntimeException("无权限");
        }
        NONCE_SET.add(nonce);

        // todo 4. 校验时间戳和当前时间的差距，和当前时间不能超过五分钟
        long currentTime = System.currentTimeMillis() / 1000;
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(currentTime - requestTime) > 300) {
            throw new RuntimeException("无权限");
        }

        // todo 5. 校验签名，从数据库中获取 secretKey
        String serverSign = SignUtils.genSign(body, "152120b4bcab38863c3dfec04af96e29");
        // 如果生成的签名不一致，则抛出异常
        if (!sign.equals(serverSign)) {
            throw new RuntimeException("无权限");
        }

        return "POST 你的名字是：" + name;
    }


    @PostMapping("/postUser")
    public String getUserNameByPost(@RequestBody User user, HttpServletRequest request) throws UnsupportedEncodingException {
        // 1. 拿到五个参数进行校验，比如 accessKey 先去数据库中查一下
        // 从请求头中获取名为 "accessKey" 的值
        String accessKey = request.getHeader("accessKey");
        // 从请求头中获取名为 "secretKey" 的值
//        String secretKey = request.getHeader("secretKey");
        String nonce = request.getHeader("nonce");
        // URL 解码，防止中文乱码
        String body = URLDecoder.decode(request.getHeader("body"), "UTF-8");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");

        // todo 2. 校验权限，从数据库中判断是否与用户的 accessKey 相同
        if (!accessKey.equals("a6b072c7a80671c2c1e0a9fae4da16e8")) {
            // 抛出一个运行时异常，表示权限不足
            throw new RuntimeException("无权限");
        }

        // todo 3. 校验随机数，随机数可以用 hashMap 或 redis 存储
        if (NONCE_SET.contains(nonce)) {
            throw new RuntimeException("无权限");
        }
        NONCE_SET.add(nonce);

        // todo 4. 校验时间戳和当前时间的差距，和当前时间不能超过五分钟
        long currentTime = System.currentTimeMillis() / 1000;
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(currentTime - requestTime) > 300) {
            throw new RuntimeException("无权限");
        }

        // todo 5. 校验签名，从数据库中获取 secretKey
        String serverSign = SignUtils.genSign(body, "152120b4bcab38863c3dfec04af96e29");
        // 如果生成的签名不一致，则抛出异常
        if (!sign.equals(serverSign)) {
            throw new RuntimeException("无权限");
        }

        // todo 6. 调用次数 + 1
        // 如果权限校验通过，返回 "POST 用户名字是" + 用户名
        return "POST 你的名字是：" + user.getUserName();
    }
}
