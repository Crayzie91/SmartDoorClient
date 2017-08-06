# Import libraries
import RPi.GPIO as GPIO
import time
import logging, sys

import requests
from requests.auth import HTTPDigestAuth
import json

from neopixel import *

logging.basicConfig(stream=sys.stderr, level=logging.INFO)

# Distance sensor configuration
# Disable warnings if setup was already done in an earlier instance
GPIO.setwarnings(False)
# Set GPIO Mode (BOARD / BCM)
GPIO.setmode(GPIO.BCM)
# Set GPIO Pins
GPIO_TRIGGER = 23
GPIO_ECHO = 24
# Set directions of GPIO-Pins (IN / OUT)
GPIO.setup(GPIO_TRIGGER, GPIO.OUT)
GPIO.setup(GPIO_ECHO, GPIO.IN)

# LED configuration:
LED_COUNT      = 1       # Number of LED pixels.
LED_PIN        = 10      # GPIO pin connected to the pixels (10 uses SPI /dev/spidev0.0).
LED_FREQ_HZ    = 800000  # LED signal frequency in hertz (usually 800khz)
LED_DMA        = 5       # DMA channel to use for generating signal (try 5)
LED_BRIGHTNESS = 30     # Set to 0 for darkest and 255 for brightest
LED_INVERT     = False   # True to invert the signal (when using NPN transistor level shift)
LED_CHANNEL    = 0       # set to '1' for GPIOs 13, 19, 41, 45 or 53
LED_STRIP      = ws.WS2811_STRIP_GRB   # Strip type and color ordering

# Set GPIO and callback to remotely open the door
GPIO_DOOR = 25
GPIO_REMOTE_TRIGGER = 18
GPIO.setup(GPIO_REMOTE_TRIGGER, GPIO.IN)
GPIO.setup(GPIO_DOOR, GPIO.IN)

def remote_callback(GPIO_REMOTE_TRIGGER):
    openDoor(True)
    logging.info ("Entered Interrupt")
 
GPIO.add_event_detect(GPIO_REMOTE_TRIGGER, GPIO.RISING, remote_callback)

def door_callback(GPIO_DOOR):
    if(GPIO.input(GPIO_DOOR)):
        openDoor(False)
        Apicall("PUT", "isOpen", False)
        logging.info("Door was opened")
    else:        
        logging.info ("Door was closed")
 
GPIO.add_event_detect(GPIO_DOOR, GPIO.BOTH, door_callback)
   
# URL of the Thingworx server
baseurl = "http://34.227.165.169/Thingworx/"
client = ""

session = requests.Session()
session.headers.update({"appKey":"ce22e9e4-2834-419c-9656-ef9f844c784c"})
session.headers.update({"Content-Type":"application/json"})
session.headers.update({"Accept":"application/json"})

def getdistance():
    # Set Trigger HIGH
    GPIO.output(GPIO_TRIGGER, True)
 
    # Set Trigger LOW after 0.01ms
    time.sleep(0.00001)
    GPIO.output(GPIO_TRIGGER, False)
 
    StartZeit = time.time()
    StopZeit = time.time()
 
    # Save start time
    while GPIO.input(GPIO_ECHO) == 0:
       StartZeit = time.time()

    # Save time of return signal
    while GPIO.input(GPIO_ECHO) == 1:
       StopZeit = time.time()

    # get elapsed time between start and return
    TimeElapsed = StopZeit - StartZeit
    # multiply with speed of sound (34300cm/s)
    # divide by to besauce signal traveled twice the distance
    distanz = (TimeElapsed * 34300) / 2
 
    return distanz

def setLedColor(LEDcolor):
    led.setPixelColor(0, LEDcolor)
    led.show()

def openDoor(state):
    if(state):
        setLedColor(Color(255, 0, 0)) #Green
    else:
        setLedColor(Color(0, 255, 0)) #Red

def Apicall(httprequest, object, value=0):

    entityurl = baseurl + "Things/"+client+"/Properties/" + object    

    if httprequest == 'PUT':
        requestbody = "{\"" + object + "\":\"" + str(float("{0:.2f}".format(value))) + "\"}"
        response = session.put(entityurl, data=requestbody)
        logging.debug ("PUT URL: " + entityurl)
        logging.debug ("PUT Body: " + requestbody)
        logging.debug ("PUT Response Code: " + str(response.status_code))
    elif httprequest == 'GET':
        response = session.get(entityurl)
        jData = json.loads(response.content)    
        logging.debug ("GET URL: " + entityurl)
        logging.debug ("GET Response Code: " + str(response.status_code))
        logging.debug ("JSON:\n" + str(jData))
    logging.debug("\n")
        
if __name__ == '__main__':
    # Create NeoPixel object with appropriate configuration.
    led = Adafruit_NeoPixel(LED_COUNT, LED_PIN, LED_FREQ_HZ, LED_DMA, LED_INVERT, LED_BRIGHTNESS, LED_CHANNEL, LED_STRIP)
    # Intialize the library (must be called once before other functions).
    led.begin()
 
    global client
    client = str(sys.argv[1])
 
    # Signal at systemstart if the door is open with blue blinking led
    while(GPIO.input(GPIO_DOOR)):
        setLedColor(Color(0, 0, 255)) #Blue
        time.sleep(1)
        setLedColor(Color(0, 0, 0)) #Off
        time.sleep(1)
 
    openDoor(False)

    try:
        distance = 0
        while True:
            distance = getdistance()
            logging.info ("Gemessene Entfernung = %.1f cm" % distance)
            Apicall("PUT", "Distance", distance)  
            time.sleep(5)  

    # Exit after STRG+C input
    except KeyboardInterrupt:
        logging.info("Messung vom User gestoppt")
        GPIO.cleanup()
