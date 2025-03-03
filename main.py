#main microphython file for Raspberry Pi Pico
#imports BME280 drivers from bme280.py file
# Main MicroPython file for Raspberry Pi Pico 2W
from machine import Pin, I2C
from time import sleep
import network
from umqtt.simple import MQTTClient
import config
import BME280
import utime
import ujson  # Import ujson for JSON handling

#MQTT Topics
MQTT_TOPIC_SENSOR_DATA = "sensor/data"
MQTT_TOPIC_LED = "sensor/led"  #LED control topic


#using onboard LED for LED topic/control
led = Pin('LED', Pin.OUT)

#MQTT Parameters
MQTT_SERVER = config.mqtt_server
MQTT_PORT = 0
MQTT_USER = config.mqtt_username
MQTT_PASSWORD = config.mqtt_password
MQTT_CLIENT_ID = b"raspberrypi_pico_2_W"
MQTT_KEEPALIVE = 7200
MQTT_SSL = True
MQTT_SSL_PARAMS = {"server_hostname": MQTT_SERVER}

#I2C initialization
i2c = I2C(id=1, scl=Pin(3), sda=Pin(2), freq=10000)
utime.sleep_ms(500)

#Initialize BME280 sensor
#added 500 sleep timer to ensure connection to I2C is set.
bme = BME280.BME280(i2c=i2c, addr=0x77)
utime.sleep_ms(500)


#wifi initialization, uses config.py
def initialize_wifi(ssid, password):
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)

    wlan.connect(ssid, password)

    connection_timeout = 15
    while connection_timeout > 0:
        if wlan.status() >= 3:
            break
        connection_timeout -= 1
        print("Connecting....")
        sleep(1)
#prints status for timeout/wifi connection
    if wlan.status() != 3:
        print("No wifi connection")
        return False
    else:
        print("wifi connected")
        print("IP address:", wlan.ifconfig()[0])
        return True

#retrieves BME280 sensor readings
def get_sensor_readings():
    temp = bme.temperature[:-1]
    hum = bme.humidity[:-1]
    pres = bme.pressure[:-3]
    return temp, hum, pres

def on_message(topic, msg):
    """Callback function triggered when a message is received"""
    topic = topic.decode("utf-8")
    msg = msg.decode("utf-8")
    print(f"Received message on {topic}: {msg}")

    if topic == MQTT_TOPIC_LED:
        if msg.lower() == "on":
            led.value(1)
            print("LED Status = ON")
        elif msg.lower() == "off":
            led.value(0)
            print("LED Status = OFF")
        else:
            print(f"Other error received: {msg}")

def connect_mqtt():
    try:
        client = MQTTClient(
            client_id=MQTT_CLIENT_ID,
            server=MQTT_SERVER,
            port=MQTT_PORT,
            user=MQTT_USER,
            password=MQTT_PASSWORD,
            keepalive=MQTT_KEEPALIVE,
            ssl=MQTT_SSL,
            ssl_params=MQTT_SSL_PARAMS
        )

        client.set_callback(on_message)
        client.connect()
        client.subscribe(MQTT_TOPIC_LED)  #Subscribe to LED topic
        print(f"Subscribed to {MQTT_TOPIC_LED}, waiting for messages...")
        return client
    except Exception as e:
        print("Error connecting to MQTT:", e)
        raise  

#temperature/pressure/humidity publish
def publish_mqtt(client, topic, value):
    try:
        client.publish(topic, value)
        print(f"Published to {topic}: {value}")
    except Exception as e:
        print(f"Error publishing message: {e}")
#error handling for wifi connection being lost on RBP pico
try:
    if not initialize_wifi(config.wifi_ssid, config.wifi_password):
        print("no wifi connection found")
    else:
        client = connect_mqtt()

        while True:
            #read sensor data, format it in JSON format for publishing
            temperature, humidity, pressure = get_sensor_readings()
            sensor_data = ujson.dumps({
                "temperature": temperature,
                "humidity": humidity,
                "pressure": pressure
            })

            # Publish sensor data
            publish_mqtt(client, MQTT_TOPIC_SENSOR_DATA, sensor_data)

            #Check for new incoming MQTT messages
            print("Checking for MQTT messages...")
            for _ in range(3):  #Check messages every 3 seconds
                client.check_msg()
                sleep(1)
#general error handling
except Exception as e:
    print("Error:", e)

