package client

import client.MessageTypes.*
import common.*
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import kotlin.system.exitProcess

class Client(addr: String, port: Int) {
    private var socketChannel: SocketChannel

    init {
        try {
            socketChannel = SocketChannel.open(InetSocketAddress(addr, port))
        } catch (e: IOException) {
            System.err.println(Strings.SOCKET_NOT_CREATED)
            exitProcess(-1)
        }

        try {
            socketChannel.writeMessage(Message(ConnectionRequest))
            readThread().start()
            writeThread().start()
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    private fun shutdown(status: Status) {
        try {
            socketChannel.close()
        } catch (e: IOException) {
        }
        println(status.message)
        exitProcess(status.code)
    }

    private fun readThread() = Thread {
        try {
            while (true) {
                val received = try {
                    socketChannel.readMessage(values())
                } catch (e: Exception) {
                    if (e is IllegalStateException || e is IOException) {
                        shutdown(Status.EXCEPTION)
                        return@Thread
                    } else
                        throw e
                }

                println(when (received.type) {
                    CurrencyAlreadyExist -> Strings.CURRENCY_ALREADY_EXIST
                    CurrencyNotExist -> Strings.CURRENCY_NOT_EXIST

                    ConnectionResponse -> Strings.CONNECTION_SUCCESSFUL
                    AddCurrencyResponse -> Strings.ADDED_SUCCESSFUL
                    RemoveCurrencyResponse -> Strings.REMOVED_SUCCESSFUL
                    AddRateResponse -> Strings.RATE_ADDED_SUCCESSFUL

                    CurrenciesListResponse, HistoryResponse -> {
                        check(received is TextMessage)
                        received.text
                    }

                    else -> break
                })
            }
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }

    enum class Functions(val symbol: String) {
        Add("+"),
        Remove("-"),
        List("*"),
        SetRate("~"),
        History("?"),
        Quit("!")
    }

    private fun writeThread() = Thread {
        try {
            while (true) {
                val userInput = readLine()!!
                val words = userInput.split(" ")

                val checkParamsNumber = { number: Int, block: () -> Unit ->
                    if (words.size < number)
                        println(Strings.TOO_FEW_PARAMS(number - 1))
                    else
                        block()
                }
                when (Functions.values().find { it.symbol == words[0] }) {
                    Functions.Add -> {
                        checkParamsNumber(2) {
                            socketChannel.writeMessage(TextMessage(AddCurrencyRequest, words[1]))
                        }
                    }
                    Functions.Remove -> {
                        checkParamsNumber(2) {
                            socketChannel.writeMessage(TextMessage(RemoveCurrencyRequest, words[1]))
                        }
                    }
                    Functions.List -> {
                        socketChannel.writeMessage(Message(CurrenciesListRequest))
                    }
                    Functions.SetRate -> {
                        checkParamsNumber(3) {
                            val rate = words[2].toIntOrNull()
                            if (rate != null && rate >= 0)
                                socketChannel.writeMessage(CurrencyData(AddRateRequest, words[1], rate.toString()))
                            else
                                println("Курс валюты должен быть неотрицательным числом")
                        }
                    }
                    Functions.History -> {
                        checkParamsNumber(2) {
                            socketChannel.writeMessage(TextMessage(HistoryRequest, words[1]))
                        }
                    }
                    Functions.Quit -> {
                        socketChannel.writeMessage(Message(DisconnectionRequest))
                        shutdown(Status.OK)
                        break
                    }
                    else -> println(Strings.UNKNOWN_COMMAND)
                }
            }
        } catch (e: IOException) {
            shutdown(Status.EXCEPTION)
        }
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            when {
                args.size >= 2 -> Client(args[0], args[1].toIntOrNull() ?: DEFAULT_PORT)
                args.size == 1 -> Client(args[0], DEFAULT_PORT)
                else -> Client(DEFAULT_ADDRESS, DEFAULT_PORT)
            }
        }
    }
}