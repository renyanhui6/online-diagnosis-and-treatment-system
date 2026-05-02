package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.AiTriageMessage;
import cn.edu.ncu.medical.entity.AiTriageSession;
import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatResponse;
import cn.edu.ncu.medical.entity.vo.AiTriageMessageVo;
import cn.edu.ncu.medical.entity.vo.AiTriageSessionVo;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.mapper.AiTriageMessageMapper;
import cn.edu.ncu.medical.mapper.AiTriageSessionMapper;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.AiService;
import cn.edu.ncu.medical.service.AiTriageSessionService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiTriageSessionServiceImpl implements AiTriageSessionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final AiTriageSessionMapper sessionMapper;
    private final AiTriageMessageMapper messageMapper;
    private final AiService aiService;

    @Override
    public AiTriageSessionVo startSession(Long userId, Long patientAttendantId) {
        requireUser(userId);

        AiTriageSession session = new AiTriageSession();
        session.setSessionId(newSessionId());
        session.setUserId(userId);
        session.setPatientAttendantId(patientAttendantId);
        session.setStatus(STATUS_ACTIVE);
        session.setCreatedTime(new Date());
        session.setIsDeleted(0);
        sessionMapper.insert(session);
        return toSessionVo(session);
    }

    @Override
    public void closeSession(Long userId, String sessionId) {
        requireUser(userId);
        if (!StringUtils.hasText(sessionId)) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
        }

        AiTriageSession session = loadSessionForUser(userId, sessionId);
        if (session == null) {
            throw new MyRuntimeException(ResultCodeEnum.DATA_ERROR.getCode(), "AI 分诊会话不存在");
        }
        if (STATUS_CLOSED.equals(session.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<AiTriageSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiTriageSession::getSessionId, sessionId)
                .eq(AiTriageSession::getUserId, userId)
                .set(AiTriageSession::getStatus, STATUS_CLOSED)
                .set(AiTriageSession::getClosedTime, new Date())
                .set(AiTriageSession::getUpdatedTime, new Date());
        sessionMapper.update(null, wrapper);
    }

    @Override
    public TriageChatResponse sendMessage(Long userId, TriageChatRequest request) {
        requireUser(userId);
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
        }

        AiTriageSession session = resolveWritableSession(userId, request);
        request.setSessionId(session.getSessionId());

        saveUserMessage(session, request.getMessage());

        TriageChatResponse response = aiService.triageChat(request);
        if (response == null) {
            response = new TriageChatResponse();
            response.setAssistantMessage("AI 分诊暂时没有返回结果，请稍后重试或手动选择科室。");
            response.setSource("local-fallback");
        }
        response.setSessionId(session.getSessionId());
        saveAssistantMessage(session, response);
        updateSession(session, response);
        return response;
    }

    @Override
    public List<AiTriageSessionVo> listSessions(Long userId) {
        requireUser(userId);
        LambdaQueryWrapper<AiTriageSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTriageSession::getUserId, userId)
                .orderByDesc(AiTriageSession::getCreatedTime)
                .last("limit 50");
        return sessionMapper.selectList(wrapper).stream()
                .map(this::toSessionVo)
                .toList();
    }

    @Override
    public List<AiTriageMessageVo> listMessages(Long userId, String sessionId) {
        requireUser(userId);
        if (!StringUtils.hasText(sessionId)) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
        }
        AiTriageSession session = loadSessionForUser(userId, sessionId);
        if (session == null) {
            throw new MyRuntimeException(ResultCodeEnum.DATA_ERROR.getCode(), "AI 分诊会话不存在");
        }

        LambdaQueryWrapper<AiTriageMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTriageMessage::getUserId, userId)
                .eq(AiTriageMessage::getSessionId, sessionId)
                .orderByAsc(AiTriageMessage::getCreateTime)
                .orderByAsc(AiTriageMessage::getId);
        return messageMapper.selectList(wrapper).stream()
                .map(this::toMessageVo)
                .toList();
    }

    private AiTriageSession resolveWritableSession(Long userId, TriageChatRequest request) {
        String sessionId = request.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            return createSessionEntity(userId, request.getPatientAttendantId());
        }

        AiTriageSession session = loadSessionForUser(userId, sessionId);
        if (session == null) {
            throw new MyRuntimeException(ResultCodeEnum.DATA_ERROR.getCode(), "AI 分诊会话不存在");
        }
        if (!STATUS_ACTIVE.equals(session.getStatus())) {
            throw new MyRuntimeException(ResultCodeEnum.OPERATION_ERROR.getCode(), "AI 分诊会话已关闭，请重新开始");
        }
        return session;
    }

    private AiTriageSession createSessionEntity(Long userId, Long patientAttendantId) {
        AiTriageSession session = new AiTriageSession();
        session.setSessionId(newSessionId());
        session.setUserId(userId);
        session.setPatientAttendantId(patientAttendantId);
        session.setStatus(STATUS_ACTIVE);
        session.setCreatedTime(new Date());
        session.setIsDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    private AiTriageSession loadSessionForUser(Long userId, String sessionId) {
        LambdaQueryWrapper<AiTriageSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTriageSession::getUserId, userId)
                .eq(AiTriageSession::getSessionId, sessionId)
                .last("limit 1");
        return sessionMapper.selectOne(wrapper);
    }

    private void saveUserMessage(AiTriageSession session, String content) {
        AiTriageMessage message = new AiTriageMessage();
        message.setSessionId(session.getSessionId());
        message.setUserId(session.getUserId());
        message.setRole(ROLE_USER);
        message.setContent(content);
        message.setNeedMoreInfo(0);
        message.setEmergency(0);
        message.setCreateTime(new Date());
        message.setIsDeleted(0);
        messageMapper.insert(message);
    }

    private void saveAssistantMessage(AiTriageSession session, TriageChatResponse response) {
        AiTriageMessage message = new AiTriageMessage();
        message.setSessionId(session.getSessionId());
        message.setUserId(session.getUserId());
        message.setRole(ROLE_ASSISTANT);
        message.setContent(defaultText(response.getAssistantMessage()));
        message.setSource(response.getSource());
        message.setRecommendedDepartments(toJson(response.getRecommendedDepartments()));
        message.setRecommendedSubDepartments(toJson(response.getRecommendedSubDepartments()));
        message.setNeedMoreInfo(response.isNeedMoreInfo() ? 1 : 0);
        message.setEmergency(response.isEmergency() ? 1 : 0);
        message.setConfidence(response.getConfidence());
        message.setCreateTime(new Date());
        message.setIsDeleted(0);
        messageMapper.insert(message);
    }

    private void updateSession(AiTriageSession session, TriageChatResponse response) {
        LambdaUpdateWrapper<AiTriageSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiTriageSession::getSessionId, session.getSessionId())
                .eq(AiTriageSession::getUserId, session.getUserId())
                .set(AiTriageSession::getSource, response.getSource())
                .set(AiTriageSession::getRecommendedDepartments, toJson(response.getRecommendedDepartments()))
                .set(AiTriageSession::getRecommendedSubDepartments, toJson(response.getRecommendedSubDepartments()))
                .set(AiTriageSession::getSummary, buildSummary(response))
                .set(AiTriageSession::getUpdatedTime, new Date());
        sessionMapper.update(null, wrapper);
    }

    private AiTriageSessionVo toSessionVo(AiTriageSession session) {
        AiTriageSessionVo vo = new AiTriageSessionVo();
        vo.setSessionId(session.getSessionId());
        vo.setUserId(session.getUserId());
        vo.setPatientAttendantId(session.getPatientAttendantId());
        vo.setStatus(session.getStatus());
        vo.setSource(session.getSource());
        vo.setRecommendedDepartments(parseList(session.getRecommendedDepartments()));
        vo.setRecommendedSubDepartments(parseList(session.getRecommendedSubDepartments()));
        vo.setSummary(session.getSummary());
        vo.setCreatedTime(session.getCreatedTime());
        vo.setUpdatedTime(session.getUpdatedTime());
        vo.setClosedTime(session.getClosedTime());
        return vo;
    }

    private AiTriageMessageVo toMessageVo(AiTriageMessage message) {
        AiTriageMessageVo vo = new AiTriageMessageVo();
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSource(message.getSource());
        vo.setRecommendedDepartments(parseList(message.getRecommendedDepartments()));
        vo.setRecommendedSubDepartments(parseList(message.getRecommendedSubDepartments()));
        vo.setNeedMoreInfo(message.getNeedMoreInfo());
        vo.setEmergency(message.getEmergency());
        vo.setConfidence(message.getConfidence());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String newSessionId() {
        return "triage-" + UUID.randomUUID().toString().replace("-", "");
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new MyRuntimeException(ResultCodeEnum.USER_NOT_LOGIN);
        }
    }

    private String toJson(List<String> values) {
        return values == null ? "[]" : JSON.toJSONString(values);
    }

    private List<String> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONArray.parseArray(json, String.class);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String buildSummary(TriageChatResponse response) {
        String summary = StringUtils.hasText(response.getRationale())
                ? response.getRationale()
                : response.getAssistantMessage();
        if (!StringUtils.hasText(summary)) {
            return null;
        }
        return summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
