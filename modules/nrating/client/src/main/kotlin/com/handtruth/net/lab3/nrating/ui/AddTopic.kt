package com.handtruth.net.lab3.nrating.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HorisontalSeparator() {
    Spacer(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colors.primary))
}

@Composable
fun AddTopic(onConfirm: (String, Collection<String>) -> Unit, onCancel: () -> Unit) {
    var topicName by remember { mutableStateOf("Имя") }
    val alternatives = remember { mutableStateListOf<String>() }
    var newAlternative by remember { mutableStateOf("Альтернатива") }

    val padding = Modifier.padding(8.dp)

    Column(modifier = padding.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            topicName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Тема") },
            onValueChange = { topicName = it }
        )
        Spacer(padding)
        HorisontalSeparator()
        Spacer(padding)
        Text("Альтернативы:")
        LazyColumnForIndexed(alternatives) { index, alternative ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                Text(alternative)
                Spacer(Modifier.padding(4.dp))
                Button(onClick = { alternatives.removeAt(index) }) {
                    Text("Убрать")
                }
            }
        }
        Spacer(padding)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextField(
                newAlternative,
                label = { Text("Новая альтернатива") },
                onValueChange = { newAlternative = it }
            )
            Spacer(padding)
            Button(onClick = { alternatives.add(newAlternative) }) {
                Text("Добавить")
            }
        }
        Spacer(padding)
        HorisontalSeparator()
        Spacer(padding)
        Row {
            Button(onClick = { onConfirm(topicName, alternatives) }) {
                Text("Подтвердить")
            }
            Spacer(padding)
            Button(onClick = onCancel) {
                Text("Отменить")
            }
        }
    }
}
