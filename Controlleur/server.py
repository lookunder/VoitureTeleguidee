#!/usr/bin/env python3

import bluetooth
import subprocess
import board
from adafruit_motorkit import MotorKit

kit = MotorKit(i2c=board.I2C())
kit._pca.frequency = 500

server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
server_sock.bind(("", bluetooth.PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

bluetooth.advertise_service(server_sock, "VoitureTeleguidee", service_id=uuid,
                            service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
                            profiles=[bluetooth.SERIAL_PORT_PROFILE],
                            )

while True:
    try:
        print("En attente d'une connection sur le canal RFCOMM", port)

        client_sock, client_info = server_sock.accept()
        print("Connection de ", client_info)

        try:
            while True:
                data = client_sock.recv(1024)
                if not data:
                    break
                print("Recu : ", data)

                if data == b'\x00': 
                            kit.motor2.throttle = 1.0
                elif data == b'\x01': 
                            kit.motor2.throttle = -1.0
                elif data == b'\x02': 
                            kit.motor2.throttle = None
                elif data == b'\x03': 
                            kit.motor1.throttle = 0.6
                elif data == b'\x04': 
                            kit.motor1.throttle = -0.6
                elif data == b'\x05': 
                            kit.motor1.throttle = None
                elif data == b'\x06': 
                            kit.motor1.throttle = None
                            kit.motor2.throttle = None
                            subprocess.call(['poweroff'], shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                else: 
                            kit.motor1.throttle = None
                            kit.motor2.throttle = None

        except OSError:
            pass
    except bluetooth.btcommon.BluetoothError:
        print("Deconnection")

print("Deconnection.")

client_sock.close()
server_sock.close()
print("All done.")
