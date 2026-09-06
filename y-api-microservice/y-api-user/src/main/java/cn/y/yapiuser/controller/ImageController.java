package cn.y.yapiuser.controller;


import cn.hutool.core.io.FileUtil;

import cn.y.yapicommon.common.BaseResponse;
import cn.y.yapicommon.common.ErrorCode;
import cn.y.yapicommon.common.ResultUtils;
import cn.y.yapicommon.exception.BusinessException;
import cn.y.yapicommon.manager.CosManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;



@RestController
@RequestMapping("/image")
public class ImageController {

    @Resource
    private CosManager cosManager;

    @PostMapping("/upload")
    public BaseResponse<String> imageUpload (@RequestPart MultipartFile file) {
        // 文件大小检查
        long size = file.getSize();
        if (size > 5 * 1024 * 1024L) {
            throw new RuntimeException("图片大小不能超过5MB");
        }
        // 文件格式检查
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        final List<String> validFileSuffixList = Arrays.asList("jpg", "png", "jpeg", "gif", "bmp");
        if (!validFileSuffixList.contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件后缀不合法，请重新上传");
        }
        String cosUrl = null;

        try {
            if (!file.isEmpty()) {
                cosUrl = cosManager.upLoadImage(file);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        }

        return ResultUtils.success(cosUrl);
    }


}
