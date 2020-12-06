sudo cp server.py /usr/local/bin/
sudo cp ConfigurationBluetooth.sh /usr/local/bin/
sudo systemctl daemon-reload
sudo systemctl start hciconfig.service
sudo systemctl start jeep.service
