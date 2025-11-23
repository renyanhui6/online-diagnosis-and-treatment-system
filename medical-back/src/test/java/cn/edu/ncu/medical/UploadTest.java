package cn.edu.ncu.medical;

import cn.edu.ncu.medical.config.UploadConfig;
import cn.edu.ncu.medical.utils.UploadUtil;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class UploadTest {
    @Autowired
    private UploadConfig ossConfig;
    @Test
    public void testUpload(){
        String localFilePath = "E:\\1.png";
        String key = "5.png";
        try {
            String upToken = UploadUtil.uploadToken(ossConfig);
            String url = UploadUtil.putPhoto(localFilePath, key, upToken);
            DefaultPutRet putRet = UploadUtil.getResult(localFilePath, key, upToken);
            //解析上传成功的结果
            System.out.println(putRet.key);
            System.out.println(putRet.hash);
        } catch (QiniuException ex) {
            ex.printStackTrace();
        }
    }
}
