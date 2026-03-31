
/* 
 * File:   Pump.h
 * Author: CLL
 *
 * Created on 25 de Novembro de 2019, 18:03
 */

#ifndef PUMP_H
#define PUMP_H

#include "Arduino.h"
#include <HX710AB.h>
#include <Serine.h>

class Pump {
public:
    void config(byte addressPump, byte addressValve, byte pressure_DOUT,
            byte pressure_SCK);
    void halt();
    boolean isControllingPressure();
    long readPressure();
    void updateAmbPressure();
    void pumpItUp(boolean yes);
    void closeValve(boolean yes);
    void pressurize(unsigned long pressure, unsigned long time);
    void pressurize(unsigned long pressure);
    unsigned long getIoP();
    unsigned long getTime();
    void proceed();
private:
    byte aPump;
    byte aValve;
    HX710B* pSensor;
    long a[3];
    byte posADC;
    long ambPressure;
    unsigned long integral;
    unsigned long goalIoP;
    boolean controllingPressure;
    boolean emptying;
    unsigned long setPressure;
    unsigned long minPressure;
    unsigned long maxPressure;
    unsigned long previousTime;
    unsigned long presentTime;
    SerineChronometer minTime;
    SerineChronometer postTime;
    boolean continuousPress;
};

#endif /* PUMP_H */
