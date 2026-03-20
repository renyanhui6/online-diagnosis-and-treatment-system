package cn.edu.ncu.medical.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Log4j2
public class RedisCache {
	private final StringRedisTemplate stringRedisTemplate;
	private final boolean fallbackEnabled;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Map<String, InMemoryValue> inMemory = new ConcurrentHashMap<>();
	private final Map<String, Map<String, Double>> inMemoryZSet = new ConcurrentHashMap<>();

	public RedisCache(
			@Autowired(required = false) StringRedisTemplate stringRedisTemplate,
			@Value("${app.redis.fallback-enabled:false}") boolean fallbackEnabled
	) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.fallbackEnabled = fallbackEnabled;
	}

	private boolean canUseRedis() {
		return stringRedisTemplate != null;
	}

	private void putInMemory(String key, String value) {
		inMemory.put(key, new InMemoryValue(value, null));
	}

	private String getFromMemory(String key) {
		InMemoryValue value = inMemory.get(key);
		if (value == null) {
			return null;
		}
		if (value.expireAtMillis != null && System.currentTimeMillis() > value.expireAtMillis) {
			inMemory.remove(key);
			return null;
		}
		return value.value;
	}

	private void expireInMemory(String key, Integer expire, TimeUnit timeUnit) {
		InMemoryValue value = inMemory.get(key);
		if (value == null) {
			return;
		}
		long ttlMillis = timeUnit.toMillis(expire.longValue());
		inMemory.put(key, new InMemoryValue(value.value, System.currentTimeMillis() + ttlMillis));
	}

	public RedisCache setString(String key, String value) {
		if (!canUseRedis()) {
			if (fallbackEnabled) {
				putInMemory(key, value);
				return this;
			}
			throw new IllegalStateException("StringRedisTemplate not available");
		}
		try {
			stringRedisTemplate.opsForValue().set(key, value);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for setString, key={}", key);
			putInMemory(key, value);
		}
		return this;
	}

	public String getString(String key) {
		if (!canUseRedis()) {
			return fallbackEnabled ? getFromMemory(key) : null;
		}
		try {
			return stringRedisTemplate.opsForValue().get(key);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for getString, key={}", key);
			return getFromMemory(key);
		}
	}

	public void setExpire(String key, Integer expire, TimeUnit timeUnit) {
		if (!canUseRedis()) {
			if (fallbackEnabled) {
				expireInMemory(key, expire, timeUnit);
			}
			return;
		}
		try {
			stringRedisTemplate.expire(key, expire, timeUnit);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for expire, key={}", key);
			expireInMemory(key, expire, timeUnit);
		}
	}

	public long increment(String key, long delta, int expireSeconds) {
		if (!canUseRedis()) {
			if (fallbackEnabled) {
				return incrementInMemory(key, delta, expireSeconds);
			}
			throw new IllegalStateException("StringRedisTemplate not available");
		}
		try {
			Long value = stringRedisTemplate.opsForValue().increment(key, delta);
			if (value != null && value == delta && expireSeconds > 0) {
				stringRedisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
			}
			return value == null ? 0L : value;
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for increment, key={}", key);
			return incrementInMemory(key, delta, expireSeconds);
		}
	}

	private long incrementInMemory(String key, long delta, int expireSeconds) {
		synchronized (inMemory) {
			InMemoryValue current = inMemory.get(key);
			Long expireAt = null;
			if (current != null && current.expireAtMillis != null && System.currentTimeMillis() > current.expireAtMillis) {
				current = null;
			}
			if (current == null) {
				if (expireSeconds > 0) {
					expireAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds);
				}
				inMemory.put(key, new InMemoryValue(String.valueOf(delta), expireAt));
				return delta;
			}
			long currentValue = 0L;
			try {
				currentValue = Long.parseLong(current.value);
			} catch (NumberFormatException ignored) {
				currentValue = 0L;
			}
			long nextValue = currentValue + delta;
			expireAt = current.expireAtMillis;
			if (expireAt == null && expireSeconds > 0) {
				expireAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds);
			}
			inMemory.put(key, new InMemoryValue(String.valueOf(nextValue), expireAt));
			return nextValue;
		}
	}

	public RedisCache setObject(String key, Object object) throws Exception {
		String value = objectMapper.writeValueAsString(object);
		setString(key, value);
		return this;
	}

	public Object getObject(String key, Class<?> clazz) throws Exception {
		String jsonObject = getString(key);
		if (jsonObject == null) {
			return null;
		}
		return objectMapper.readValue(jsonObject, clazz);
	}

	public void delete(String key) {
		if (!canUseRedis()) {
			if (fallbackEnabled) {
				inMemory.remove(key);
				inMemoryZSet.remove(key);
			}
			return;
		}
		try {
			if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
				return;
			}
			stringRedisTemplate.delete(key);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for delete, key={}", key);
			inMemory.remove(key);
			inMemoryZSet.remove(key);
		}
	}



	// -------------------------- 新增ZSet操作（支持延迟队列） --------------------------

	/**
	 * 向ZSet中添加元素（用于延迟队列：订单ID+到期时间戳）
	 * @param key ZSet的键（如"order:delay:register"）
	 * @param value 订单ID（字符串形式）
	 * @param score 到期时间戳（毫秒）
	 */
	public void addToZSet(String key, String value, double score) {
		if (!canUseRedis()) {
			if (fallbackEnabled) {
				inMemoryZSet.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(value, score);
			}
			return;
		}
		try {
			stringRedisTemplate.opsForZSet().add(key, value, score);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for zadd, key={}", key);
			inMemoryZSet.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(value, score);
		}
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
		if (!canUseRedis()) {
			if (!fallbackEnabled) {
				return Set.of();
			}
			Map<String, Double> values = inMemoryZSet.get(key);
			if (values == null || values.isEmpty()) {
				return Set.of();
			}
			return values.entrySet().stream()
					.filter(e -> e.getValue() >= minScore && e.getValue() <= maxScore)
					.sorted(Map.Entry.comparingByValue())
					.limit(limit)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
		}
		try {
			return stringRedisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore, 0, limit);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for zrangeByScore, key={}", key);
			Map<String, Double> values = inMemoryZSet.get(key);
			if (values == null || values.isEmpty()) {
				return Set.of();
			}
			return values.entrySet().stream()
					.filter(e -> e.getValue() >= minScore && e.getValue() <= maxScore)
					.sorted(Map.Entry.comparingByValue())
					.limit(limit)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
		}
	}

	/**
	 * 从ZSet中删除元素（用于订单支付后移除）
	 * @param key ZSet的键
	 * @param values 要删除的订单ID（可变参数）
	 * @return 成功删除的数量
	 */
	public Long removeFromZSet(String key, Object... values) {
		if (!canUseRedis()) {
			if (!fallbackEnabled) {
				return 0L;
			}
			Map<String, Double> stored = inMemoryZSet.get(key);
			if (stored == null || stored.isEmpty()) {
				return 0L;
			}
			long removed = 0L;
			for (Object v : values) {
				if (v != null && stored.remove(String.valueOf(v)) != null) {
					removed++;
				}
			}
			return removed;
		}
		try {
			return stringRedisTemplate.opsForZSet().remove(key, values);
		} catch (RuntimeException ex) {
			if (!fallbackEnabled) {
				throw ex;
			}
			log.warn("Redis unavailable, fallback to in-memory for zrem, key={}", key);
			Map<String, Double> stored = inMemoryZSet.get(key);
			if (stored == null || stored.isEmpty()) {
				return 0L;
			}
			long removed = 0L;
			for (Object v : values) {
				if (v != null && stored.remove(String.valueOf(v)) != null) {
					removed++;
				}
			}
			return removed;
		}
	}

	private static class InMemoryValue {
		private final String value;
		private final Long expireAtMillis;

		private InMemoryValue(String value, Long expireAtMillis) {
			this.value = value;
			this.expireAtMillis = expireAtMillis;
		}
	}

}
