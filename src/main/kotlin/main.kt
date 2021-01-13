import data.ArticlesRepository
import models.*
import network.*

fun main() {
    saveDataOnExit()
    val routing = routing {
        var currentSection = "/"
        val sectionsHistory = ArrayDeque<String>()
        fun printCurrentState() {
            println("Current section: $currentSection")
            println("Previous sections: $sectionsHistory\n")
        }
        get("/current-section") {
            val currentSectionsList = ArticlesRepository.getSubsectionsList(currentSection)
            call.respondObject(currentSectionsList)
        }
        get("/current-articles") {
            val articles = ArticlesRepository.getArticlesBySection(currentSection)
            call.respondObject(articles)
        }
        post("/open-section") {
            val section = call.receive<Section>()
            ArticlesRepository.containsSection(section)?.let {
                sectionsHistory.addLast(currentSection)
                currentSection = it
                call.respond(Message(Header.ok()))
                printCurrentState()
            } ?: call.respondError(Message(Header.badRequest("section doesn't exist")))
        }
        get("/previous-section") {
            val prev = sectionsHistory.removeLast()
            currentSection = prev
            call.respond(Message(Header.ok()))
            printCurrentState()
        }
        post("/get-article-by-name") {
            val title = call.receive<Title>()
            ArticlesRepository.getArticleByName(
                title.title,
                Section(sectionsHistory.lastOrNull() ?: "", currentSection)
            )?.let {
                call.respondObject(it)
            } ?: call.respondError(Message(Header.notFound()))
        }
        post("/get-articles-by-author") {
            val author = call.receive<Author>()
            val articles = ArticlesRepository.getArticlesByAuthor(
                author,
                Section(sectionsHistory.lastOrNull() ?: "", currentSection)
            )
            if (articles.isEmpty()) call.respondError(Message(Header.notFound()))
            else call.respondObject(articles)
        }
        post("/add-article") {
            val newArticle = call.receive<ArticleWithSections>()
            val result = ArticlesRepository.addArticle(newArticle)
            when {
                result.isSuccess -> call.respond(Message(Header.ok()))
                result.isFailure -> call.respondText("400 ${result.exceptionOrNull().toString()}")
            }
            ArticlesRepository.onSaveData()
        }
    }
    Server(routing)

}

fun saveDataOnExit() {
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            ArticlesRepository.onSaveData()
        }
    })
}