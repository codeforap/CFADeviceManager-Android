# Features

This Android App must have following minimal functions:

* Send the Location Co-ordinates of the device to the server for every 30 min and whenever server requested.
* Device Online Availabilty Details.
* Function for Device Info like Model Name, IMEI, Battery Charge available etc.
* Apps installed and Data Usage.

## Device Details
* Device Model
* Device IMEI Number
* Device Battery Status
* Device Name/Email
* OS Version

## Location

* We use Google Location Services to get `Last Location` of the device.
* We push this location details (Latitude, Longitude and time) to the server for every 30 minutes.
* We push the location data to the server, when the device is online.
* We are getting the Accuracy through `Get Accuracy` to understand the accuracy of the Location.

## Usage
* Storage Details and Usage
* Installed Apps and their usage, running status, last known-opened etc.
* Internet-Data Usage and classification by apps and (wifi/mobile data)
