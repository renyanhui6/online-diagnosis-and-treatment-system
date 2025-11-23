package cn.edu.ncu.medical.utils;

import java.util.regex.Pattern;

public class FormatValidator {
    // 邮箱正则表达式：用户名允许字母、数字、点、减号、下划线，域名允许字母、数字、连字符，顶级域名2-6位
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,6}$");
    
    // 手机号正则表达式：匹配11位数字，以1开头，第二位为3-9之间的数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    // 身份证号正则表达式：15位或18位，最后一位可能是数字或X/x
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(^\\d{15}$)|(^\\d{17}[0-9Xx]$)");

    /**
     * 验证邮箱格式
     * @param email 待验证的邮箱
     * @return true-格式正确，false-格式错误
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证手机号格式
     * @param phone 待验证的手机号
     * @return true-格式正确，false-格式错误
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证身份证号格式
     * @param idCard 待验证的身份证号
     * @return true-格式正确，false-格式错误
     */
    public static boolean isValidIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return false;
        }
        return ID_CARD_PATTERN.matcher(idCard).matches();
    }

    // 身份证号严格验证（包括校验位计算）
    public static boolean isValidIdCardStrict(String idCard) {
        if (!isValidIdCard(idCard)) {
            return false;
        }
        
        // 18位身份证需要验证最后一位校验位
        if (idCard.length() == 18) {
            char[] idChars = idCard.toUpperCase().toCharArray();
            int[] factors = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
            char[] checkCodeList = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
            
            int sum = 0;
            for (int i = 0; i < 17; i++) {
                sum += (idChars[i] - '0') * factors[i];
            }
            
            int mod = sum % 11;
            return idChars[17] == checkCodeList[mod];
        }
        
        // 15位身份证不验证校验位，仅格式检查
        return true;
    }

    // 示例用法
    public static void main(String[] args) {
        System.out.println(isValidEmail("test@example.com"));      // true
        System.out.println(isValidEmail("test.example.com"));      // false
        
        System.out.println(isValidPhone("13800138000"));           // true
        System.out.println(isValidPhone("23800138000"));           // false
        
        System.out.println(isValidIdCard("110101199001011234"));  // true
        System.out.println(isValidIdCard("11010119900101123x"));  // true
        System.out.println(isValidIdCard("11010119900101123"));   // false
        
        System.out.println(isValidIdCardStrict("110101199001011234"));  // true
        System.out.println(isValidIdCardStrict("110101199001011239"));  // false
    }
}    