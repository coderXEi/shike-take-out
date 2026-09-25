package com.shike.controller.admin;

import com.shike.constant.MessageConstant;
import com.shike.result.Result;
import com.shike.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@Slf4j
@RequestMapping("/admin/common")
@RestController
@Api(tags = "公共接口服务")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传接口")
    public Result<String> fileUpload(MultipartFile file) {
        log.info("文件上传{}",file);
        // 调用aliyun oss 上传该文件

        try {

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                originalFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() +originalFilename;
            String path = aliOssUtil.upload(file.getBytes(),newFileName);
            return Result.success(path);
        } catch (IOException e) {
            log.error("文件上传失败:{}",e.getMessage());

        }

        return Result.error(MessageConstant.UPLOAD_FAILED);



    }
}
