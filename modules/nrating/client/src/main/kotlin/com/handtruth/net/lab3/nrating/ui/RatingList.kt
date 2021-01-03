package com.handtruth.net.lab3.nrating.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.handtruth.net.lab3.nrating.types.Topic

@Composable
fun RatingList(
    topics: List<Topic>,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: () -> Unit,
    onAdd: () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        val padding = Modifier.padding(8.dp)
        Column(modifier = padding, horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onUpdate) {
                Text("Обновить")
            }
            Spacer(padding)
            Button(onClick = onAdd) {
                Text("Создать")
            }
        }
        Spacer(padding)
        Spacer(Modifier.fillMaxHeight().width(4.dp).background(MaterialTheme.colors.primary))
        Spacer(padding)
        LazyColumnFor(topics, modifier = Modifier.fillMaxSize().padding(8.dp), contentPadding = PaddingValues(4.dp)) { item ->
            TopicCard(item, onSelect = { onSelect(item.id) }, onRemove = { onRemove(item.id) })
        }
    }
}

@Composable
fun TopicCard(topic: Topic, onSelect: () -> Unit, onRemove: () -> Unit) {
    val modifiers = Modifier/*.size(50.dp, 25.dp)*/
        .background(Color(0xEF, 0xEF, 0xEF))
        .border(BorderStroke(5.dp, MaterialTheme.colors.secondary), MaterialTheme.shapes.small)
        .padding(8.dp)
        .clickable(onClick = onSelect)
    Row(modifier = modifiers, verticalAlignment = Alignment.CenterVertically) {
        Text(topic.name, style = MaterialTheme.typography.h6)
        Spacer(Modifier.padding(4.dp))
        Button(onClick = onSelect) {
            Text("Показать")
        }
        Spacer(Modifier.padding(4.dp))
        Button(onClick = onRemove) {
            Text("Удалить")
        }
    }
}
