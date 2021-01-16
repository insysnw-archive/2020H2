package server

object Strings {
    const val SERVER_STARTED = "Сервер запущен"
    const val SERVER_NOT_STARTED = "Сервер не запущен"
    const val RESULTS_NOT_FOUND = "Результаты последнего теста не найдены"
    const val BAD_FORMAT = "Получено некорректное сообщение от клиента. Подробнее:"
    val QUESTION_WITH_OPTIONS = { question: String, options: List<String> ->
        """
            $question
            Варианты ответа: $options
        """.trimIndent()
    }
    val RESULTS = { testName: String, userAnswers: List<String>, correctAnswers: List<String>,
                    correctAnswersCount: Int, answersCount: Int ->
        """
            Последний тест:    $testName
            Ваши ответы:       $userAnswers
            Правильные ответы: $correctAnswers
            Результат:         $correctAnswersCount / $answersCount
        """.trimIndent()
    }
}