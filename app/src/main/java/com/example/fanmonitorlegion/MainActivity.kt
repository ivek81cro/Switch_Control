package com.example.fanmonitorlegion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fanmonitorlegion.ui.theme.FanMonitorLegionTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FanMonitorLegionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ESP32ControlApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESP32ControlApp(modifier: Modifier = Modifier) {
    val auth = FirebaseAuth.getInstance()
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    if (isLoggedIn) {
        DeviceControlScreen(auth) { isLoggedIn = false }
    } else {
        LoginScreen(auth) { isLoggedIn = true }
    }
}

@Composable
fun LoginScreen(auth: FirebaseAuth, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                errorMessage = task.exception?.message
                            }
                        }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Login")
            }
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlScreen(auth: FirebaseAuth, onLogout: () -> Unit) {
    val database = FirebaseDatabase.getInstance("https://fan-monitor-d0c5b-default-rtdb.europe-west1.firebasedatabase.app/")
    val switchRef = database.getReference("device/switch")
    var isOn by remember { mutableStateOf(false) }

    // Read initial state from Firebase
    LaunchedEffect(Unit) {
        switchRef.get().addOnSuccessListener { dataSnapshot ->
            isOn = dataSnapshot.getValue(Boolean::class.java) ?: false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ESP32 Controller") },
                actions = {
                    Button(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Text("Logout")
                    }
                }
            )
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isOn) "Device is ON" else "Device is OFF",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = {
                            isOn = !isOn
                            switchRef.setValue(isOn)
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(if (isOn) "Turn OFF" else "Turn ON")
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FanMonitorLegionTheme {
        ESP32ControlApp()
    }
}