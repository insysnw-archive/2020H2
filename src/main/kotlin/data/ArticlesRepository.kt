package data

import models.*

object ArticlesRepository {

    private val jsonRepository = JsonRepository()

    private val sections: Sections = jsonRepository.readJsonSections()

    @Synchronized
    fun addArticle(article: ArticleWithSections): Result<String> {
        val subsection = sections.sections.get(article.section)
            ?: return Result.failure(Exception("Section doesn't exist"))
        val articles = subsection.subsections.get(article.subsection) ?: return Result.failure(
            Exception(
                "Subsection in this section doesn't exist"
            )
        )
        articles.add(article.article)
        return Result.success("OK")
    }

    @Synchronized
    fun getArticlesByAuthor(author: Author, section: Section): List<Article> {
        val articles =
            sections.sections[section.section]?.subsections?.get(section.subsection) ?: emptyList()
        val articlesByAuthor = articles.filter { it.author == author.author }
        return articlesByAuthor.map {
            Article(
                it.author,
                it.title,
                it.body.take(200)
            )
        }
    }

    @Synchronized
    fun getArticleByName(title: String, section: Section): Article? =
        sections.sections[section.section]?.subsections?.get(section.subsection)?.find { it.title == title }


    @Synchronized
    private fun getArticles(): List<Article> =
        sections.sections.flatMap { it.value.subsections.flatMap { it.value.map { it } } }

    @Synchronized
    fun getSubsections(sectionName: String) = sections.sections.get(sectionName)

    @Synchronized
    fun getSections() = sections

    @Synchronized
    fun containsSection(section: Section): String? {
        val subsections = sections.sections[section.section] ?: return null
        return if (subsections.subsections[section.subsection] != null) section.subsection else section.section
    }

    @Synchronized
    fun getSubsectionsList(currentSection: String): List<String> {
        return if (currentSection == "/") sections.sections.keys.toList()
        else sections.sections[currentSection]?.subsections?.keys?.toList() ?: emptyList()
    }

    @Synchronized
    fun getArticlesBySection(currentSection: String): List<Article> {
        val allSubsections = sections.sections.flatMap { it.value.subsections.entries }.associate { it.key to it.value }
        return allSubsections[currentSection]?.map { Article(it.author, it.title, it.body.take(200)) } ?: emptyList()
    }

    fun onSaveData() {
        jsonRepository.writeJson(sections)
    }
}