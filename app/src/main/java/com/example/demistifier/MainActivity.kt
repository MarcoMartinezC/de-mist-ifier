package com.example.demistifier



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.demistifier.ui.theme.DemistifierTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemistifierTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF80DEEA)) {  //cyan background
                    WeatherDataScreen()
                }
            }
        }
    }
}

//Composable for the entire screen, calling from the MQTTHelper class.
@Composable
fun WeatherDataScreen() {
    val context = LocalContext.current
    val mqttHelper = remember { MqttHelper(context) }
    //var type used for placeholder instantiations of the data. This approach ended up being easier than implementing a data class for the JSON data. Here, the converted
    //string types are sent to the mutableStateOf string/boolean var-types, which are then populated later on
    var temperature by remember { mutableStateOf("Not Reading ") }
    var pressure by remember { mutableStateOf("No Reading ") }
    var humidity by remember { mutableStateOf("No Reading") }
    var isConnected by remember { mutableStateOf(false) }
    var ledStatus by remember { mutableStateOf(false) }

    //connects to MQTT on app launch

    /*At first, the app would launch, then crash immediately. It later changed to launching, but stalling indefinitely. I originally thought this was because the connection
    * on the MQTT server was not established "fast enough", within a few seconds, but it was not the case. It wasn't until I implemented a non data class that it actually was able
    * launch and connect to the server. HOWEVER, there is still a very noticeable and unpleasant hang up when the connection is first established on app launch, as well as when
    * the connect/disconnect functions are called. It is very noticeable on the emulator, as the pop up message comes up alerting of the stall, but not as noticeable on a
    * samsung galaxy S8, where the app appears to lag due to the connection. This is not the case, it is the app itself. Various frame skips can still be seen in Logcat. I was not
    * able to track the root cause of this. I was content with it working at all after the initial crashes and hang ups.
    * */
    LaunchedEffect(Unit) {
        mqttHelper.connect()
    }

    // Listen for MQTT messages
    LaunchedEffect(mqttHelper) {
        mqttHelper.setOnMessageReceived { temp, press, hum ->
            temperature = String.format("%.2f", temp)
            pressure = String.format("%.2f", press)
            humidity = String.format("%.2f", hum)
        }

        //listen for connection status changes
        mqttHelper.setOnConnectionChanged { isConnectedStatus ->
            isConnected = isConnectedStatus
        }
    }

    //listen for LED status toggle
    LaunchedEffect(ledStatus) {
        mqttHelper.publish("sensor/led", if (ledStatus) "on" else "off")
    }

    Column(modifier = Modifier.padding(16.dp)) {
        //App title. I wanted it to look similar to "Two Plus Two Machine"
        Text(
            text = "De-mist-ifer", //My wife came up with the name
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        //Temperature section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Thermostat, contentDescription = "Temperature", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Temperature: $temperature°C", style = MaterialTheme.typography.titleMedium)
        }

        //Pressure section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Assessment, contentDescription = "Pressure", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pressure: $pressure hPa", style = MaterialTheme.typography.titleMedium)
        }

        //Humidity section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Cloud, contentDescription = "Humidity", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Humidity: $humidity%", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        //LED Toggle Switch
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lightbulb, contentDescription = "LED", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("LED Control", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = ledStatus, onCheckedChange = { ledStatus = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        //connect/disconnect buttons
        Button(onClick = { mqttHelper.connect() }) {
            Text("Connect")
        }

        Button(onClick = { mqttHelper.disconnect() }) {
            Text("Disconnect")
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Display HiveMQ Cloud MQTT server status with icon and colored text
        //This has a slight lag as the app loads.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isConnected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Connected", tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "MQTT Server Status: Connected",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Icon(Icons.Filled.Error, contentDescription = "Disconnected", tint = MaterialTheme.colorScheme.error)
                Text(
                    text = "MQTT Server Status: Disconnected",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

//preview, but again, not really used since i was loading/reloading the app every time i would make changes
@Preview(showBackground = true)
@Composable
fun PreviewWeatherDataScreen() {
    WeatherDataScreen()
}