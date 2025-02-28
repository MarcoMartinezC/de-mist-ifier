# bme280.py - BME280 I2C Driver for MicroPython
import time
import struct
#test change
class BME280:
    def __init__(self, i2c, address=0x76):
        self.i2c = i2c
        self.address = address
        self._load_calibration()

    def _read(self, addr, length):
        return self.i2c.readfrom_mem(self.address, addr, length)

    def _write(self, addr, data):
        self.i2c.writeto_mem(self.address, addr, bytearray([data]))

    def _load_calibration(self):
        calib = self._read(0x88, 24)
        self.dig_T1, self.dig_T2, self.dig_T3 = struct.unpack('<Hhh', calib[:6])
    
    def read_temperature(self):
        raw_temp = struct.unpack('>i', self._read(0xFA, 3) + b'\x00')[0] >> 4
        var1 = ((raw_temp / 16384.0) - (self.dig_T1 / 1024.0)) * self.dig_T2
        var2 = (((raw_temp / 131072.0) - (self.dig_T1 / 8192.0)) ** 2) * self.dig_T3
        return (var1 + var2) / 5120.0
