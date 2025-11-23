package cn.edu.ncu.medical.websocket;

import cn.edu.ncu.medical.config.UploadConfig;
import cn.edu.ncu.medical.entity.ChatMessage;
import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.service.PatientAttendantService;
import cn.edu.ncu.medical.service.RoomService;
import cn.edu.ncu.medical.utils.UploadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {


    private static ChatMessageService chatMessageService;

    private static UploadConfig uploadConfig;
    private static RoomService roomService;
    private static PatientAttendantService patientAttendantService;
    @Autowired
    public ChatController(ChatMessageService chatMessageService, UploadConfig uploadConfig, RoomService roomService, PatientAttendantService patientAttendantService) {
        ChatController.chatMessageService = chatMessageService;
        ChatController.uploadConfig = uploadConfig;
        ChatController.roomService = roomService;
        ChatController.patientAttendantService = patientAttendantService;
    }
    /**
     * 医生发起问诊
     */
    @PostMapping("/initiate-consultation")
    public Result<Map<String, Object>> initiateConsultation(@RequestBody Map<String, Object> request) {
        try {
            Long registrationId = Long.valueOf(request.get("registrationId").toString());
            Long doctorId = Long.valueOf(request.get("doctorId").toString());
            Long patientId = Long.valueOf(request.get("patientId").toString());
            String patientName = (String) request.get("patientName");
            LambdaQueryWrapper<PatientAttendant> patientAttendantLambdaQueryWrapper = new LambdaQueryWrapper<>();
            patientAttendantLambdaQueryWrapper.eq(PatientAttendant::getId, patientId);
            List<PatientAttendant> list = patientAttendantService.list(patientAttendantLambdaQueryWrapper);
            if (list.size() == 0) {
                return Result.fail(404, "患者不存在");
            }
            PatientAttendant patientAttendant = list.get(0);
            patientId=patientAttendant.getSystemUserId();
            System.out.println("🏥 医生发起问诊，预约ID: " + registrationId + ", 医生ID: " + doctorId + ", 患者ID: " + patientId);

            // 先检查是否已存在房间
            Room existingRoom = roomService.getRoomByRegistrationId(registrationId);
            Room room;
            
            if (existingRoom != null) {
                // 如果房间已存在，删除现有房间
                System.out.println("✅ 房间已存在，删除现有房间ID: " + existingRoom.getId());
                roomService.removeById(existingRoom.getId());
            }

            // 创建新房间
                room = new Room();
                room.setRegistrationId(registrationId);
                room.setDoctorId(doctorId);
                room.setPatientId(patientId);
                room.setPatientName(patientName);
                room.setRoomStatus(1); // 1-等待患者确认
                room.setCreateTime(new Date());
                room.setUpdateTime(new Date());
                roomService.save(room);
                System.out.println("✅ 创建新房间，房间ID: " + room.getId());


            // 发送问诊请求通知给患者（通过WebSocket）
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "consultation_request");
            notification.put("registrationId", registrationId);
            notification.put("roomId", room.getId());
            notification.put("doctorId", doctorId);
            notification.put("patientId", patientId);
            notification.put("patientName", patientName);
            notification.put("timestamp", new Date());

            ChatWebSocket.sendToPatientLongConnection(patientId, notification);

            // 启动患者响应超时定时器
            ChatWebSocket.schedulePatientResponseTimeout(registrationId, room.getId());

            // 返回房间ID和预约ID
            Map<String, Object> result = new HashMap<>();
            result.put("roomId", room.getId());
            result.put("registrationId", registrationId);

            System.out.println("✅ 问诊发起成功，房间ID: " + room.getId());
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "发起问诊失败: " + e.getMessage());
        }
    }

    /**
     * 患者响应问诊请求
     */
    @PostMapping("/respond-consultation")
    public Result<String> respondToConsultation(@RequestBody Map<String, Object> request) {
        try {
            Long registrationId = Long.valueOf(request.get("registrationId").toString());
            String response = (String) request.get("response"); // "accept" 或 "reject"

            return respondToConsultationInternal(registrationId, response);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "响应问诊失败: " + e.getMessage());
        }
    }

    /**
     * 内部响应问诊方法（供WebSocket调用）
     */
    public static Result<String> respondToConsultationInternal(Long registrationId, String response) {
        try {
            System.out.println("📝 患者响应问诊，预约ID: " + registrationId + ", 响应: " + response);

            // 通过预约ID查询房间
            LambdaQueryWrapper<Room> roomLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomLambdaQueryWrapper.eq(Room::getRegistrationId, registrationId);
            Room room = roomService.getOne(roomLambdaQueryWrapper);
            if (room == null) {
                return Result.fail(404, "房间不存在");
            }

            // 更新房间状态
            if ("accept".equals(response)) {
                room.setRoomStatus(2); // 2-问诊中
            } else {
                room.setRoomStatus(5); // 5-患者拒绝
            }
            room.setUpdateTime(new Date());
            roomService.updateById(room);

            // 发送响应通知给医生（通过WebSocket）
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "consultation_response");
            notification.put("response", response);
            notification.put("roomId", room.getId());
            notification.put("registrationId", registrationId);
            notification.put("timestamp", new Date());

            ChatWebSocket.sendToDoctorLongConnection(room.getDoctorId(), notification);

            System.out.println("✅ 患者响应成功，房间状态: " + room.getRoomStatus());
            return Result.ok("响应成功");

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "响应问诊失败: " + e.getMessage());
        }
    }

    /**
     * 获取房间信息
     */
    @GetMapping("/room/{registrationId}")
    public Result<Room> getRoomStatus(@PathVariable Long registrationId) {
        try {
            LambdaQueryWrapper<Room> roomLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomLambdaQueryWrapper.eq(Room::getRegistrationId, registrationId);
            Room room = roomService.getOne(roomLambdaQueryWrapper);
            if (room == null) {
                return Result.fail(404, "房间不存在");
            }
            return Result.ok(room);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取房间信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取聊天消息
     */
    @GetMapping("/messages/{roomId}")
    public Result<List<ChatMessage>> getChatMessages(@PathVariable Long roomId) {
        try {
            System.out.println("📝 获取聊天消息，房间ID: " + roomId);

            // 直接通过房间ID查询消息
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getRoomId, roomId);
            wrapper.eq(ChatMessage::getIsDeleted, 0);
            wrapper.orderByAsc(ChatMessage::getCreateTime);

            List<ChatMessage> messages = chatMessageService.list(wrapper);

            System.out.println("✅ 获取到 " + messages.size() + " 条聊天消息");
            return Result.ok(messages);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取聊天消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    @PostMapping("/send-message")
    public Result<String> sendMessage(@RequestBody ChatMessage message) {
        try {
            message.setCreateTime(new Date());
            chatMessageService.save(message);

            // 广播消息到聊天房间
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "chat");
            wsMessage.put("roomId", message.getRoomId());
            wsMessage.put("senderType", message.getSenderType());
            wsMessage.put("senderId", message.getSenderId());
            wsMessage.put("messageType", message.getMessageType());
            wsMessage.put("content", message.getContent());
            wsMessage.put("createTime", message.getCreateTime());

            ChatWebSocket.broadcastToChatRoom(message.getRoomId().toString(), wsMessage);

            return Result.ok("消息发送成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "发送消息失败: " + e.getMessage());
        }
    }
    @GetMapping("/room-by-id/{roomId}")
    public Result<Room> getRoomById(@PathVariable Long roomId) {
        try {
            Room room = roomService.getById(roomId);
            if (room == null) {
                return Result.fail(404, "房间不存在");
            }
            return Result.ok(room);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取房间信息失败: " + e.getMessage());
        }
    }
    /**
     * 重新接诊
     */
    @PostMapping("/resume-consultation")
    public Result<String> resumeConsultation(@RequestBody Map<String, Object> request) {
        try {
            Long registrationId = Long.valueOf(request.get("registrationId").toString());
            LambdaQueryWrapper<Room> roomLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomLambdaQueryWrapper.eq(Room::getRegistrationId, registrationId);
            Room room = roomService.getOne(roomLambdaQueryWrapper);
            if (room == null) {
                return Result.fail(404, "房间不存在");
            }

            room.setRoomStatus(1);
            room.setUpdateTime(new Date());
            roomService.updateById(room);

            // 发送重新接诊通知给患者（格式与initiateConsultation一致）
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "consultation_request");
            notification.put("registrationId", registrationId);
            notification.put("roomId", room.getId());
            notification.put("doctorId", room.getDoctorId());
            notification.put("patientId", room.getPatientId());
            notification.put("patientName", room.getPatientName());
            notification.put("timestamp", new Date());
            // 添加额外的字段，确保患者端能正确显示通知
            notification.put("doctorName", "医生"); // 可以从医生信息中获取
            notification.put("departmentName", "科室"); // 可以从医生信息中获取
            notification.put("doctorTitle", "主治医师"); // 可以从医生信息中获取
            notification.put("consultationType", "图文问诊");
            notification.put("message", "医生请求重新开始问诊，请确认是否同意。");

            ChatWebSocket.sendToPatientLongConnection(room.getPatientId(), notification);

            ChatWebSocket.schedulePatientResponseTimeout(registrationId, room.getId());

            return Result.ok("重新接诊成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "重新接诊失败: " + e.getMessage());
        }
    }

    /**
     * 结束问诊
     */
    @PostMapping("/room/{roomId}/end")
    public Result<String> endConsultation(@PathVariable Long roomId) {
        try {
            System.out.println("🏥 医生结束问诊，房间ID: " + roomId);
            
            // 查询房间信息
            Room room = roomService.getById(roomId);
            if (room == null) {
                return Result.fail(404, "房间不存在");
            }
            
            // 更新房间状态为已结束
            room.setRoomStatus(3); // 3-已结束
            room.setUpdateTime(new Date());
            roomService.updateById(room);
            
            // 发送结束问诊消息给患者
            Map<String, Object> endMessage = new HashMap<>();
            endMessage.put("type", "consultation_end");
            endMessage.put("roomId", roomId);
            endMessage.put("registrationId", room.getRegistrationId());
            endMessage.put("timestamp", new Date());
            endMessage.put("message", "问诊已结束，感谢您的配合");
            
            // 广播给聊天房间的所有用户
            ChatWebSocket.broadcastToChatRoom(roomId.toString(), endMessage);
            
            // 发送状态更新消息
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("type", "status");
            statusMessage.put("roomStatus", 3);
            statusMessage.put("message", "问诊已结束");
            statusMessage.put("timestamp", new Date());
            
            ChatWebSocket.broadcastToChatRoom(roomId.toString(), statusMessage);
            
            // 发送房间状态更新消息
            Map<String, Object> roomStatusMessage = new HashMap<>();
            roomStatusMessage.put("type", "room_status_update");
            roomStatusMessage.put("room_status", 3);
            roomStatusMessage.put("roomId", roomId);
            roomStatusMessage.put("timestamp", new Date());
            
            ChatWebSocket.broadcastToChatRoom(roomId.toString(), roomStatusMessage);
            
            // 通知患者端断开连接
            if (room.getPatientId() != null) {
                Map<String, Object> disconnectMessage = new HashMap<>();
                disconnectMessage.put("type", "disconnect");
                disconnectMessage.put("roomId", roomId);
                disconnectMessage.put("reason", "consultation_ended");
                disconnectMessage.put("timestamp", new Date());
                
                ChatWebSocket.sendToPatientLongConnection(room.getPatientId(), disconnectMessage);
            }
            
            System.out.println("✅ 问诊结束成功，房间ID: " + roomId);
            return Result.ok("问诊结束成功");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "结束问诊失败: " + e.getMessage());
        }
    }

    /**
     * 上传聊天图片
     */
    @PostMapping("/upload-image")
    public Result<Map<String, Object>> uploadChatImage(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("room_id") Long roomId,
                                                       @RequestParam("sender_id") Long senderId) {
        try {
            if (file.isEmpty()) {
                return Result.fail(400, "文件为空");
            }

            // 验证房间是否存在
            Room room = roomService.getById(roomId);
            if (room == null) {
                return Result.fail(404, "房间不存在");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = "chat_" + roomId + "_" + UUID.randomUUID().toString() + fileExtension;

            // 获取上传token
            String token = UploadUtil.uploadToken(uploadConfig);

            // 上传到七牛云
            String imageUrl = UploadUtil.putPhoto(file.getInputStream(), fileName, token);

            // 保存消息记录
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRoomId(roomId);
            chatMessage.setSenderId(senderId);

            // 根据senderId判断发送者类型
            if (senderId.equals(room.getPatientId())) {
                chatMessage.setSenderType(1); // 患者
            } else if (senderId.equals(room.getDoctorId())) {
                chatMessage.setSenderType(2); // 医生
            } else {
                return Result.fail(400, "发送者ID无效");
            }

            chatMessage.setMessageType(2); // 图片消息
            chatMessage.setContent(imageUrl);
            chatMessage.setCreateTime(new Date());

            chatMessageService.save(chatMessage);

            // 通过WebSocket广播消息
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "chat");
            wsMessage.put("roomId", roomId);
            wsMessage.put("senderType", chatMessage.getSenderType());
            wsMessage.put("senderId", senderId);
            wsMessage.put("messageType", 2); // 图片消息
            wsMessage.put("content", imageUrl);
            wsMessage.put("createTime", chatMessage.getCreateTime());

            ChatWebSocket.broadcastToChatRoom(roomId.toString(), wsMessage);

            // 返回图片URL
            Map<String, Object> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("messageId", chatMessage.getId());

            System.out.println("✅ 图片上传成功: " + imageUrl);
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "图片上传失败: " + e.getMessage());
        }
    }
}