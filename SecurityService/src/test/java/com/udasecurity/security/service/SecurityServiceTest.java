package com.udasecurity.security.service;

import com.udasecurity.image.service.ImageService;
import com.udasecurity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private SecurityService securityService;


    private Sensor testSensor;

    @BeforeEach
    void setUp(){
        testSensor = new Sensor("Test Sensor", SensorType.DOOR);
    }

    // Requirement 1: Alarm armed + sensor activated → pending
    @Test
    void alarmArmed_sensorActivated_setPendingAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(testSensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    // Requirement 2: Armed + sensor activated (already pending) → alarm
    @Test
    void alarmArmed_sensorActivatedWhenPending_setAlarm() {
        Sensor sensor = new Sensor("Test Sensor", SensorType.DOOR);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    // Requirement 3: Pending + all sensors inactive → no alarm
    @Test
    void pendingAlarm_allSensorsInactive_setNoAlarm() {
        Sensor sensor = testSensor;
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);

        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    // Requirement 4: Alarm active → sensor changes don't affect state
    @Test
    void alarmActive_changeSensor_noStateChange() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(testSensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Requirement 5: Activate active sensor while pending → alarm
    @Test
    void activateActiveSensor_pendingState_setAlarm() {
        Sensor sensor = new Sensor("Test Sensor", SensorType.DOOR);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Requirement 6: Deactivate inactive sensor → no change
    @Test
    void deactivateInactiveSensor_noStateChange() {
        testSensor.setActive(false);
        securityService.changeSensorActivationStatus(testSensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Requirement 7: Cat detected + armed home → alarm
    @Test
    void catDetected_armedHome_setAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        BufferedImage realImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(realImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    // Requirement 8: No cat + sensors inactive → no alarm
    @Test
    void noCatDetected_allSensorsInactive_setNoAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(testSensor));

        BufferedImage realImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(realImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Requirement 9: System disarmed → no alarm
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "PENDING_ALARM"})
    void systemDisarmed_always_setNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Requirement 10: System armed → reset sensors
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_resetSensorsToInactive(ArmingStatus status) {
        Sensor sensor = testSensor;
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);

        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);

        assertFalse(sensor.getActive());
        verify(securityRepository).updateSensor(sensor);
    }

    // Requirement 11: Armed home with cat → alarm
    @Test
    void armedHomeWithCat_setAlarm() {
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.DISARMED)
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        BufferedImage realImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(realImage);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

}