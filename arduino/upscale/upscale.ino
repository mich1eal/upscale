/*
    Michael Miller
    6/7/2020
*/

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <HX711.h>

#define UUID_SERVICE_SCALE      "f52ce065-6405-4cc0-a9eb-60e04533fa48"
#define UUID_CHAR_WEIGHT        "bab027df-7e05-4358-9c2c-53596d940bef"
#define UUID_CHAR_BATTERY       "4c1a2005-a529-4574-9eed-96e7e520f9c5"
#define DEVICE_NAME "upscale"
#define DOUT 4
#define CLK 2

HX711 scale;

//Used to determine if new connectinon/disconnection
bool deviceConnected = false;
bool lastDeviceConnected = false;

//determined using calibration script, returns weight in grams, not related to taring
float calibration_factor = -443;

//BLEDevice device; 
//BLEService scaleService; 
BLECharacteristic* charWeight = NULL;
BLECharacteristic* charBattery = NULL;
BLEServer* server = NULL; 

class ScaleServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};


void setup() {
  Serial.begin(115200);
  while (!Serial); //wait for serial to initialize

  // Initialize scale
  scale.begin(DOUT, CLK);
  scale.set_scale(calibration_factor);
  scale.tare(); 

  // Initialize BLE Server
  BLEDevice::init(DEVICE_NAME);
  server = BLEDevice::createServer();
  server->setCallbacks(new ScaleServerCallbacks());

  // Create Service
  BLEService *scaleService = server->createService(UUID_SERVICE_SCALE);
  
  // Create characts
  charWeight = scaleService->createCharacteristic(
    UUID_CHAR_WEIGHT, 
    BLECharacteristic::PROPERTY_READ | 
    BLECharacteristic::PROPERTY_NOTIFY);
 
  charBattery = scaleService->createCharacteristic(
    UUID_CHAR_BATTERY,
    BLECharacteristic::PROPERTY_READ | 
    BLECharacteristic::PROPERTY_NOTIFY);

  float weight = scale.get_units();
  float bat = .77;
  charWeight->setValue(weight);
  charBattery->setValue(bat);

  scaleService->start();
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
  if (deviceConnected) {
    if (!lastDeviceConnected) {
      Serial.println("New device connection");
      //Do stuff on first connection
      lastDeviceConnected = true; 
    }
    float weight = scale.get_units();
    charWeight->setValue(weight);
    charWeight->notify();
    delay(100); // bluetooth stack will go into congestion, if too many packets are sent, in 6 hours test i was able to go as low as 3ms
  }
   //No device
  else {
    if (lastDeviceConnected) {
      Serial.println("New device disconnection");
      delay(500); // give the bluetooth stack the chance to get things ready
      server->startAdvertising(); // restart advertising
      Serial.println("start advertising");
      lastDeviceConnected = false; 
    }
    //Normal disconnect stuff
    delay(100);
  }
  
}
