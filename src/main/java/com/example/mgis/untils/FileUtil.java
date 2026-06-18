package com.example.mgis.untils;

import org.springframework.util.StringUtils;
import java.util.UUID;

public class FileUtil {
    /**
     * 生成唯一文件名：UUID + 原后缀
     */
    public static String getUniqueFileName(String originalFilename) {
        String suffix = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID().toString().replace("-", "") + "." + suffix;
    }
}