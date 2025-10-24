import time
from datetime import datetime
import board
import busio
import adafruit_ads1x15.ads1115 as ADS
from adafruit_ads1x15.analog_in import AnalogIn


i2c = busio.I2C(board.SCL, board.SDA)
ads = ADS.ADS1115(i2c)
ads.gain = 1

#input channel
chan = AnalogIn(ads, ADS.P0)

try:
    while True:
        voltage = chan.voltage
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"{timestamp}, Voltage: {voltage:.4f} V")
        data_log.append((timestamp, voltage))
        print(f"{timestamp} | Voltage: {voltage:.4f} V")
        time.sleep(1)
        
except KeyboardInterrupt:
    print("Exiting...")