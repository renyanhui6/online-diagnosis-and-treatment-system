package cn.edu.ncu.medical.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256Util {
    /**
     * 对输入字符串进行SHA-256加密
     * @param input 待加密的字符串
     * @return 加密后的十六进制字符串
     */
    public static String encrypt(String input) {
        try {
            // 获取SHA-256摘要实例
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 计算摘要
            byte[] digest = md.digest(input.getBytes());
            // 将字节数组转换为十六进制字符串
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 处理算法不存在的异常
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            // 将每个字节转换为两位的十六进制字符串
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static void main(String[] args) {
        // 测试示例
        String originalString = "Hello, World!";
        String encryptedString = SHA256Util.encrypt(originalString);
        System.out.println("原始字符串: " + originalString);
        System.out.println("SHA-256加密后: " + encryptedString);
    }
}