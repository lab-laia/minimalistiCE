/* 
 * File:   FontHV.h
 * Author: CLL
 *
 * Created on 25 de Novembro de 2019, 16:40
 */

#ifndef FONTHV_H
#define FONTHV_H

#include "Arduino.h"
#include <Wire.h>

class FontHV {
private:
    byte i2c;
    byte nGFI; // number of GFI events;
    boolean isRunning;
    boolean enabled;
public:
    FontHV(byte i2cAddress);
    void run(char id);
    void halt(char id);
    void clear(char id);
    void beep(char id);
    void frozen(char id);
    void unfrozen(char id);
    void enable(char id, boolean shouldEnable);
    boolean isEnabled();
    void getStatus(char id, char stream[]);
    void getMonitor(char id, char stream[]);
    byte getNGFI();
    boolean progRunning();
    void program(char message[]);
};

#endif /* FONTHV_H */
