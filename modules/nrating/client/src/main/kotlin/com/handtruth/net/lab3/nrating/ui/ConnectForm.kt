package com.handtruth.net.lab3.nrating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConnectForm(
    hostname: String, port: Int,
    onHostnameChange: (String) -> Unit, onPortChange: (Int) -> Unit, onConnect: () -> Unit, onClose: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        val padding = Modifier.padding(8.dp)
        Surface(border = BorderStroke(1.dp, Color.Gray), modifier = padding) {
            TextField(
                value = hostname,
                singleLine = true,
                onValueChange = onHostnameChange,
                label = { Text("Имя хоста") }
            )
        }
        Surface(border = BorderStroke(1.dp, Color.Gray), modifier = padding) {
            TextField(
                value = port.toString(),
                singleLine = true,
                onValueChange = { onPortChange(it.toIntOrNull() ?: port) },
                label = { Text("Номер порта") }
            )
        }
        Spacer(padding)
        Row(horizontalArrangement = Arrangement.Center) {
            Button(onClick = onConnect) {
                Text("Подключиться")
            }
            Spacer(padding)
            Button(onClick = onClose) {
                Text("Отмена")
            }
        }
    }
}
