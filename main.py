#main microphython file for Raspberry Pi Pico
#imports BME280 drivers from bme280.py file
from machine import Pin, I2C
import time
from bme280 import BME280
from umqtt_simple import MQTTClient


#MQTT Configuration
MQTT_BROKER = "ssl:18a8caf27c6b40449526c0491ea5c727.s1.eu.hivemq.cloud:8883"  #broker's IP or hostname
MQTT_TOPIC = "sensor/data"
CLIENT_ID = "PicoWeatherStation"

#I2C initialization on I2C0 (GP4=SDA, GP5=SCL)
i2c = I2C(0, scl=Pin(5), sda=Pin(4), freq=100000)


#BME280 initialize via I2C
bme = BME280(i2c)

while True:

    temperature = bme.read_temperature()
    pressure = bme.read_pressure()
    humidity = bme.read_humidity()

    #data sent
    payload = f'{{"temperature": {temperature:.2f}, "pressure": {pressure:.2f}, "humidity": {humidity:.2f}}}'
    print("Publishing:", payload)

    mqtt_client.publish(MQTT_TOPIC, payload)
    time.sleep(5)

