package server


data class Test(val name: String, val questions: List<Question>) {
    data class Question(val question: String, val options: List<String>? = null, val answer: String) {
        override fun toString(): String {
            return if (options != null)
                Strings.QUESTION_WITH_OPTIONS(question, options)
            else
                question
        }
    }
}

data class UserResults(val username: String, var lastTest: Int? = null, var answers: MutableList<String>? = null)