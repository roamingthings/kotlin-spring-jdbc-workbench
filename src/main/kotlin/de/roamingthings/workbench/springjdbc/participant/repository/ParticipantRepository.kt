package de.roamingthings.workbench.springjdbc.participant.repository


import de.roamingthings.workbench.springjdbc.participant.domain.Participant
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.util.*


interface ParticipantRepository {
    fun save(participant: Participant): Participant
    fun findAll(): List<Participant>
}

@Repository
class JdbcParticipantRepository(val jdbcTemplate: JdbcTemplate): ParticipantRepository {
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

    override fun save(participant: Participant): Participant {
        val savedParticipant: Participant?
        if (participant.uuid == null) {
            val uuid = UUID.randomUUID().toString()
            val currentTime = Date()

            savedParticipant = participant.copy(
                    uuid = uuid,
                    created = currentTime,
                    updated = currentTime)
            SimpleJdbcInsert(jdbcTemplate).withTableName("PARTICIPANT").apply {
                execute(BeanPropertySqlParameterSource(savedParticipant))
            }
        } else {
            val currentTime = Date()
            savedParticipant = participant.copy(
                    updated = currentTime)
            // TODO implement update
        }
        return savedParticipant
    }

     override fun findAll(): List<Participant> =
        jdbcTemplate.query("SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT WHERE UUID = ?") {rs, _ ->
            Participant(
                    rs.getString("uuid"),
                    rs.getDate("created"),
                    rs.getDate("updated"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("additional_names"),
                    setOf())
        }
}