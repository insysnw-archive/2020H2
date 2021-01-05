package com.handtruth.net.lab3.nrating.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.handtruth.net.lab3.nrating.types.TopicStatus

@Composable
fun Vote(
    topic: TopicStatus,
    onUpdate: (Int) -> Unit,
    onOpen: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onVote: (Int, Int) -> Unit,
    onGoBack: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        val padding = Modifier.padding(8.dp)
        Column(modifier = padding, horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { onUpdate(topic.topicData.id) }) {
                Text("Обновить")
            }
            Spacer(padding)
            Button(onClick = { onOpen(topic.topicData.id) }) {
                Text("Открыть")
            }
            Spacer(padding)
            Button(onClick = { onClose(topic.topicData.id) }) {
                Text("Закрыть")
            }
            Spacer(padding)
            Button(onClick = { onGoBack() }) {
                Text("Назад")
            }
        }
        Spacer(padding)
        Spacer(Modifier.fillMaxHeight().width(4.dp).background(MaterialTheme.colors.primary))
        Spacer(padding)
        Column(modifier = Modifier.fillMaxHeight()) {
            Text("Тема: ${topic.topicData.name}", style = MaterialTheme.typography.h4)
            Text(
                "Состояние: ${if (topic.isOpen) "открыто" else "закрыто"}",
                style = MaterialTheme.typography.subtitle2
            )
            Spacer(padding)
            Text("Альтернативы:", style = MaterialTheme.typography.h5)
            LazyColumnFor(
                topic.rating,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${item.altName}: ${item.votesAbs}, ${item.votesRel * 100}%",
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(padding)
                    Button(onClick = { onVote(topic.topicData.id, item.altId) }) {
                        Text("Голосовать")
                    }
                }
            }
        }
    }
}
