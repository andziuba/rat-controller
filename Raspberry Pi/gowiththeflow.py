from walking import *

from gpiozero import AngularServo, Servo, Device
from time import sleep
import socket
import sqlite3

from gpiozero.pins.pigpio import PiGPIOFactory

factory = PiGPIOFactory()

servoBL1 = AngularServo(2, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)
servoBR1 = AngularServo(3, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)
servoFL1 = AngularServo(4, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)
servoFR1 = AngularServo(14, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)

servoFL2 = AngularServo(15, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)
servoBL2 = AngularServo(18, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)
servoFR2 = AngularServo(17, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)
servoBR2 = AngularServo(27, min_pulse_width=0.5 / 1000, max_pulse_width=2.5 / 1000, pin_factory=factory)


conn = sqlite3.connect('directions.db')
cursor = conn.cursor()
cursor.execute("SELECT * FROM directions order by id asc")
rows = cursor.fetchall()
for row in rows:
    print(row)
    if "Forward" in row:
        walk()
    elif "Backward" in row:
        walkBack()
    elif "Left" in row:
        turnLeft()
    elif "Right" in row:
        turnRight()
    elif "Rest" in row:
        lie_down()
conn.close()