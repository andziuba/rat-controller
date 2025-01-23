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


def lie_down():
    sleep(1)
    servoFL1.angle = -90
    servoFL2.angle = 60
    sleep(1)
    servoBR1.angle = 90
    servoBR2.angle = 0
    sleep(1)
    servoBL1.angle = -75
    servoBL2.angle = 90
    sleep(1)
    servoFR1.angle = -70
    servoFR2.angle = 0


def center():
    servoFL1.angle = 0
    servoFL2.angle = 60
    sleep(0.2)
    servoBR1.angle = -15
    servoBR2.angle = 15
    sleep(0.2)
    servoBL1.angle = 30
    servoBL2.angle = -90
    sleep(0.2)
    servoFR1.angle = 30
    servoFR2.angle = 0
    sleep(0.2)


def walkBack():
    center()
    servoFR1.angle = 90
    servoFR2.angle = -30
    sleep(0.2)
    servoBR2.angle = 60
    servoBL1.angle = 75
    servoBL2.angle = -60
    sleep(0.2)
    servoFL1.angle = 60
    servoFL2.angle = 90
    sleep(0.2)
    servoBL2.angle = -80


# more like roll back...
def walk():
    center()
    servoFL1.angle = -90
    servoFR1.angle = -70
    sleep(0.2)
    servoBL1.angle = -30
    servoBR1.angle = 45
    sleep(0.2)


def turnLeft():
    center()
    servoFL1.angle = -90
    servoFR1.angle = -70
    sleep(0.2)
    servoBL1.angle = -30
    servoBR1.angle = -45
    sleep(0.2)


def turnRight():
    center()
    servoFL1.angle = 90
    servoFL2.angle = -90
    sleep(0.2)
    servoBL1.angle = 90
    servoBL2.angle = -75
    sleep(0.2)
    servoFR1.angle = -15
    servoFR2.angle = 30
    sleep(0.2)
    servoBR1.angle = 90
    sleep(0.2)

    servoFL1.angle = -90
    servoBL1.angle = -90

def create_database():
    conn = sqlite3.connect('directions.db')
    cursor = conn.cursor()
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS directions (
     id INTEGER PRIMARY KEY AUTOINCREMENT,
    direction VARCHAR)''')
    cursor.execute(''' DELETE FROM directions''')
    conn.commit()
    conn.close()

def save_to_table(direction):
    conn = sqlite3.connect('directions.db')
    cursor = conn.cursor()
    cursor.execute('''
    INSERT INTO directions (direction)
    VALUES (?)
    ''', (direction,))  # Note the trailing comma here
    conn.commit()
    conn.close()


def display_table():
    conn = sqlite3.connect('directions.db')
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM directions")
    rows = cursor.fetchall()
    for row in rows:
        print(row)
    conn.close()

if __name__ == '__main__':
    create_database()
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    server_socket.bind(('192.168.34.27', 1100))

    server_socket.listen(1)

    print("Serwer oczekuje na polaczenie...")

    try:
        lie_down()
        while True:
            client_socket, addr = server_socket.accept()
            # print(f"Połączono z: {addr}")

            # Odbieranie danych
            data = client_socket.recv(1024)
            command = data.decode('utf-8')
            print(f"Otrzymano: {command}")
            command = command.strip('\n')
            save_to_table(command)

            if "Forward" in command:
                walk()
            elif "Backward" in command:
                walkBack()
            elif "Left" in command:
                turnLeft()
            elif "Right" in command:
                turnRight()
            elif "Rest" in command:
                lie_down()
                sleep(1)

            client_socket.close()
    except KeyboardInterrupt:
        display_table()
        lie_down()
        server_socket.close()
