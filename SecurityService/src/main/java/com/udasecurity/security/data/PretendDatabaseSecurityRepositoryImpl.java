package com.udasecurity.security.data;

import dev.mccue.guava.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

/**
 * Fake repository implementation for demo purposes. Stores state information in local
 * memory and writes it to user preferences between app loads. This implementation is
 * intentionally a little hard to use in unit tests, so watch out!
 */
public class PretendDatabaseSecurityRepositoryImpl implements SecurityRepository{

    private Set<Sensor> sensors;
    private com.udasecurity.security.data.AlarmStatus alarmStatus;
    private com.udasecurity.security.data.ArmingStatus armingStatus;

    //preference keys
    private static final String SENSORS = "SENSORS";
    private static final String ALARM_STATUS = "ALARM_STATUS";
    private static final String ARMING_STATUS = "ARMING_STATUS";

    private static final Preferences prefs = Preferences.userNodeForPackage(PretendDatabaseSecurityRepositoryImpl.class);
    private static final Gson gson = new Gson();

    public PretendDatabaseSecurityRepositoryImpl() {
        try {
            alarmStatus = com.udasecurity.security.data.AlarmStatus.valueOf(prefs.get(ALARM_STATUS, com.udasecurity.security.data.AlarmStatus.NO_ALARM.toString()));
            armingStatus = com.udasecurity.security.data.ArmingStatus.valueOf(prefs.get(ARMING_STATUS, com.udasecurity.security.data.ArmingStatus.DISARMED.toString()));

            String sensorString = prefs.get(SENSORS, null);
            if(sensorString == null) {
                sensors = new TreeSet<>();
            } else {
                Type type = new TypeToken<Set<Sensor>>() {
                }.getType();
                sensors = gson.fromJson(sensorString, type);
            }
        } catch (Exception e) {
            sensors = new TreeSet<>();
            alarmStatus = AlarmStatus.NO_ALARM;
            armingStatus = ArmingStatus.DISARMED;

            throw new RuntimeException("Failed to initialize security repository", e);
        }

    }

    @Override
    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
        prefs.put(SENSORS, gson.toJson(sensors));
    }

    @Override
    public void removeSensor(Sensor sensor) {
        sensors.remove(sensor);
        prefs.put(SENSORS, gson.toJson(sensors));
    }

    @Override
    public void updateSensor(Sensor sensor) {
        sensors.remove(sensor);
        sensors.add(sensor);
        prefs.put(SENSORS, gson.toJson(sensors));
    }


    @Override
    public void setAlarmStatus(com.udasecurity.security.data.AlarmStatus alarmStatus) {
        this.alarmStatus = alarmStatus;
        prefs.put(ALARM_STATUS, this.alarmStatus.toString());
    }

    @Override
    public void setArmingStatus(com.udasecurity.security.data.ArmingStatus armingStatus) {
        this.armingStatus = armingStatus;
        prefs.put(ARMING_STATUS, this.armingStatus.toString());
    }

    @Override
    public Set<Sensor> getSensors() {
        return new TreeSet<>(sensors);

    }

    @Override
    public com.udasecurity.security.data.AlarmStatus getAlarmStatus() {
        return alarmStatus;
    }

    @Override
    public com.udasecurity.security.data.ArmingStatus getArmingStatus() {
        return armingStatus;
    }
}
