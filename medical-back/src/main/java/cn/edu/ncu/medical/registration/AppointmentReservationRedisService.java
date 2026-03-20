package cn.edu.ncu.medical.registration;

import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.exception.AppointmentException;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.utils.ScheduleTimePolicy;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AppointmentReservationRedisService {
    private static final DefaultRedisScript<String> RESERVE_SCRIPT = script("""
            local stockKey = KEYS[1]
            local onceKey = KEYS[2]
            local resvKey = KEYS[3]
            local zexpKey = KEYS[4]

            local now = ARGV[1]
            local ttl = tonumber(ARGV[2])
            local userId = ARGV[3]
            local patientId = ARGV[4]
            local personKey = ARGV[5]
            local token = ARGV[6]
            local expireAt = tonumber(ARGV[7])

            local reserved = redis.call('SET', onceKey, token, 'NX', 'PX', ttl)
            if not reserved then
                local existing = redis.call('GET', onceKey)
                if not existing then
                    return 'DUP|'
                end
                return 'DUP|' .. existing
            end

            local left = tonumber(redis.call('DECR', stockKey))
            if left < 0 then
                redis.call('INCR', stockKey)
                redis.call('DEL', onceKey)
                return 'NO_STOCK'
            end

            redis.call('HSET', resvKey,
                'u', userId,
                'pid', patientId,
                'pk', personKey,
                'st', 'P',
                'ts', now,
                'rid', '',
                'msg', '')
            redis.call('PEXPIRE', resvKey, ttl)
            redis.call('ZADD', zexpKey, expireAt, token)
            redis.call('PEXPIRE', zexpKey, ttl + 60000)

            return 'OK|' .. token .. '|' .. left
            """);

    private static final DefaultRedisScript<String> CONFIRM_SCRIPT = script("""
            local onceKey = KEYS[1]
            local resvKey = KEYS[2]
            local zexpKey = KEYS[3]

            local token = ARGV[1]
            local registrationId = ARGV[2]
            local keepMs = tonumber(ARGV[3])

            local st = redis.call('HGET', resvKey, 'st')
            if not st then
                return 'NO_RESV'
            end
            if st == 'C' then
                return 'ALREADY_CONFIRMED'
            end
            if st ~= 'P' then
                return 'NOT_PENDING|' .. st
            end

            redis.call('HSET', resvKey, 'st', 'C', 'rid', registrationId, 'msg', '')
            if keepMs and keepMs > 0 then
                redis.call('PEXPIRE', resvKey, keepMs)
            end
            redis.call('ZREM', zexpKey, token)
            if redis.call('GET', onceKey) == token then
                if keepMs and keepMs > 0 then
                    redis.call('PEXPIRE', onceKey, keepMs)
                end
            end
            return 'OK'
            """);

    private static final DefaultRedisScript<String> ROLLBACK_SCRIPT = script("""
            local stockKey = KEYS[1]
            local onceKey = KEYS[2]
            local resvKey = KEYS[3]
            local zexpKey = KEYS[4]

            local token = ARGV[1]
            local reason = ARGV[2]
            local failKeepMs = tonumber(ARGV[3])

            local st = redis.call('HGET', resvKey, 'st')
            if not st then
                redis.call('ZREM', zexpKey, token)
                return 'NO_RESV'
            end
            if st ~= 'P' then
                redis.call('ZREM', zexpKey, token)
                return 'NOT_PENDING|' .. st
            end

            redis.call('HSET', resvKey, 'st', 'R', 'msg', reason, 'rid', '')
            if failKeepMs and failKeepMs > 0 then
                redis.call('PEXPIRE', resvKey, failKeepMs)
            end
            redis.call('INCR', stockKey)
            if redis.call('GET', onceKey) == token then
                redis.call('DEL', onceKey)
            end
            redis.call('ZREM', zexpKey, token)
            return 'OK'
            """);

    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock = Clock.systemDefaultZone();

    public AppointmentReservationRedisService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void initializeStockIfAbsent(Schedule schedule) {
        try {
            String stockKey = AppointmentReservationKeys.stockKey(schedule.getId());
            long ttlSeconds = calculateSuccessKeepSeconds(schedule);
            int stock = Math.max(0, (schedule.getAppointmentLimit() == null ? 0 : schedule.getAppointmentLimit())
                    - (schedule.getCurrentAppointmentCount() == null ? 0 : schedule.getCurrentAppointmentCount()));
            Boolean initialized = stringRedisTemplate.opsForValue()
                    .setIfAbsent(stockKey, String.valueOf(stock), ttlSeconds, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(initialized) && ttlSeconds > 0) {
                stringRedisTemplate.expire(stockKey, ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (RuntimeException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预占库存服务不可用");
        }
    }

    public ReserveResult reserve(Long scheduleId, String personKey, Long userId, Long patientId,
                                 String token, long processingTtlMillis, long expireAtMillis) {
        String stockKey = AppointmentReservationKeys.stockKey(scheduleId);
        String onceKey = AppointmentReservationKeys.onceKey(scheduleId, personKey);
        String resvKey = AppointmentReservationKeys.reservationKey(scheduleId, token);
        String zexpKey = AppointmentReservationKeys.expireKey(scheduleId);

        String response;
        try {
            response = stringRedisTemplate.execute(
                    RESERVE_SCRIPT,
                    List.of(stockKey, onceKey, resvKey, zexpKey),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(processingTtlMillis),
                    String.valueOf(userId),
                    String.valueOf(patientId),
                    personKey,
                    token,
                    String.valueOf(expireAtMillis)
            );
        } catch (RuntimeException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预占库存服务不可用");
        }

        if (response == null || response.isBlank()) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预占库存服务返回空结果");
        }
        if (Objects.equals(response, "NO_STOCK")) {
            return ReserveResult.soldOut();
        }
        String[] parts = response.split("\\|", 3);
        if (parts.length == 0) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预占库存结果不可解析");
        }
        if (Objects.equals(parts[0], "DUP")) {
            String existingToken = parts.length > 1 ? parts[1] : null;
            return ReserveResult.duplicate(existingToken);
        }
        if (Objects.equals(parts[0], "OK") && parts.length >= 3) {
            long leftStock = Long.parseLong(parts[2]);
            return ReserveResult.success(parts[1], leftStock);
        }
        throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预占库存结果不可解析");
    }

    public boolean confirm(Long scheduleId, String personKey, String token, Long registrationId, long keepAliveMillis) {
        String response = executeScript(
                CONFIRM_SCRIPT,
                List.of(
                        AppointmentReservationKeys.onceKey(scheduleId, personKey),
                        AppointmentReservationKeys.reservationKey(scheduleId, token),
                        AppointmentReservationKeys.expireKey(scheduleId)
                ),
                token,
                String.valueOf(registrationId),
                String.valueOf(keepAliveMillis)
        );
        return Objects.equals(response, "OK") || Objects.equals(response, "ALREADY_CONFIRMED");
    }

    public boolean rollback(Long scheduleId, String personKey, String token, String reason, long failedKeepMillis) {
        String response = executeScript(
                ROLLBACK_SCRIPT,
                List.of(
                        AppointmentReservationKeys.stockKey(scheduleId),
                        AppointmentReservationKeys.onceKey(scheduleId, personKey),
                        AppointmentReservationKeys.reservationKey(scheduleId, token),
                        AppointmentReservationKeys.expireKey(scheduleId)
                ),
                token,
                reason == null ? "" : reason,
                String.valueOf(failedKeepMillis)
        );
        return Objects.equals(response, "OK") || Objects.equals(response, "NO_RESV");
    }

    public AppointmentReservationRecord getReservation(String token) {
        Long scheduleId = AppointmentReservationKeys.parseScheduleId(token);
        return getReservation(scheduleId, token);
    }

    public AppointmentReservationRecord getReservation(Long scheduleId, String token) {
        try {
            Map<Object, Object> values = stringRedisTemplate.opsForHash()
                    .entries(AppointmentReservationKeys.reservationKey(scheduleId, token));
            if (values == null || values.isEmpty()) {
                return null;
            }
            AppointmentReservationRecord record = new AppointmentReservationRecord();
            record.setUserId(parseLong(values.get("u")));
            record.setPatientId(parseLong(values.get("pid")));
            record.setPersonKey(valueToString(values.get("pk")));
            record.setState(AppointmentReservationState.fromCode(valueToString(values.get("st"))));
            record.setCreatedAt(parseLong(values.get("ts")));
            record.setRegistrationId(parseLong(values.get("rid")));
            record.setMessage(valueToString(values.get("msg")));
            return record;
        } catch (DataAccessException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预约记录不可用");
        }
    }

    public Set<String> listExpiredTokens(Long scheduleId, long nowMillis, long limit) {
        try {
            Set<String> result = stringRedisTemplate.opsForZSet()
                    .rangeByScore(AppointmentReservationKeys.expireKey(scheduleId), 0, nowMillis, 0, limit);
            return result == null ? Set.of() : result;
        } catch (RuntimeException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 超时回收索引不可用");
        }
    }

    public void removeExpireToken(Long scheduleId, String token) {
        try {
            stringRedisTemplate.opsForZSet().remove(AppointmentReservationKeys.expireKey(scheduleId), token);
        } catch (RuntimeException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 超时回收索引不可用");
        }
    }

    public String getExistingToken(Long scheduleId, String personKey) {
        try {
            return stringRedisTemplate.opsForValue().get(AppointmentReservationKeys.onceKey(scheduleId, personKey));
        } catch (RuntimeException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 幂等闸门不可用");
        }
    }

    public long calculateProcessingTtlMillis(Duration ttl) {
        return ttl.toMillis();
    }

    public long calculateSuccessKeepMillis(Schedule schedule) {
        return TimeUnit.SECONDS.toMillis(calculateSuccessKeepSeconds(schedule));
    }

    public long calculateSuccessKeepSeconds(Schedule schedule) {
        ZoneId zoneId = clock.getZone();
        LocalDate date = ScheduleTimePolicy.toLocalDate(schedule.getScheduleDate(), zoneId);
        ScheduleTimePolicy.Session session = ScheduleTimePolicy.resolveSession(schedule);
        LocalDateTime end = ScheduleTimePolicy.sessionEnd(date, session).plusDays(1);
        Instant endInstant = end.atZone(zoneId).toInstant();
        long seconds = Duration.between(Instant.now(clock), endInstant).getSeconds();
        return Math.max(seconds, TimeUnit.HOURS.toSeconds(1));
    }

    private static DefaultRedisScript<String> script(String content) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(content);
        script.setResultType(String.class);
        return script;
    }

    private String executeScript(DefaultRedisScript<String> script, List<String> keys, String... args) {
        try {
            String response = stringRedisTemplate.execute(script, keys, (Object[]) args);
            return response == null ? "" : response;
        } catch (RuntimeException ex) {
            throw new AppointmentException(ResultCodeEnum.SERVICE_ERROR.getCode(), "Redis 预约状态服务不可用");
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }
        return Long.parseLong(text);
    }

    private String valueToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record ReserveResult(boolean success, boolean duplicate, boolean noStock, String token, long leftStock) {
        public static ReserveResult success(String token, long leftStock) {
            return new ReserveResult(true, false, false, token, leftStock);
        }

        public static ReserveResult duplicate(String token) {
            return new ReserveResult(false, true, false, token, 0L);
        }

        public static ReserveResult soldOut() {
            return new ReserveResult(false, false, true, null, 0L);
        }
    }
}
