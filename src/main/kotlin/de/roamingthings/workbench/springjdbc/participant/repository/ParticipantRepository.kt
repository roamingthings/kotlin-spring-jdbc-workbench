package de.roamingthings.workbench.springjdbc.participant.repository


import de.roamingthings.workbench.springjdbc.participant.domain.Participant
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import kotlin.collections.ArrayList


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

const val SQL_DELETE_SINGLE = "DELETE FROM PARTICIPANT WHERE UUID=?"
const val SQL_DELETE_ALL = "DELETE FROM PARTICIPANT"
const val SQL_COUNT = "SELECT count(*) FROM PARTICIPANT"
const val SQL_EXISTS_BY_ID = "SELECT count(*) FROM PARTICIPANT WHERE uuid=?"
const val SQL_UPDATE_SINGLE = "UPDATE PARTICIPANT SET UPDATED=?, FIRST_NAME=?, LAST_NAME=?, ADDITIONAL_NAMES=? WHERE UUID=?"
const val SQL_FIND_ALL = "SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT"
const val SQL_FIND_BY_ID = "SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT WHERE UUID=?"
const val SQL_FIND_BY_MULTIPLE_IDS = "SELECT uuid, created, updated, first_name, last_name, additional_names FROM PARTICIPANT WHERE UUID IN "

@Repository
class JdbcParticipantRepository(val jdbcTemplate: JdbcTemplate) : ParticipantRepository {

    override fun deleteById(id: String) {
        jdbcTemplate.update(SQL_DELETE_SINGLE, arrayOf(id))
    }

    override fun deleteAll(entities: MutableIterable<Participant>) {
        val entityIds = entities.mapNotNull { it.uuid }.map { arrayOf(it) }
        jdbcTemplate.batchUpdate(SQL_DELETE_SINGLE, entityIds)
    }

    override fun deleteAll() {
        jdbcTemplate.update(SQL_DELETE_ALL)
    }

    override fun count(): Long =
            jdbcTemplate.queryForObject(SQL_COUNT, Long::class.java) ?: 0

    override fun findAllById(ids: MutableIterable<String>): MutableIterable<Participant> {
        val parameterString = (1..ids.count()).map { "?" }.joinToString(",", "(", ")")
        val idArray = ArrayList<String>(ids.toMutableList()).toArray()
        return jdbcTemplate.query(SQL_FIND_BY_MULTIPLE_IDS + parameterString, this::mapToParticipant, idArray)
    }

    override fun existsById(id: String): Boolean =
            jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Long::class.java, arrayOf(id)) > 0

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
        val savedParticipant = prepareEntityToCreate(participant)
        SimpleJdbcInsert(jdbcTemplate)
                .withTableName("PARTICIPANT")
                .execute(BeanPropertySqlParameterSource(savedParticipant))

        return savedParticipant
    }

    private fun prepareEntityToCreate(participant: Participant): Participant {
        val uuid = UUID.randomUUID().toString()
        val currentTime = Date()

        return participant.copy(
                uuid = uuid,
                created = currentTime,
                updated = currentTime)
    }

    private fun updateEntity(participant: Participant): Participant {
        if (participant.uuid == null) {
            throw IllegalArgumentException("uuid may not be null")
        }
        val updatedParticipant = prepareEntityToUpdate(participant)
        jdbcTemplate.update(SQL_UPDATE_SINGLE,
                updatedParticipant.updated,
                updatedParticipant.firstName,
                updatedParticipant.lastName,
                updatedParticipant.additionalNames,
                updatedParticipant.uuid
        )
        return updatedParticipant
    }

    private fun prepareEntityToUpdate(participant: Participant): Participant {
        return participant.copy(updated = Date())
    }

    override fun saveAll(entities: MutableIterable<Participant>): MutableIterable<Participant> {
        val (entitiesToCreate, entitiesToUpdate) = entities.partition { it.uuid == null }

        return saveNewEntities(entitiesToCreate).union(updateEntities(entitiesToUpdate)).toMutableList()
    }

    private fun updateEntities(entitiesToUpdate: List<Participant>): List<Participant> {
        if (entitiesToUpdate.isEmpty()) {
            return emptyList()
        }

        val preparedEntities = entitiesToUpdate.map(this::prepareEntityToUpdate)

        jdbcTemplate.batchUpdate(SQL_UPDATE_SINGLE, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val entity = preparedEntities[i]
                ps.setObject(1, entity.updated)
                ps.setObject(2, entity.firstName)
                ps.setObject(3, entity.lastName)
                ps.setObject(4, entity.additionalNames)
                ps.setObject(5, entity.uuid)
            }

            override fun getBatchSize() =
                    preparedEntities.size
        })

        return preparedEntities
    }

    private fun saveNewEntities(entitiesToCreate: List<Participant>): List<Participant> {
        if (entitiesToCreate.isEmpty()) {
            return emptyList()
        }
        val preparedEntities = entitiesToCreate.map(this::prepareEntityToCreate)

        val batch = SqlParameterSourceUtils.createBatch(preparedEntities)
        SimpleJdbcInsert(jdbcTemplate).withTableName("PARTICIPANT").executeBatch(*batch)

        return preparedEntities
    }

    override fun findAll(): List<Participant> =
            jdbcTemplate.query(SQL_FIND_ALL, this::mapToParticipant)

    override fun findById(uuid: String): Participant? = try {
        jdbcTemplate.queryForObject(SQL_FIND_BY_ID, this::mapToParticipant, arrayOf(uuid))
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
