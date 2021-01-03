package com.handtruth.net.lab3.nrating.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Fail(message: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ужасная оказия!", style = MaterialTheme.typography.h4)
        Spacer(Modifier.padding(8.dp))
        Text(message, style = MaterialTheme.typography.body1)
        Spacer(Modifier.padding(16.dp))
        Button(onClick = onClose) {
            Text("Принять и отпустить")
        }
    }
}
