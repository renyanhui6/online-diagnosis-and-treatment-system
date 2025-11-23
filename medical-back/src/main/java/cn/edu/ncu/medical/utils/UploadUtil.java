package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.config.UploadConfig;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import java.io.File;
import java.io.InputStream;


public class UploadUtil {
    private static final Configuration cfg;
    private static Auth auth;
    private static final UploadManager uploadManager;
    static {
         cfg= Configuration.create(Region.region2());
         cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
         uploadManager= new UploadManager(cfg);
    }

    /**
     * 下载token
     * @param config
     * @return token
     */
    public static String uploadToken(UploadConfig config){
        auth=Auth.create(config.getAccessKey(), config.getSecretKey());
        return auth.uploadToken(config.getBucket());
    }


    /**
     * 得到结果，可以使用getKey()和getHash()
     * @param path
     * @param key
     * @param token
     * @return
     * @throws QiniuException
     */
    public static String putPhoto(String path,String key,String token) throws QiniuException {
       uploadManager.put(path,key,token);
        return "http://szwlb5xin.hn-bkt.clouddn.com/"+key;
    }
    public static String putPhoto(InputStream inputStream,String key, String token) throws QiniuException {
         uploadManager.put(inputStream,key,token,null,null);
         return "http://szwlb5xin.hn-bkt.clouddn.com/"+key;
    }
    public static DefaultPutRet getResult(String path,String key,String token) throws QiniuException {
        Response response=uploadManager.put(path,key,token);
        return new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
    }
}
