/* 
 * File:   FontHV.cpp
 * Author: CLL
 * 
 * Created on 25 de Novembro de 2019, 16:40
 */

#include "FontHV.h"

void sendI2C(byte destino, char message[]) {
  Wire.beginTransmission(destino);
  Wire.write(message);
  Wire.endTransmission();
}

FontHV::FontHV(byte i2cAddress) {
  i2c = i2cAddress;
  isRunning = false;
  enabled = false;
}

void FontHV::run(char id)  // dispara o programa da fonte de HV
{
  char message[5] = "h_R;";
  message[1] = id;
  sendI2C(i2c, message);
  nGFI = 0;
  isRunning = true;
}

void FontHV::halt(char id) {
  char message[5] = "h_H;";
  message[1] = id;
  sendI2C(i2c, message);
  isRunning = false;
}

void FontHV::clear(char id) {
  char message[5] = "h_C;";
  message[1] = id;
  sendI2C(i2c, message);
}

void FontHV::beep(char id) {
  char message[5] = "h_B;";
  message[1] = id;
  sendI2C(i2c, message);
}

void FontHV::frozen(char id) {
  char message[5] = "h_F;";
  message[1] = id;
  sendI2C(i2c, message);
}

void FontHV::unfrozen(char id) {
  char message[5] = "h_U;";
  message[1] = id;
  sendI2C(i2c, message);
}

void FontHV::enable(char id, boolean enableHV) {
  char message[5] = "h_.;";
  message[1] = id;
  message[2] = enableHV ? 'E' : 'D';
  sendI2C(i2c, message);
  enabled = enableHV;
}

boolean FontHV::isEnabled() {
  return enabled;
}

void FontHV::getStatus(char id, char stream[]) {
  delay(1);
  char message[5] = "h_S;";
  message[1] = id;
  sendI2C(i2c, message);
  delay(5);
  Wire.requestFrom(i2c, byte(13));
  for (int i = 0; i < 13; i++) stream[i] = Wire.read();
  stream[13] = '\0';
  if (stream[3] == 'f') nGFI++;     // atualiza o número de ocorrências de GFI
  isRunning = stream[4] == 'r';  // indica se tem programa rodando;
}

boolean FontHV::progRunning() {
  return isRunning;
}

void FontHV::getMonitor(char id, char stream[]) {
  delay(1);
  char message[5] = "h_G;";
  message[1] = id;
  sendI2C(i2c, message);
  delay(5);
  Wire.requestFrom(i2c, byte(29));
  for (int i = 0; i < 29; i++) stream[i] = Wire.read();
  stream[29] = '\0';
}

byte FontHV::getNGFI() {
  return nGFI;
}

void FontHV::program(char message[]) {
  sendI2C(i2c, message);
  delay(1);
}
