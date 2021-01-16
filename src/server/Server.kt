package server

import common.*
import com.beust.klaxon.Klaxon
import server.MessageTypes.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

class Server(addr: String, port: Int) {
    private val usersResults = mutableMapOf<SocketChannel, UserResults>()
    private val testsFile = File("tests.json")
    private val resultsFile = File("results.json")

    init {
        val selector = Selector.open()
        val serverSocket = try {
            ServerSocketChannel.open().apply {
                bind(InetSocketAddress(addr, port))
                configureBlocking(false)
                register(selector, SelectionKey.OP_ACCEPT)
            }
        } catch (e: IOException) {
            System.err.println(Strings.SERVER_NOT_STARTED)
            exitProcess(-1)
        }
        println(Strings.SERVER_STARTED)

        while (true) {
            selector.select()
            val selectedKeys = selector.selectedKeys()
            val iter = selectedKeys.iterator()
            while (iter.hasNext()) {
                val key = iter.next()
                if (key.isAcceptable) {
                    register(selector, serverSocket)
                }
                if (key.isReadable) {
                    try {
                        readAndSendResponse(key)
                    }
                    catch (e: IllegalStateException) {
                        println(Strings.BAD_FORMAT)
                        e.printStackTrace()
                    }
                }
                iter.remove()
            }
        }
    }

    @Throws(IOException::class)
    private fun readAndSendResponse(key: SelectionKey) {
        val socket: SocketChannel = key.channel() as SocketChannel
        val tests = Klaxon().parseArray<Test>(testsFile.readText(StandardCharsets.UTF_8)) ?: return

        val received = try {
            socket.readMessage(values(), print = true)
        } catch (e: Exception) {
            if (e is IllegalStateException || e is IOException)
                Message(DisconnectionRequest)
            else
                throw e
        }
        val userResults = usersResults[socket]

        when (received.type) {
            ConnectionRequest -> {
                check(received is TextMessage)
                val username = received.text
                if (usersResults.values.find { it.username == username } != null) {
                    socket.writeMessage(Message(UserAlreadyConnected), print = true)
                    return
                }

                socket.writeMessage(Message(ConnectionResponse), print = true)

                val recordedResults = getResultsByUsername(username)
                if (recordedResults != null) {
                    usersResults[socket] = recordedResults
                } else {
                    usersResults[socket] = UserResults(username)
                }
            }
            DisconnectionRequest -> {
                usersResults.remove(socket)
                if (userResults != null)
                    setResultsForUsername(userResults)
                return
            }

            LastResultsRequest -> {
                check(userResults != null)
                socket.writeMessage(TextMessage(LastResultsResponse, getResults(tests, userResults)), print = true)
            }
            TestsListRequest -> {
                socket.writeMessage(
                        TextMessage(
                                TestsListResponse,
                                tests.mapIndexed { i, test -> "${i + 1}) ${test.name}" }.joinToString("\n")
                        ),
                        print = true
                )
            }
            TestRequest -> {
                check(userResults != null && received is TextMessage)

                val receivedNumber = received.text.toIntOrNull()
                if (receivedNumber == null || receivedNumber - 1 !in tests.indices) {
                    socket.writeMessage(Message(BadTestNumber), print = true)
                    return
                }
                val testNumber = receivedNumber - 1
                if (tests[testNumber].questions.isNotEmpty()) {
                    val currentTest = tests[testNumber]
                    socket.writeMessage(TextMessage(Question, currentTest.questions[0].toString()), print = true)
                    userResults.apply {
                        lastTest = testNumber
                        answers = mutableListOf()
                    }
                }
            }
            Answer -> {
                check(userResults != null && received is TextMessage)

                val lastTest = userResults.lastTest
                val previousAnswers = userResults.answers
                check(lastTest != null && previousAnswers != null)

                val answer = received.text

                previousAnswers.add(answer)

                val currentTest = tests[lastTest]

                if (previousAnswers.size == currentTest.questions.size) {
                    setResultsForUsername(userResults)
                    socket.writeMessage(TextMessage(LastResultsResponse, getResults(tests, userResults)), print = true)
                } else {
                    socket.writeMessage(TextMessage(Question, currentTest.questions[previousAnswers.size].toString()), print = true)
                }
            }
            else -> throw IllegalStateException()
        }
    }

    @Throws(IOException::class)
    private fun register(selector: Selector, serverSocket: ServerSocketChannel) {
        val socket = serverSocket.accept()
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_READ)
    }

    private fun getResultsByUsername(username: String): UserResults? {
        if (!resultsFile.exists())
            return null
        return Klaxon().parseArray<UserResults>(resultsFile.readText(StandardCharsets.UTF_8))?.find {
            it.username == username
        }
    }

    private fun setResultsForUsername(results: UserResults) {
        var savedResults: MutableList<UserResults>? = null

        if (resultsFile.exists()) {
            savedResults = Klaxon().parseArray<UserResults>(resultsFile.readText(StandardCharsets.UTF_8))?.toMutableList()

            if (savedResults != null) {
                var found = false
                savedResults.forEach {
                    if (it.username == results.username) {
                        found = true
                        it.lastTest = results.lastTest
                        it.answers = results.answers
                    }
                }
                if (!found)
                    savedResults.add(results)
            }
        }

        if (savedResults == null)
            savedResults = mutableListOf(results)

        resultsFile.writeText(Klaxon().toJsonString(savedResults))
    }

    private fun getResults(tests: List<Test>, userResults: UserResults): String {
        val lastLestNumber = userResults.lastTest
        val lastTestAnswers = userResults.answers

        return if (lastLestNumber != null && lastTestAnswers != null) {
            val lastTest = tests[lastLestNumber]
            val correctAnswersCount = lastTestAnswers
                    .filterIndexed { i, answer -> answer == lastTest.questions[i].answer }
                    .count()

            Strings.RESULTS(lastTest.name, lastTestAnswers, lastTest.questions.map { it.answer },
                    correctAnswersCount, lastTest.questions.size)
        } else
            Strings.RESULTS_NOT_FOUND

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            when {
                args.size >= 2 -> Server(args[0], args[1].toIntOrNull() ?: DEFAULT_PORT)
                args.size == 1 -> Server(args[0], DEFAULT_PORT)
                else -> Server(DEFAULT_ADDRESS, DEFAULT_PORT)
            }
        }
    }
}