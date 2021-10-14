#!/usr/bin/env python3

import bluetooth
import subprocess
import threading
import cv2
import time
import board
from adafruit_motorkit import MotorKit

def fil_video():

    while True:
        server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        server_sock.bind(("", bluetooth.PORT_ANY))
        server_sock.listen(1)

        port = server_sock.getsockname()[1]

        uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ef"

        bluetooth.advertise_service(server_sock, "Video", service_id=uuid,
                                    service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
                                    profiles=[bluetooth.SERIAL_PORT_PROFILE],
                                    )

    
        try:
            print("En attente d'une connection sur le canal RFCOMM", port)

            connection, client_info = server_sock.accept()
            print("Connection pour video de ", client_info)
            
            camareDemaree = False
            while True:
                if camera==True and camareDemaree==False: #Demare la capture video
                    vc = cv2.VideoCapture(0)
                    camareDemaree = True
                elif camera==False and camareDemaree==True: #Ferme la video
                    vc.release()
                elif camera==True and camareDemaree==True:
                    if vc.isOpened(): # try to get the first frame
                        rval, frame = vc.read()
                    else:
                        rval = False

                    while rval:
                        rval, frame = vc.read()
                        resized_image = cv2.resize(frame, (64, 48), interpolation=cv2.INTER_CUBIC)
                        #start_time = time.time()
                        connection.settimeout(5)
                        ret = connection.send(resized_image)
                        client_sock.recv(4)
                        #elapsed_time = time.time() - start_time
                        #print("Video envoyee: "+str(ret)+" bytes en "+str(elapsed_time)+" secondes.")

        except OSError as error:
            print(error)
                
        except bluetooth.btcommon.BluetoothError as error:
            print(error)

        connection.close()
        server_sock.close()

def fil_commandes():

    kit = MotorKit(i2c=board.I2C())
    kit._pca.frequency = 500

    while True:
        server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        server_sock.bind(("", bluetooth.PORT_ANY))
        server_sock.listen(1)

        port = server_sock.getsockname()[1]

        uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

        bluetooth.advertise_service(server_sock, "Commandes", service_id=uuid,
                                    service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
                                    profiles=[bluetooth.SERIAL_PORT_PROFILE],
                                    )

    
        try:
            print("En attente d'une connection sur le canal RFCOMM", port)

            connection, client_info = server_sock.accept()
            print("Connection pour commande de ", client_info)

            while True:
                data = connection.recv(1024)
                if not data:
                    break

                else:
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
                    elif data == b'\x07':
                        camera = True
                    elif data == b'\x08':
                        camera = False
                    else: 
                            kit.motor1.throttle = None
                            kit.motor2.throttle = None

        except OSError as error:
            print(error)

        except bluetooth.btcommon.BluetoothError as error:
            print(error)
            
        connection.close()
        server_sock.close()

# Initialize the camera to On
camera = True

video = threading.Thread(target=fil_video)
video.start()

commandes = threading.Thread(target=fil_commandes)
commandes.start()
