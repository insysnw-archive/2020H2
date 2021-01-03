package com.handtruth.net.lab3.nrating

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DesktopDialogProperties
import androidx.compose.ui.window.Dialog
import com.handtruth.net.lab3.nrating.types.Topic
import com.handtruth.net.lab3.nrating.types.TopicStatus
import com.handtruth.net.lab3.nrating.ui.*
import com.handtruth.net.lab3.nrating.vm.ClientViewModel
import io.ktor.network.selector.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlin.system.exitProcess

const val DEFAULT_PORT = 3433

val selector: SelectorManager =
    ActorSelectorManager(Dispatchers.IO + CoroutineName("nrating/client/selector"))

enum class ClientState(val title: String) {
    Connect("Параметры подключения"),
    Connecting("Соединение..."),
    RatingList("Список голосований"),
    AddTopic("Добавить тему голосования"),
    Loading("Загрузка..."),
    Vote("Голосование"),
    Fail("УЖАС")
}

fun main() = Window(title = "NRating", size = IntSize(600, 400)) {
    val viewModel = ClientViewModel()

    val scope = rememberCoroutineScope()// { Dispatchers.Default + CoroutineName("nrating/client/ui") + Job() }

    val state by viewModel.state.collectAsState(scope.coroutineContext)
    val hostname by viewModel.hostname.collectAsState(scope.coroutineContext)
    val port by viewModel.port.collectAsState(scope.coroutineContext)
    val fatal by viewModel.fatal.collectAsState(scope.coroutineContext)
    val topics by viewModel.topics.collectAsState(scope.coroutineContext)
    val currentTopic by viewModel.currentTopic.collectAsState(scope.coroutineContext)
    val error by viewModel.error.collectAsState(scope.coroutineContext)

    DesktopMaterialTheme {
        if (error != null) {
            val properties = DesktopDialogProperties(title = "Ошибка")
            Dialog(properties = properties, onDismissRequest = { viewModel.resetError() }) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка!", style = MaterialTheme.typography.h4)
                    Spacer(Modifier.padding(8.dp))
                    Text(error ?: "", style = MaterialTheme.typography.body1)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                ClientState.Connect -> ConnectForm(
                    hostname = hostname,
                    port = port,
                    onHostnameChange = { viewModel.setHostname(it) },
                    onPortChange = { viewModel.setPort(it) },
                    onConnect = { viewModel.connect() },
                    onClose = { exitProcess(0) }
                )
                ClientState.Connecting -> Loading("Установление подключения...")
                ClientState.RatingList -> RatingList(
                    topics,
                    onSelect = { viewModel.select(it) },
                    onRemove = { viewModel.remove(it) },
                    onUpdate = { viewModel.fetchTopicList() },
                    onAdd = { viewModel.gotoNewTopic() }
                )
                ClientState.AddTopic -> AddTopic(
                    onConfirm = { a, b -> viewModel.newTopic(a, b) },
                    onCancel = { viewModel.gotoTopicList() }
                )
                ClientState.Loading -> Loading("Загрузка...")
                ClientState.Vote -> Vote(
                    currentTopic,
                    onUpdate = { viewModel.select(it) },
                    onOpen = { viewModel.open(it) },
                    onVote = { a, b -> viewModel.vote(a, b) },
                    onClose = { viewModel.close(it) },
                    onGoBack = { viewModel.gotoTopicList() }
                )
                ClientState.Fail -> Fail(fatal, onClose = { exitProcess(1) })
            }
        }
    }
}