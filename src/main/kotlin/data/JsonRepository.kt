package data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import models.Sections
import java.io.File

class JsonRepository {
    private val sectionsJsonName = "sections_json.json"
    private val sectionsJsonPath = "data/"

    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).registerModule(KotlinModule())

    fun readJsonSections(): Sections {
        val inputJson = File(sectionsJsonPath + sectionsJsonName)
        return mapper.readValue(inputJson)
    }

    fun writeJson(sections: Sections) {
        File(sectionsJsonPath + sectionsJsonName).delete()
        val sectionsJsonFile = File(sectionsJsonPath + sectionsJsonName)
        mapper.writeValue(sectionsJsonFile, sections)
    }
}