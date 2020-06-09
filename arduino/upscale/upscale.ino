/*
    Michael Miller
    6/7/2020
*/

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <math.h>
#include <HX711.h>

#define UUID_SERVICE_SCALE      "f52ce065-6405-4cc0-a9eb-60e04533fa48"
#define UUID_CHAR_WEIGHT        "bab027df-7e05-4358-9c2c-53596d940bef"
#define UUID_CHAR_BATTERY       "4c1a2005-a529-4574-9eed-96e7e520f9c5"
#define DEVICE_NAME "upscale"
#define DOUT 4
#define CLK 2

//
#define ALPHA .4 //weight of previous values vs current 
#define SAMPLE_RATE 10 //sample rate in ms
#define SAMPLE_RESOLUTION .5 

HX711 scale;

//Used to determine if new connectinon/disconnection
bool deviceConnected = false;
bool lastDeviceConnected = false;

//determined using calibration script, returns weight in grams, not related to taring
float calibration_factor = -443;

//used for averaging and to determine whether or not to update weight
float lastWeight = 0.0;
float lastRoundedWeight = 0.0;

//built-in blue LED 
int blueLED = 5;

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
  // Initialize serial
  Serial.begin(115200);
  while (!Serial); //wait for serial to initialize

  //Initialize connection LED
  pinMode(blueLED, OUTPUT);
  digitalWrite(blueLED, LOW);

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
  float bat = .77; //placeholder for battery level
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
  Serial.println("Setup done!");
}

void loop() {
  if (deviceConnected) {
    if (!lastDeviceConnected) {
      Serial.println("New device connection");
      digitalWrite(blueLED, HIGH);
      lastDeviceConnected = true; 
    }
    float weight = scale.get_units();
    //get running average weight
    lastWeight = weight * ALPHA + lastWeight * (1 - ALPHA);
    
    float roundedWeight = floorf(lastWeight / SAMPLE_RESOLUTION) * SAMPLE_RESOLUTION;

    // Only send update if weight has changed
    if (roundedWeight != lastRoundedWeight) {
      charWeight->setValue(roundedWeight);
      charWeight->notify();
      lastRoundedWeight = roundedWeight;
      Serial.print("Weight set to: ");
      Serial.println(roundedWeight);
    }
    delay(SAMPLE_RATE);
  }
   //No device
  else {
    if (lastDeviceConnected) {
      Serial.println("New device disconnection");
      digitalWrite(blueLED, LOW);
      delay(500); // give the bluetooth stack the chance to get things ready
      server->startAdvertising(); // restart advertising
      Serial.println("start advertising");
      lastDeviceConnected = false; 
    }
    //Delay when no device is present
    delay(100);
  }
}
