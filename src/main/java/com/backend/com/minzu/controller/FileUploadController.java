package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "images";

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("图片大小不能超过10MB");
        }

        try {
            String dateDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
            Path uploadPath = Paths.get(UPLOAD_DIR, dateDir);
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 返回可访问的URL（通过 /api/upload/image/ 接口读取）
            String url = "/api/upload/image/" + dateDir + "/" + fileName;
            return Result.success(url);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败，请重试");
        }
    }

    /** 获取已上传的图片（{fileName:.+} 确保带扩展名的文件名能正确匹配） */
    @GetMapping("/image/{dateDir}/{fileName:.+}")
    public void getImage(@PathVariable String dateDir, @PathVariable String fileName, HttpServletResponse response) {
        Path filePath = Paths.get(UPLOAD_DIR, dateDir, fileName);
        File file = filePath.toFile();
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        String name = fileName.toLowerCase();
        if (name.endsWith(".png")) response.setContentType("image/png");
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) response.setContentType("image/jpeg");
        else if (name.endsWith(".gif")) response.setContentType("image/gif");
        else if (name.endsWith(".webp")) response.setContentType("image/webp");
        else response.setContentType("application/octet-stream");

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) os.write(buf, 0, len);
            os.flush();
        } catch (IOException e) {
            response.setStatus(500);
        }
    }
}
