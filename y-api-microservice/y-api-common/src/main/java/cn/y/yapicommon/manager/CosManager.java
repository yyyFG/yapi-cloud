package cn.y.yapicommon.manager;

import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.config.CosClientConfig;
import cn.y.yapicommon.exception.BusinessException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Cos 对象存储操作
 */
@Component
@ConditionalOnBean(COSClient.class)
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 cos 并返回访问 URL
     * @param key
     * @param file
     * @return 文件访问的 URL，失败返回 null
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 构建访问URL
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("文件上传COS成功：{} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空");
            return null;
        }
    }

    /**
     * 只要调用静态方法upLoadFile(MultipartFile multipartFile)就可以获取上传后文件的全路径
     *
     * @param file
     * @return 返回文件的浏览全路径
     */
    public String upLoadImage(MultipartFile file) {
        try {
            // 获取上传的文件的输入流
            InputStream inputStream = file.getInputStream();

            // 避免文件覆盖，获取文件的原始名称，如123.jpg,然后通过截取获得文件的后缀，也就是文件的类型
            String originalFilename = file.getOriginalFilename();
            //获取文件的类型
            String fileType = originalFilename.substring(originalFilename.lastIndexOf("."));

            String fileName = UUID.randomUUID().toString().substring(0,8) + "_avatar.jpg";
            String cosKey = generatePictureKey(fileName);
            // 创建上传Object的Metadata
            ObjectMetadata objectMetadata = new ObjectMetadata();
            // - 使用输入流存储，需要设置请求长度
            objectMetadata.setContentLength(inputStream.available());
            // - 设置缓存
            objectMetadata.setCacheControl("no-cache");
            // - 设置Content-Type
            objectMetadata.setContentType(fileType);

            //上传文件
//            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, file);
            PutObjectResult result = cosClient.putObject(cosClientConfig.getBucket(), cosKey, inputStream, objectMetadata);
//            System.out.println(result);

            if (result != null) {
                // 构建访问URL
                String url = String.format("https://" + "%s/%s", cosClientConfig.getHost(), cosKey);
                log.info("文件上传COS成功：{} -> {}", file.getName(), url);
                return url;
            } else {
                log.error("文件上传COS失败，返回结果为空");
                return null;
            }

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败");
        }
    }

    /**
     * 生成图片存储 key：所有头像统一存放在 avatar 目录下
     *
     * @param fileName 文件名
     * @return COS 中的存储 key，如 avatar/xxx_avatar.jpg
     */
    private String generatePictureKey(String fileName) {
        return "avatar/" + fileName;
    }
}
