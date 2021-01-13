package models

data class Article(val author: String, val title: String, val body: String)

data class ArticleWithSections(
    val section: String, val subsection: String,
    val article: Article
)