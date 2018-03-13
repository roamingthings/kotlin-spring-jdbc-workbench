package de.roamingthings.workbench.springjdbc.participant.repository


import de.roamingthings.workbench.springjdbc.participant.domain.Participant
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*


interface ParticipantRepository {
    fun deleteById(id: String)
    fun deleteAll(entities: MutableIterable<Participant>)
    fun deleteAll()
    fun saveAll(entities: MutableIterable<Participant>): MutableIterable<Participant>
    fun count(): Long
    fun findAllById(ids: MutableIterable<String>): MutableIterable<Participant>
    fun existsById(id: String): Boolean
    fun delete(participant: Participant)
    fun save(participant: Participant): Participant
    fun findAll(): List<Participant>
    fun findById(uuid: String): Participant?
}

@Repository
class JdbcParticipantRepository(val jdbcTemplate: JdbcTemplate) : ParticipantRepository {

    override fun deleteById(id: String) {
        jdbcTemplate.update("DELETE FROM PARTICIPANT WHERE UUID=?", arrayOf(id))
    }

    override fun deleteAll(entities: MutableIterable<Participant>) {
        entities.forEach(this::delete)
    }

    override fun deleteAll() {
        jdbcTemplate.update("DELETE FROM PARTICIPANT")
    }

    override fun saveAll(entities: MutableIterable<Participant>): MutableIterable<Participant> =
            entities.map(this::save).toMutableList()

    override fun count(): Long =
            jdbcTemplate.queryForObject("SELECT count(*) FROM PARTICIPANT", Long::class.java) ?: 0

    override fun findAllById(ids: MutableIterable<String>): MutableIterable<Participant> =
            ids.mapNotNull(this::findById).toMutableList()

    override fun existsById(id: String): Boolean =
            jdbcTemplate.queryForObject("SELECT count(*) FROM PARTICIPANT WHERE uuid=?", Long::class.java, arrayOf(id)) > 0

    override fun delete(participant: Participant) {
        when {
            participant.uuid != null -> deleteById(participant.uuid)
            else -> throw IllegalArgumentException("Participant to delete does not have an id")
        }
    }

    override fun save(participant: Participant): Participant =
            when {
                participant.uuid == null -> saveNewEntity(participant)
                else -> updateEntity(participant)
            }

    private fun saveNewEntity(participant: Participant): Participant {
        val uuid = UUID.randomUUID().toString()
        val currentTime = Date()

        val savedParticipant = participant.copy(
                uuid = uuid,
                created = currentTime,
                updated = currentTime)
        SimpleJdbcInsert(jdbcTemplate).withTableName("PARTICIPANT").apply {
            execute(BeanPropertySqlParameterSource(savedParticipant))
        }

        return savedParticipant
    }

    private fun updateEntity(participant: Participant): Participant {
        if (participant.uuid == null) {
            throw IllegalArgumentException("uuid may not be null")
        }
        val updatedParticipant = participant.copy(updated = Date())
        jdbcTemplate.update("UPDATE PARTICIPANT SET UPDATED=?, FIRST_NAME=?, LAST_NAME=?, ADDITIONAL_NAMES=? WHERE UUID=?",
                updatedParticipant.updated,
                updatedParticipant.firstName,
                updatedParticipant.lastName,
                updatedParticipant.additionalNames,
                updatedParticipant.uuid
        )
        return updatedParticipant
    }

    override fun findAll(): List<Participant> =
            jdbcTemplate.query("SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT", this::mapToParticipant)

    override fun findById(uuid: String): Participant? = try {
        jdbcTemplate.queryForObject("SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT WHERE UUID=?", this::mapToParticipant, arrayOf(uuid))
    } catch (e: EmptyResultDataAccessException) {
        null
    }

    private fun mapToParticipant(rs: ResultSet, rowNum: Int) = Participant(
            uuid = rs.getString("uuid"),
            created = rs.getDate("created"),
            updated = rs.getDate("updated"),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            additionalNames = rs.getString("additional_names")
    )
}
