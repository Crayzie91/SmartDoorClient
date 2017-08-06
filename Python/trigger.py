# Import libraries
import RPi.GPIO as GPIO
import time
import requests
import sys
from requests.auth import HTTPDigestAuth

# Distance sensor configuration
# Disable warnings if setup was already done in an earlier instance
GPIO.setwarnings(False)
# Set GPIO Mode (BOARD / BCM)
GPIO.setmode(GPIO.BCM)

GPIO_REMOTE_TRIGGER = 18
GPIO.setup(GPIO_REMOTE_TRIGGER, GPIO.OUT)
GPIO.output(GPIO_REMOTE_TRIGGER, True)
time.sleep(0.01)
GPIO.output(GPIO_REMOTE_TRIGGER, False)

session = requests.Session()
session.headers.update({"appKey":"ce22e9e4-2834-419c-9656-ef9f844c784c"})
session.headers.update({"Content-Type":"application/json"})
session.headers.update({"Accept":"application/json"})
requestbody = "{\"isOpen\":\"True\"}"

baseurl = "http://34.227.165.169/Thingworx/"
entityurl = baseurl + "Things/"+sys.argv[1]+"/Properties/isOpen"

response = session.put(entityurl, data=requestbody)
print ("PUT URL: " + entityurl)
print ("PUT Body: " + requestbody)
print ("PUT Response Code: " + str(response.status_code))