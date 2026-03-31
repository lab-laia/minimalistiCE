/* 
 * File:   config.h
 * Author: CLL
 *
 * Created on 2025 02 21
 */

#ifndef CONFIG_H
#define CONFIG_H

const long MAX_LONG = 2147483647L;
const long MAX_UL = 4294967295UL;

/* I2C devices */
const byte I2C_HVpowerSource = 0x13;

/* Arudino UNO R4 minima pins */
const byte PRESSURE_DOUT_PIN = 6;
const byte PRESSURE_SCK_PIN = 5;
const byte PRESSURE_PUMP_PIN = 0;
const byte PRESSURE_VALV_PIN = 1;
const byte LED_DATA_PIN = 10;

/* Pump */
// const float PRESSURE_SENSOR_FACTOR = 0.0133; // readings in Pascal - sensor convencional 25k5
//const float PRESSURE_SENSOR_FACTOR = 0.01355; // readings in Pascal - sensor 1
const float PRESSURE_SENSOR_FACTOR = 0.00927; // readings in Pascal - sensor 2

#endif /* CONFIG_H */
