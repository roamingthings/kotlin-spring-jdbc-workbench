package de.roamingthings.workbench.springjdbc.participant.repository


import de.roamingthings.workbench.springjdbc.participant.domain.Participant
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*


interface ParticipantRepository {
    fun save(participant: Participant): Participant
    fun findAll(): List<Participant>
    fun findByUuid(uuid: String): Participant?
}

@Repository
class JdbcParticipantRepository(val jdbcTemplate: JdbcTemplate) : ParticipantRepository {
    companion object {
        @JvmField
        val QUERY_UPDATE_PARTICIPANT = """
          UPDATE PARTICIPANT
            SET
              UPDATED = :updated,
              FIRST_NAME = :firstName,
              LAST_NAME = :lastName,
              ADDITIONAL_NAMES = :additionalNames
            WHERE
              UUID = :uuid
"""
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
            jdbcTemplate.query("SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT", ParticipantRowMapper)

    override fun findByUuid(uuid: String): Participant? =
            jdbcTemplate.queryForObject("SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT WHERE UUID=?", ParticipantRowMapper, uuid)
}

object ParticipantRowMapper : RowMapper<Participant> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Participant? =
            Participant(
                    rs.getString("uuid"),
                    rs.getDate("created"),
                    rs.getDate("updated"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("additional_names"),
                    setOf())
}