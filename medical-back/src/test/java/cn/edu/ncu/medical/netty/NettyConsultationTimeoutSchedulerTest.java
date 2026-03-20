package cn.edu.ncu.medical.netty;

import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.service.RoomService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class NettyConsultationTimeoutSchedulerTest {

    @Test
    void scheduleTimeoutUpdatesRoomStatus() throws Exception {
        RoomService roomService = Mockito.mock(RoomService.class);
        Room room = new Room();
        room.setId(10L);
        room.setDoctorId(20L);
        room.setRegistrationId(30L);
        room.setRoomStatus(1);

        Mockito.when(roomService.getById(10L)).thenReturn(room);
        Mockito.when(roomService.getRoomByRegistrationId(30L)).thenReturn(room);
        Mockito.when(roomService.updateById(Mockito.any())).thenReturn(true);

        NettyServerProperties properties = new NettyServerProperties();
        properties.setPatientResponseTimeoutMinutes(0);
        NettyConsultationTimeoutScheduler scheduler = new NettyConsultationTimeoutScheduler(roomService, properties);

        scheduler.schedulePatientResponseTimeout(30L, 10L);
        Thread.sleep(150);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        Mockito.verify(roomService, Mockito.atLeastOnce()).updateById(captor.capture());
        Room updated = captor.getValue();
        assertNotNull(updated);
        assertEquals(4, updated.getRoomStatus());

        scheduler.destroy();
    }
}
