package com.example.demistifier

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject


//Class setup for the entire MQTT retrieval/reading/publishing process. I had no issues connecting to the SSL server on the kotlin/android side,
//but the umqtt simple and robust libraries were a nightmare to setup. I tried going the certificate route (exporting certificate, loading to RBP pico), but
//that didn't let it connect. Eventually had to include both simple/robust python libraries and call it from there, which DO support the SSL = True parameter
//I realized i took the "ssl://address:8883" field here for granted. That took ~3 full hours to debug.
class MqttHelper(context: Context) {
    //sets up the MQTT connection using the HiveMQ Cloud broker
    private val brokerUrl = "ssl://18a8caf27c6b40449526c0491ea5c727.s1.eu.hivemq.cloud:8883"
    private val clientId = "AndroidClient"
    private val username = "phone1"
    private val password = "Q11plebada" //shortened spanish for "Que once, plebada?" slang for "What eleven, peeps?" I saw it on a movie about monterrey singers

    //setting up the MQTT client, listener for connection, messages
    private var mqttClient: MqttClient? = null
    private var messageListener: ((Double, Double, Double) -> Unit)? = null
    private var connectionListener: ((Boolean) -> Unit)? = null
//when message is received, it will call the listener function. Similarly, the connection function is called for the actual connection. Not sure if this
    //is what was causing the initial crashes on launch, since it was reading "in the middle" of the MQTT server being active?
    fun setOnMessageReceived(listener: (Double, Double, Double) -> Unit) {
        messageListener = listener
    }

    fun setOnConnectionChanged(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    //connecting to the server, subscribing to the topic, and logging the results
    fun connect() {
        try {
            val persistence = MemoryPersistence() //keeps track of the state of the connection
            mqttClient = MqttClient(brokerUrl, clientId, persistence)
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                userName = username
                password = this@MqttHelper.password.toCharArray()
            }

            //reads/checks for sensor/data on the server, as pushed by the Raspberry Pi Pico 2 W
            mqttClient?.setCallback(object : MqttCallback {
                override fun messageArrived(topic: String, message: MqttMessage) {
                    if (topic == "sensor/data") { //if the text string matches this, it will read it as a JSON object, and convert it to string
                        try {
                            val json = JSONObject(message.toString()) //reads JSON object, uses .toString method
                            val temperature = json.getDouble("temperature") //temperature
                            val pressure = json.getDouble("pressure") //pressure
                            val humidity = json.getDouble("humidity") //humidity
                            messageListener?.invoke(temperature, pressure, humidity)
                        } catch (e: Exception) {
                            Log.e("MQTT", "Error parsing JSON: ${e.message}")
                            //error for invalid JSON, message
                        }
                    }
                }
//error for lost connection, reconnect, and logging
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Connection lost: ${cause?.message}")
                    connectionListener?.invoke(false)
                }
//logs message delivery
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Message delivered")
                }
            })
//subscribing to sensor/data topic, otherwise throws an error
            mqttClient?.connect(options)
            mqttClient?.subscribe("sensor/data")
            Log.d("MQTT", "Connected successfully")
            connectionListener?.invoke(true)
        } catch (e: MqttException) {
            Log.e("MQTT", "Error connecting: ${e.message}")
        }
    }
//disconnect function for button, throws error because I didnt want the app to keep freezing after opening. Logcat output was full of errors at the beginning
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d("MQTT", "Disconnected successfully")
            connectionListener?.invoke(false)
        } catch (e: MqttException) {
            Log.e("MQTT", "Error disconnecting: ${e.message}")
        }
    }

    //publish topic for LED status/toggle switch
    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient?.publish(topic, mqttMessage)
            Log.d("MQTT", "Message published to topic: $topic")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error publishing message: ${e.message}")
        }
    }
}
