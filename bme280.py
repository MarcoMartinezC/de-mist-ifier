# bme280.py - BME280 I2C Driver for MicroPython
import struct

class BME280:
    def __init__(self, i2c, address=0x76):
        self.i2c = i2c
        self.address = address
        self._load_calibration()
        self._configure_sensor()

    def _read(self, addr, length):
        return self.i2c.readfrom_mem(self.address, addr, length)

    def _write(self, addr, data):
        self.i2c.writeto_mem(self.address, addr, bytearray([data]))

    def _load_calibration(self):
        calib = self._read(0x88, 24)  # Read temp and pressure calibration
        self.dig_T1, self.dig_T2, self.dig_T3 = struct.unpack('<Hhh', calib[:6])
        self.dig_P1, self.dig_P2, self.dig_P3, self.dig_P4, self.dig_P5, self.dig_P6, self.dig_P7, self.dig_P8, self.dig_P9 = struct.unpack('<Hhhhhhhhh', calib[6:24])

        calib_h = self._read(0xA1, 1) + self._read(0xE1, 7)  # Read humidity calibration
        self.dig_H1 = calib_h[0]
        self.dig_H2, self.dig_H3, h4, h5, self.dig_H6 = struct.unpack('<hBbBb', calib_h[1:])
        self.dig_H4 = (h4 << 4) | (h5 & 0x0F)
        self.dig_H5 = (h5 >> 4) | (self.dig_H6 << 4)

    def _configure_sensor(self):
        self._write(0xF2, 0x01)  # Humidity oversampling x1
        self._write(0xF4, 0x27)  # Pressure and temperature oversampling x1, normal mode
        self._write(0xF5, 0xA0)  # Standby 1000ms, filter off

    def read_temperature(self):
        raw_temp = struct.unpack('>i', self._read(0xFA, 3) + b'\x00')[0] >> 4
        var1 = ((raw_temp / 16384.0) - (self.dig_T1 / 1024.0)) * self.dig_T2
        var2 = (((raw_temp / 131072.0) - (self.dig_T1 / 8192.0)) ** 2) * self.dig_T3
        return (var1 + var2) / 5120.0

    def read_pressure(self):
        raw_pres = struct.unpack('>i', self._read(0xF7, 3) + b'\x00')[0] >> 4
        var1 = self.read_temperature() - 30.0
        var2 = var1 * var1 * self.dig_P6
        var2 += (var1 * self.dig_P5) * 2.0
        var2 = (var2 / 4.0) + (self.dig_P4 * 65536.0)
        var1 = ((self.dig_P3 * var1 * var1) / 524288.0 + (self.dig_P2 * var1)) / 524288.0
        var1 = (1.0 + (var1 / 32768.0)) * self.dig_P1
        if var1 == 0:
            return 0
        pressure = 1048576.0 - raw_pres
        pressure = ((pressure - (var2 / 4096.0)) * 6250.0) / var1
        var1 = (self.dig_P9 * pressure * pressure) / 2147483648.0
        var2 = (pressure * self.dig_P8) / 32768.0
        pressure += (var1 + var2 + self.dig_P7) / 16.0
        return pressure / 100  # Convert to hPa

    def read_humidity(self):
        raw_hum = struct.unpack('>H', self._read(0xFD, 2))[0]
        temp = self.read_temperature()
        var1 = raw_hum - ((self.dig_H4 * 64.0) + (self.dig_H5 / 16384.0) * temp)
        var2 = self.dig_H2 / 65536.0
        var3 = (1.0 + (self.dig_H3 / 67108864.0) * temp)
        var4 = 1.0 + (self.dig_H6 / 67108864.0) * temp * var3
        humidity = var1 * var2 * var3 * var4
        return max(0, min(100, humidity))  # Clamp to 0-100% RH
