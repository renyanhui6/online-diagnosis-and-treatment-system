package cn.edu.ncu.medical.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCache {
	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	private ObjectMapper objectMapper = new ObjectMapper();

	public RedisCache setString(String key, String value) {
		stringRedisTemplate.opsForValue().set(key, value);
		return this;
	}

	public String getString(String key) {
		return stringRedisTemplate.opsForValue().get(key);
	}

	public void setExpire(String key, Integer expire, TimeUnit timeUnit) {
		stringRedisTemplate.expire(key, expire, timeUnit);
	}

	public RedisCache setObject(String key, Object object) throws Exception {
		String value = objectMapper.writeValueAsString(object);
		stringRedisTemplate.opsForValue().set(key, value);
		return this;
	}

	public Object getObject(String key, Class<?> clazz) throws Exception {
		String jsonObject = stringRedisTemplate.opsForValue().get(key);
		if (jsonObject == null) {
			return null;
		}
		return objectMapper.readValue(jsonObject, clazz);
	}

	public void delete(String key) {
		if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
			return;
		}
		stringRedisTemplate.delete(key);
	}



	// -------------------------- 新增ZSet操作（支持延迟队列） --------------------------

	/**
	 * 向ZSet中添加元素（用于延迟队列：订单ID+到期时间戳）
	 * @param key ZSet的键（如"order:delay:register"）
	 * @param value 订单ID（字符串形式）
	 * @param score 到期时间戳（毫秒）
	 */
	public void addToZSet(String key, String value, double score) {
		stringRedisTemplate.opsForZSet().add(key, value, score);
	}

	/**
	 * 从ZSet中获取分数范围内的元素（用于扫描到期订单）
	 * @param key ZSet的键
	 * @param minScore 最小分数（如0）
	 * @param maxScore 最大分数（如当前时间戳）
	 * @param limit 最多返回数量
	 * @return 元素集合（订单ID字符串）
	 */
	public Set<String> rangeZSetByScore(String key, double minScore, double maxScore, long limit) {
		return stringRedisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore, 0, limit);
	}

	/**
	 * 从ZSet中删除元素（用于订单支付后移除）
	 * @param key ZSet的键
	 * @param values 要删除的订单ID（可变参数）
	 * @return 成功删除的数量
	 */
	public Long removeFromZSet(String key, Object... values) {
		return stringRedisTemplate.opsForZSet().remove(key, values);
	}


}
