# Import libraries
import RPi.GPIO as GPIO
import time
import logging, sys

import requests
from requests.auth import HTTPDigestAuth
import json

from neopixel import *

import lcddriver 
from _ast import Str

logging.basicConfig(stream=sys.stderr, level=logging.INFO) # Setup of log

lcd = lcddriver.lcd() # Initialization of lcd display

# Distance sensor configuration
# Disable warnings if setup was already done in an earlier instance
GPIO.setwarnings(False)
# Set GPIO Mode (BOARD / BCM)
GPIO.setmode(GPIO.BCM)
# Set GPIO Pins
GPIO_TRIGGER = 16
GPIO_ECHO = 19
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

# Set GPIO and callback to remotely handle the door
GPIO_DOOR = 17
GPIO_REMOTE_TRIGGER = 18
GPIO_REMOTE_TRIGGER_SELECT1 = 20
GPIO_REMOTE_TRIGGER_SELECT2 = 21
GPIO.setup(GPIO_REMOTE_TRIGGER, GPIO.IN)
GPIO.setup(GPIO_REMOTE_TRIGGER_SELECT1, GPIO.IN)
GPIO.setup(GPIO_REMOTE_TRIGGER_SELECT2, GPIO.IN)
GPIO.setup(GPIO_DOOR, GPIO.IN)

DoorState="Closed" # Initial state of door

def trigger(status):
    DoorStatus(status)

# Triggered if the state of the door is changed remotely
def remote_callback(GPIO_REMOTE_TRIGGER):
    print(GPIO.input(GPIO_REMOTE_TRIGGER_SELECT1))
    print(GPIO.input(GPIO_REMOTE_TRIGGER_SELECT2))
    if(GPIO.input(GPIO_REMOTE_TRIGGER_SELECT1) and GPIO.input(GPIO_REMOTE_TRIGGER_SELECT2)):
        logging.info("Door opened")
        DoorStatus("Open")
    elif(not GPIO.input(GPIO_REMOTE_TRIGGER_SELECT1) and not GPIO.input(GPIO_REMOTE_TRIGGER_SELECT2)):
        logging.info("Door closed")
        DoorStatus("Closed")
    else:
        logging.info("Door Pending")
        DoorStatus("Pending")
    
    logging.info ("Entered Interrupt")
 
GPIO.add_event_detect(GPIO_REMOTE_TRIGGER, GPIO.RISING, remote_callback)

# Triggered if door is opened or closed
# If state of door is "Open" the door will be locked again after it was opened
def door_callback(GPIO_DOOR):
    printf("Door")
    if(GPIO.input(GPIO_DOOR)):
        DoorStatus("Closed")
        Apicall("PUT", "DoorStatus", "Closed")
        logging.info("Door was opened")
    else:        
        logging.info ("Door was closed")
 
GPIO.add_event_detect(GPIO_DOOR, GPIO.BOTH, door_callback)
   
session = requests.Session()

# This function retrievs the measured distance of the distance sensor
def getdistance():
    print("Distance")

    # Set Trigger HIGH
    GPIO.output(GPIO_TRIGGER, True)
 
    # Set Trigger LOW after 0.01ms
    time.sleep(1)
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

# This function sets the color of the neopixel

def setLedColor(LEDcolor):
    led.setPixelColor(0, LEDcolor)
    led.show()


# This function is called by every function that changed the door state
# The global state and the led color are changed
def DoorStatus(newState):
    global DoorState
    DoorState=newState
    print(newState)
    if(newState=="Open"):
        setLedColor(Color(255, 0, 0)) #Green
    elif (newState=="Closed"):
        setLedColor(Color(0, 255, 0)) #Red
    else:
        setLedColor(Color(0, 0, 255)) #Blue

# This function calles the REST api of the Thingworx platform
def Apicall(httprequest, object, value=0):

    entityurl = baseurl + "Things/"+client+"/Properties/" + object    

    if httprequest == 'PUT':
        if(type(value) is str):
            requestbody = "{\"" + object + "\":\"" + value + "\"}"
        else:
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
 
    # Connection info of the Thingworx ser
    global baseurl
    baseurl = "http://"+str(sys.argv[1])+"/Thingworx/"
    global appkey
    appkey = str(sys.argv[2])
    global client
    client = str(sys.argv[3])

    print(baseurl)
    print(appkey)
    print(client)

    session.headers.update({"appKey":appkey})
    session.headers.update({"Content-Type":"application/json"})
    session.headers.update({"Accept":"application/json"})
    
    lcd.lcd_clear()
    
    if(GPIO.input(GPIO_DOOR)):
        lcd.lcd_display_string("Door is",1)
        lcd.lcd_display_string("not closed!",2)
     
    # Signal at systemstart if the door is open with blue blinking led
    while(GPIO.input(GPIO_DOOR)):
	print("Cycle")
	setLedColor(Color(0, 0, 255)) #Blue
	time.sleep(1)
	setLedColor(Color(0, 0, 0)) #Off
	time.sleep(1)
        
    DoorStatus("Closed")

    try:
        distance = 0
        while True:     
            distance = getdistance()
            logging.info ("Measured Distance = %.1f cm" % distance)
            
	    if(distance<1000):
            	Apicall("PUT", "Distance", distance)  
            	if(distance<15 or DoorState!="Closed"):
                	lcd.lcd_clear()
                	lcd.lcd_display_string("%.1f cm" % distance,1)
                	lcd.lcd_display_string("Door: "+DoorState,2)
            	elif(DoorState=="Closed"):
                	lcd.lcd_backlight("Off")
            
            time.sleep(0.1)  

    # Exit after STRG+C input
    except KeyboardInterrupt:
        logging.info("Measurement stopped by User")
        GPIO.cleanup()
