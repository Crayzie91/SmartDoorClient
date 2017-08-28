# Import libraries
import RPi.GPIO as GPIO
import time
import requests
import sys
from requests.auth import HTTPDigestAuth
#from periphery import trigger

# Distance sensor configuration
# Disable warnings if setup was already done in an earlier instance
GPIO.setwarnings(False)
# Set GPIO Mode (BOARD / BCM)
GPIO.setmode(GPIO.BCM)

GPIO_REMOTE_TRIGGER = 18
GPIO_REMOTE_TRIGGER_SELECT1 = 19
GPIO_REMOTE_TRIGGER_SELECT2 = 20
GPIO.setup(GPIO_REMOTE_TRIGGER, GPIO.OUT)
GPIO.setup(GPIO_REMOTE_TRIGGER_SELECT1, GPIO.OUT)
GPIO.setup(GPIO_REMOTE_TRIGGER_SELECT2, GPIO.OUT)

print(sys.argv[2])
# set trigger select depending on event
if(sys.argv[2]=="Open"):
    GPIO.output(GPIO_REMOTE_TRIGGER_SELECT1, True)
    GPIO.output(GPIO_REMOTE_TRIGGER_SELECT2, True)
elif(sys.argv[2]=="Closed"):
    GPIO.output(GPIO_REMOTE_TRIGGER_SELECT1, False)
    GPIO.output(GPIO_REMOTE_TRIGGER_SELECT2, False)
else:
    GPIO.output(GPIO_REMOTE_TRIGGER_SELECT1, True)
    GPIO.output(GPIO_REMOTE_TRIGGER_SELECT2, False)

# trigger Interrupt of periphery script
GPIO.output(GPIO_REMOTE_TRIGGER, True)
time.sleep(1)
GPIO.output(GPIO_REMOTE_TRIGGER, False)
GPIO.output(GPIO_REMOTE_TRIGGER_SELECT1, False)
GPIO.output(GPIO_REMOTE_TRIGGER_SELECT2, False)

#trigger(sys.argv[2])

# create http session and call rest api of thingworx platform
session = requests.Session()
session.headers.update({"appKey":"ce22e9e4-2834-419c-9656-ef9f844c784c"})
session.headers.update({"Content-Type":"application/json"})
session.headers.update({"Accept":"application/json"})

baseurl = "http://34.252.164.220/Thingworx/"
entityurl = baseurl + "Things/"+sys.argv[1]+"/Properties/DoorStatus"
requestbody = "{\"DoorStatus\":\""+sys.argv[2]+"\"}"

response = session.put(entityurl, data=requestbody)
print ("PUT URL: " + entityurl)
print ("PUT Body: " + requestbody)
print ("PUT Response Code: " + str(response.status_code))

GPIO.cleanup()