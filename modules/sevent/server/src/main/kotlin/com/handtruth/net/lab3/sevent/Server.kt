package com.handtruth.net.lab3.sevent

import com.handtruth.kommon.Log
import com.handtruth.kommon.default
import com.handtruth.kommon.getLog
import com.handtruth.net.lab3.util.forever
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.*
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.seconds

private val log = Log.default("sevent/server")

/**
 * Главный обработчик системных событий ввода вывода. Надо думать, что эта чтука управляет
 * асинхронными каналами и новыми подключениями через события на дескрипторах, полученные с помощью
 * вызова функций мультиплексоров событий (select, poll и epoll). Всегда блокирует 1 поток при
 * ожидании следующего события, поэтому в контексте должен присутствовать диспатчер корутин,
 * привязанный к этому потоку, либо Dispatchers.IO, который состоит из пула потоков как раз для таких операций.
 *
 * Это свойство используется при создании асинхронного сокета (только такие и есть в ktor).
 */
val selector: SelectorManager =
    ActorSelectorManager(Dispatchers.IO + CoroutineName("sevent/server/selector"))

/**
 * Некоторые объекты логики сервера, которые нужны на разных уровнях дерева задач сервера.
 * Этот объект лежит в контексте корутин [coroutineContext], чтобы быть доступным отовсюду без
 * дополнительных аргументов у функции подзадачи.
 *
 * @property idGenerator генератор id для новых событий
 * @property repository репозиторий событий сервера
 * @property minimalPeriod минимальный период, который может использовать клиент при создании нового события
 */
data class ServerContext(
    val idGenerator: IdGenerator,
    val repository: Repository,
    val minimalPeriod: Duration
) : CoroutineContext.Element {

    override val key get() = Key

    companion object Key : CoroutineContext.Key<ServerContext>
}

/**
 * Достать объекты логики сервера из контекста корутин [coroutineContext].
 * Данная функция в случае отсутствия контекста попробует записать в лог, который привязан к контексту.
 *
 * @return объекты логики сервера
 */
suspend inline fun getServerContext() = coroutineContext[ServerContext] ?: getLog().fatal { "no server context" }

/**
 * Входная точка приложения, прописана в build.gradle.kts. В новых версиях Kotlin может быть прерывной.
 *
 * @param args аргументы
 */
suspend fun main(args: Array<String>): Unit = coroutineScope {
    // Котлиновский способ парсинга аргументов
    val argsParser = ArgParser("sevent-server")
    val host by argsParser.option(
        ArgType.String,
        shortName = "a",
        description = "server hostname for binding"
    ).default("localhost")
    val port by argsParser.option(
        ArgType.Int,
        shortName = "p",
        description = "server port"
    ).default(DEFAULT_PORT)
    val state by argsParser.option(
        ArgType.String,
        shortName = "s",
        description = "state file path"
    ).default("state.json")
    val minPeriod by argsParser.option(
        ArgType.Double,
        shortName = "P",
        description = "minimum period value allowed"
    ).default(1.0)
    argsParser.parse(args)

    val stateFile = File(state)
    val previousState = State.load(stateFile)
    val idGenerator = IdGenerator(previousState.nextId)
    val repository = Repository(coroutineContext + Dispatchers.Default, previousState.events)
    // Вот и серверный сокет, подключённый к главному селектору приложения
    val serverSocket = aSocket(selector).tcp().bind(host, port)
    val superJob = coroutineContext[Job]!!
    // Этот поток будет запущен при попытке выключить сервер, например с помощью SIGTERM,
    // по окончанию работы этого потока сервер будет остановлен.
    val onExit = thread(start = false, name = "shutdown") {
        runBlocking {
            log.info { "stopping server..." }
            // Остановим superJob (коренную задачу) при выключении сервера, таким образом всё дерево
            // задач будет остановлено, это гарантирует то, что все клиенты будут отсоединены и все
            // обновления событий в репозитории остановлены.
            superJob.cancelAndJoin()
            log.info { "server stopped, saving state..." }
            val newState = State(idGenerator.next(), repository.events.toList())
            newState.save(stateFile)
        }
        log.info { "exited successfully!" }
    }
    Runtime.getRuntime().addShutdownHook(onExit)

    // Создаём контекст сервера и запускаем сервер
    val serverContext = ServerContext(idGenerator, repository, minPeriod.seconds)
    withContext(serverContext) {
        server(serverSocket)
    }
}

/**
 * Главный подпроцесс сервера. Данная функция реализует бесконечный цикл и завершается
 * только в случае остановки сервера через отмену операции или в результате ошибки.
 *
 * @param serverSocket серверный сокет, на котором следует прослушивать подключения
 */
suspend fun server(serverSocket: ServerSocket): Nothing = coroutineScope {
    log.info { "server socket started on ${serverSocket.localAddress}" }
    forever {
        // На каждое новое подключение создаём новую корутину для сессии клиента в общем пуле потоков.
        val clientSocket = serverSocket.accept()
        launch(Dispatchers.Default) {
            clientSocket.use {
                val readChannel = clientSocket.openReadChannel()
                val writeChannel = clientSocket.openWriteChannel()
                val endpoint = clientSocket.remoteAddress.toString()
                connection(readChannel, writeChannel, endpoint)
            }
        }
    }
}
