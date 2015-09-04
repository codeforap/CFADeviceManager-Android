# CFADeviceManager-Android
Code For AP Device Manager - Android Client is an Android Device Management application. It uses MqttClient and periodically communicates with an Mqtt Server to give updates on the device location, application usage etc. Also takes the admin status on the device if allowed.

This Client is still under construction! It is purely an open source project. If you find any bugs, you can help us by raising issues in Github. You can also contribute to the project by sending pull requests:

* Fork the project on Github
* Create a topic branch for your changes
* Ensure that you provide *documentation* and *test coverage* for your changes (patches won't be accepted without these)
* Ensure that all tests pass (`./gradlew clean test`)
* Create a pull request on Github 

If you simply want to run the application and see how it works, you will have to:

* Clone the project on your computer
* Open it using Android Studio
* Go to SendToServer class and make sure you put in your server's ip address into the string "Server" of that class (without this, the app cannot send updates to your Mqtt Server)
* Run the app
* Test Login is username: "admin", password: "cfap"

