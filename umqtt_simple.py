#umqtt 
import usocket as socket
import ustruct as struct
import utime
from ubinascii import hexlify

class MQTTClient:
    def __init__(self, client_id, server, port=8883):
        self.client_id = client_id
        self.server = server
        self.port = port
        self.sock = None

    def connect(self):
        addr = socket.getaddrinfo(self.server, self.port)[0][-1]
        self.sock = socket.socket()
        self.sock.connect(addr)
        packet = bytearray([0x10, 12, 0, 4]) + b'MQTT' + bytearray([4, 2, 0, 60]) + struct.pack("!H", len(self.client_id)) + self.client_id.encode()
        self.sock.send(packet)

    def publish(self, topic, msg):
        packet = bytearray([0x30, len(topic) + len(msg) + 2])
        self.sock.send(packet + struct.pack("!H", len(topic)) + topic.encode() + msg.encode())

    def disconnect(self):
        self.sock.send(b"\xe0\0")
        self.sock.close()



"""
https://uiflow-micropython.readthedocs.io/en/latest/software/umqtt.html
 # uiflow2 uses this class by default
from umqtt import MQTTClient

# If you want to use the `umqtt.default` module, go this way.
from umqtt.simple import MQTTClient

# If you want to use the `umqtt.robust` module, go this way.
from umqtt.robust import MQTTClient """