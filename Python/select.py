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

GPIO_BUTTON1 = 7
GPIO_BUTTON2 = 8
GPIO.setup(GPIO_BUTTON1, GPIO.IN)
GPIO.setup(GPIO_BUTTON2, GPIO.IN)

name=""
count=0
nameList=["Alice","Bob","Chris","Daniel","Eddie","Unknown"]
exit=False

def button1_callback(GPIO_BUTTON1):
    global count
    
    print("Name: "+str(nameList[count]))
 
    if (count==5):
        count=0
    else:
        count=count+1
        
GPIO.add_event_detect(GPIO_BUTTON1, GPIO.RISING, button1_callback)

def button2_callback(GPIO_BUTTON2):
    Apicall(str(nameList[count-1]))
    print(str(nameList[count-1])+" has entered.")
    
GPIO.add_event_detect(GPIO_BUTTON2, GPIO.RISING, button2_callback)

def Apicall(name):
    session = requests.Session()
    session.headers.update({"appKey":"ce22e9e4-2834-419c-9656-ef9f844c784c"})
    session.headers.update({"Content-Type":"application/json"})
    session.headers.update({"Accept":"application/json"})
    requestbody = "{\"LastEntered\":\""+name+"\"}"

    baseurl = "http://34.227.165.169/Thingworx/"
    entityurl = baseurl + "Things/"+sys.argv[1]+"/Properties/LastEntered"

    response = session.put(entityurl, data=requestbody)
    print ("PUT URL: " + entityurl)
    print ("PUT Body: " + requestbody)
    print ("PUT Response Code: " + str(response.status_code))
    
if __name__ == '__main__':
    try:
        while ~exit:
            exit=False
    # Exit after STRG+C input
    except KeyboardInterrupt:
        print("Messung vom User gestoppt")
        GPIO.cleanup()
    