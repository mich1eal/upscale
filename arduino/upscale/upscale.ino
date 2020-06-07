/*
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleServer.cpp
    Ported to Arduino ESP32 by Evandro Copercini
    updates by chegewara
*/

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define UUID_SERVICE_SCALE      "f52ce065-6405-4cc0-a9eb-60e04533fa48"
#define UUID_CHAR_WEIGHT        "bab027df-7e05-4358-9c2c-53596d940bef"
#define UUID_CHAR_BATTERY       "4c1a2005-a529-4574-9eed-96e7e520f9c5"

void setup() {
  Serial.begin(115200);
  Serial.println("Starting BLE work!");

  BLEDevice::init("Long name works now");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(UUID_SERVICE_SCALE);
  BLECharacteristic *charWeight = pService->createCharacteristic(
                                         UUID_CHAR_WEIGHT,
                                         BLECharacteristic::PROPERTY_READ 
                                       );
                                       
  BLECharacteristic *charBattery = pService->createCharacteristic(
                                         UUID_CHAR_BATTERY,
                                         BLECharacteristic::PROPERTY_READ 
                                       );

  charWeight->setValue("2");
  charBattery->setValue("5");

  pService->start();
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(UUID_SERVICE_SCALE);
  pAdvertising->setScanResponse(true);
  //pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}

void loop() {
  // put your main code here, to run repeatedly:
  delay(10000); 
  
}
