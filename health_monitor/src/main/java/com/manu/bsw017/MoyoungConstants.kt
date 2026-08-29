package com.manu.bsw017



import java.util.UUID

/**
 * Constants and definitions for the MoYoung/DaFit protocol used by Fire-Boltt BSW017.
 * Adapted from Gadgetbridge MoyoungConstants.
 */
object MoyoungConstants {
    private const val BASE_UUID_FORMAT = "0000%s-0000-1000-8000-00805f9b34fb"

    private fun from16Bit(value: String): UUID =
        UUID.fromString(String.format(BASE_UUID_FORMAT, value.lowercase()))

    // Services
    val UUID_SERVICE_MOYOUNG = from16Bit("feea")
    val UUID_SERVICE_BATTERY = from16Bit("180f")
    val UUID_SERVICE_HEART_RATE = from16Bit("180d")
    val UUID_SERVICE_DEVICE_INFO = from16Bit("180a")

    // Characteristics
    val UUID_CHARACTERISTIC_STEPS = from16Bit("fee1")
    val UUID_CHARACTERISTIC_DATA_OUT = from16Bit("fee2")
    val UUID_CHARACTERISTIC_DATA_IN = from16Bit("fee3")
    val UUID_CHARACTERISTIC_DATA_SPECIAL_1 = from16Bit("fee5")
    val UUID_CHARACTERISTIC_DATA_SPECIAL_2 = from16Bit("fee6")
    val UUID_CHARACTERISTIC_DATA_ECG_OLD = from16Bit("fee7")
    val UUID_CHARACTERISTIC_DATA_ECG_NEW = from16Bit("fee8")
    
    val UUID_CHARACTERISTIC_BATTERY_LEVEL = from16Bit("2a19")
    val UUID_CHARACTERISTIC_HR_MEASUREMENT = from16Bit("2a37")
    val UUID_CHARACTERISTIC_BODY_SENSOR_LOCATION = from16Bit("2a38")
    
    val UUID_DESCRIPTOR_CCCD = from16Bit("2902")

    // Special Commands
    const val CMD_SHUTDOWN: Byte = 81
    const val CMD_FIND_MY_WATCH: Byte = 97
    const val CMD_FIND_MY_PHONE: Byte = 98
    const val CMD_HS_DFU: Byte = 99

    // Activity/Training Commands
    const val CMD_QUERY_LAST_DYNAMIC_RATE: Byte = 52
    const val CMD_QUERY_PAST_HEART_RATE_1: Byte = 53
    const val CMD_QUERY_PAST_HEART_RATE_2: Byte = 54
    const val CMD_QUERY_MOVEMENT_HEART_RATE: Byte = 55
    const val CMD_QUERY_V2_WORKOUT: Byte = 0xb2.toByte()

    // Health Measurement Commands
    const val CMD_QUERY_TIMING_MEASURE_HEART_RATE: Byte = 47
    const val CMD_SET_TIMING_MEASURE_HEART_RATE: Byte = 31
    const val CMD_START_STOP_MEASURE_DYNAMIC_RATE: Byte = 104
    const val CMD_TRIGGER_MEASURE_BLOOD_PRESSURE: Byte = 105
    const val CMD_TRIGGER_MEASURE_BLOOD_OXYGEN: Byte = 107
    const val CMD_TRIGGER_MEASURE_HEARTRATE: Byte = 109
    const val CMD_ECG: Byte = 111

    // Functionality Commands
    const val CMD_SYNC_TIME: Byte = 49
    const val CMD_SYNC_SLEEP: Byte = 50
    const val CMD_SYNC_PAST_SLEEP_AND_STEP: Byte = 51
    const val CMD_SEND_MESSAGE: Byte = 65
    const val CMD_SET_WEATHER_FUTURE: Byte = 66
    const val CMD_SET_WEATHER_TODAY: Byte = 67
    const val CMD_SET_MUSIC_INFO: Byte = 68
    const val CMD_SET_MUSIC_STATE: Byte = 123

    // Settings Commands
    const val CMD_SET_USER_INFO: Byte = 18
    const val CMD_SET_GOAL_STEP: Byte = 22
    const val CMD_SET_QUICK_VIEW: Byte = 24
    const val CMD_SET_DEVICE_LANGUAGE: Byte = 27
    const val CMD_SET_SEDENTARY_REMINDER: Byte = 29
}
