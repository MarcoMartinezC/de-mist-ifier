# de-mist-ifier
A miniature weather station



This project implements a Raspberry Pi Pico 2 W, a HiveMQ Cloud MQTT Server, a Kotlin Android application, and a BME280 Sensor to make up a weather sensing station capable of uploading data to the internet and displaying it on an Android phone. The BME280 sensor transmits values via I2C to the Raspberry Pi, which are then converted into a JSON object. Once this is done, the Raspberry Pi connects to a WiFi network, a HiveMQ server, and publishes the JSON under the "sensor/data" topic. The Android app connects via paho/mqttv3 implementations, and subscribes to pull the JSON from the HiveMQ server. The JSON data is converted to string-type, and parsed to look for Temperature, Humidity, and Pressure data. This data is then displayed on screen, and refreshed every time the server posts a new message. The Android application, in turn, publishes LED data, which controls the built in LED on the Raspberry Pi.


![Screenshot 2025-03-03 at 12 47 45 AM](https://github.com/user-attachments/assets/2b1c8134-25db-4aae-a75b-68fbb20899ae)


Kotlin Implementation
The Kotlin implementation uses 2 main files: MqttHelper.kt and MainActivity.kt. MqttHelper implements a single class, MqttHelper. This class imports eclipse/paho MQTT v3 dependancies and defines the MQTT server URL, client ID, username, and password, (the latter two also setup on the HiveMQ server). The class then sets up message and connection listeners for the incoming HiveMQ server connection. After this is established, it awaits a JSON object to be published, and subsequently converted to String types. Following this conversion, the string is parsed for "temperature", "humidity" and "pressure", which are stored as values, called upon by composables in the MainActivity.kt file.
A publish function is also included, for the status of the LED Control toggle switch. Once this is toggled, the topic of the message is "sensor/led", with the data consisting of "on" or "off". 

The application imports the Eclipse Paho Java MQTT library to implement clients, check connections, and read/publish data. Internet permission was added to the AndoidManifest.xml file via "<uses-permission android:name="android.permission.INTERNET"/>".


Microcontroller Implementation
The Raspberry Pi Pico 2 W supports MicroPython for the programs executed from internal memory. The following Python files were used for dependency/driver support:
	BME280.py - the MicroPython driver for the BME280
	config.py - wireless SSID and password for WiFi connectivity
	simple.py & robust.py - MQTT client implementation for the Raspberry Pi. 

A main.py file was used for the main program that the Raspberry Pi loads and runs. The file imports the BME280 driver, enables I2C on pins 3 and 4, at 10000 Hz frequency, and sets a 500 ms timer to ensure that I2C connection is established prior to continuing through the program.
WiFi connection is established, using config.py, and a timeout of 15 seconds is set. After this, the BME280 sensor readings are retrieved, along with the LED status (reading "ON" or "OFF" as strings, this is not a boolean value). 
If connection to the MQTT server is established, then a publish function is called, sending the sensor readings in JSON format, and under the topic sensor/data. After this is done, the program looks for new incoming messages from the server, at a 3 second refresh rate.

Video Demo of Application, Rasberry Pi MQTT broker, and HiveMQ Cloud Server: https://www.youtube.com/watch?v=Lkvbq-PwJ9s
