#Import libraries
#import RPi.GPIO as GPIO
import time
import requests
from requests.auth import HTTPDigestAuth
import json

#Disable warnings if setup was already done in an earlier instance
#GPIO.setwarnings(False)
 
#Set GPIO Mode (BOARD / BCM)
#GPIO.setmode(GPIO.BCM)
 
#Set GPIO Pins
#GPIO_TRIGGER = 23
#GPIO_ECHO = 24
 
#Set directions of GPIO-Pins (IN / OUT)
#GPIO.setup(GPIO_TRIGGER, GPIO.OUT)
#GPIO.setup(GPIO_ECHO, GPIO.IN)

baseurl = "http://34.227.165.169/Thingworx/"

session=requests.Session()
session.headers.update({"appKey":"ce22e9e4-2834-419c-9656-ef9f844c784c"})
session.headers.update({"Content-Type":"application/json"})
session.headers.update({"Accept":"application/json"})

def getdistance():
    #Set Trigger HIGH
#    GPIO.output(GPIO_TRIGGER, True)
 
    #Set Trigger LOW after 0.01ms
    time.sleep(0.00001)
#    GPIO.output(GPIO_TRIGGER, False)
 
    StartZeit = time.time()
    StopZeit = time.time()
 
    #Save start time
#    while GPIO.input(GPIO_ECHO) == 0:
 #       StartZeit = time.time()

    #Save time of return signal
#    while GPIO.input(GPIO_ECHO) == 1:
  #      StopZeit = time.time()

    #get elapsed time between start and return
    TimeElapsed = StopZeit - StartZeit
    #multiply with speed of sound (34300cm/s)
    #divide by to besauce signal traveled twice the distance
    distanz = (TimeElapsed * 34300) / 2
 
    return distanz


 
def Apicall(httprequest, object, value = 0):

    entityurl=baseurl+"Things/ClientThing_01/Properties/"+object    

    if httprequest == 'PUT':
        requestbody = "{\""+object+"\":\""+str(float("{0:.2f}".format(value)))+"\"}"
        response = session.put(entityurl,data=requestbody)
        print ("PUT URL: "+entityurl)
        print ("PUT Body: "+requestbody)
        print ("PUT Response Code: "+str(response.status_code))
    elif httprequest == 'GET':
        response = session.get(entityurl)
        print ("GET URL: "+entityurl)
        jData = json.loads(response.content)    
        print ("GET Response Code: "+str(response.status_code))
        print ("JSON:\n"+str(jData))

if __name__ == '__main__':
    try:
        distance = 0
        while True:
            #distance = getdistance()
            distance=distance + 1 
            print ("Gemessene Entfernung = %.1f cm" % distance)
	    print ("Call PUT")
	    Apicall("PUT","Distance",distance)          
            time.sleep(10)
 
    #Exit after STRG+C input
    except KeyboardInterrupt:
        print("Messung vom User gestoppt")
        GPIO.cleanup()
