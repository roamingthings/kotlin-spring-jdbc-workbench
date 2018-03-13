package de.roamingthings.workbench.springjdbc.participant.repository

import de.roamingthings.workbench.springjdbc.participant.domain.Participant
import org.assertj.core.api.JUnitSoftAssertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@DataJpaTest
@ComponentScan(basePackages = arrayOf("de.roamingthings.workbench.springjdbc.participant"))
class JdbcParticipantRepositoryIT {

    val random = Random()

    @Autowired
    lateinit var jdbcParticipantRepository: JdbcParticipantRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @get: Rule
    private val softly = JUnitSoftAssertions()

    @Test
    fun `save should persist new participant`() {
        // given
        val participant = Participant(
                firstName = "First",
                lastName = "Last",
                additionalNames = "Additional"
        )

        // when
        val savedParticipant = jdbcParticipantRepository.save(participant)

        // then
        softly.assertThat(savedParticipant).isNotNull
        softly.assertThat(savedParticipant.uuid).isNotNull
        softly.assertThat(savedParticipant.created).isNotNull
        softly.assertThat(savedParticipant.updated).isNotNull
        softly.assertThat(savedParticipant.firstName).isEqualTo("First")
        softly.assertThat(savedParticipant.lastName).isEqualTo("Last")
        softly.assertThat(savedParticipant.additionalNames).isEqualTo("Additional")
    }

    @Test
    fun `findAll should find all participants`() {
        // given
        val participant1 = aPersistedParticipant()
        val participant2 = aPersistedParticipant()

        // when
        val participants = jdbcParticipantRepository.findAll()

        // then
        softly.assertThat(participants).hasSize(2)
        softly.assertThat(participants).contains(participant1)
        softly.assertThat(participants).contains(participant2)
    }

    @Test
    fun `findByUuid should find a participant`() {
        // given
        val participant = aPersistedParticipant()

        // when
        val foundParticipant = jdbcParticipantRepository.findById(participant.uuid!!)

        // then
        softly.assertThat(foundParticipant).isNotNull
        softly.assertThat(foundParticipant).isEqualTo(participant)
    }

    @Test
    fun `save should update an existing participant`() {
        // given
        val participant = aPersistedParticipant()
        val modifiedParticipant = participant.copy(
                firstName = "Updated first",
                lastName = "Updated last",
                additionalNames = "Additional updated"
        )

        // when
        val savedParticipant = jdbcParticipantRepository.save(modifiedParticipant)

        // then
        softly.assertThat(savedParticipant).isNotNull
        softly.assertThat(savedParticipant.uuid).isEqualTo(participant.uuid)
        softly.assertThat(savedParticipant.created).isEqualTo(participant.created)
        softly.assertThat(savedParticipant.updated).isAfter(participant.updated)
        softly.assertThat(savedParticipant.firstName).isEqualTo("Updated first")
        softly.assertThat(savedParticipant.lastName).isEqualTo("Updated last")
        softly.assertThat(savedParticipant.additionalNames).isEqualTo("Additional updated")
    }

    @Test
    fun `deleteById should delete by given id`() {
        // given
        val participant = aPersistedParticipant()
        val participantUuid = participant.uuid!!

        // when
        jdbcParticipantRepository.deleteById(participantUuid)

        // then
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PARTICIPANT WHERE UUID=?", Long::class.java, arrayOf(participantUuid))
        softly.assertThat(count).isEqualTo(0)
    }

    @Test
    fun `deleteAll should only delete entities from list`() {
        // given
        val participant1 = aPersistedParticipant()
        val participant2 = aPersistedParticipant()
        val participant3 = aPersistedParticipant()
        val participantList = mutableListOf(participant1, participant2)

        // when
        jdbcParticipantRepository.deleteAll(participantList)

        // then
        softly.assertThat(jdbcParticipantRepository.existsById(participant1.uuid!!)).isFalse
        softly.assertThat(jdbcParticipantRepository.existsById(participant2.uuid!!)).isFalse
        softly.assertThat(jdbcParticipantRepository.existsById(participant3.uuid!!)).isTrue
    }

    @Test
    fun `deleteAll should delete all entities`() {
        // given
        (1 until 10).forEach {
            aPersistedParticipant()
        }

        // when
        jdbcParticipantRepository.deleteAll()

        // then
        softly.assertThat(jdbcParticipantRepository.count()).isEqualTo(0)
    }

    @Test
    fun `saveAll should save all entities`() {
        // given
        val participantList = (1 until 10).map {
            aParticipant()
        }

        // when
        jdbcParticipantRepository.saveAll(participantList.toMutableList())

        // then
        participantList
                .mapNotNull { participant ->
                    participant.uuid
                }
                .forEach {
                    softly.assertThat(jdbcParticipantRepository.existsById(it))
                }
    }

    @Test
    fun `count of empty database should return 0`() {
        // given
        // an empty database

        // when
        val count = jdbcParticipantRepository.count()

        // then
        softly.assertThat(count).isEqualTo(0)
    }

    @Test
    fun `count of database with 10 entities should return 10`() {
        // given
        (1 until 10).forEach {
            aPersistedParticipant()
        }

        // when
        val count = jdbcParticipantRepository.count()

        // then
        softly.assertThat(count).isEqualTo(10)
    }

    @Test
    fun `findAllById should find all entities`() {
        // given
        val participantIds = (1 until 10)
                .map {
                    aPersistedParticipant()
                }
                .mapNotNull {
                    it.uuid
                }

        // when
        val foundParticipants = jdbcParticipantRepository.findAllById(participantIds.toMutableList())

        // then
        softly.assertThat(foundParticipants).extracting("uuid").containsAll(participantIds.toMutableList())
    }

    @Test
    fun `existsById should return true for persited entity`() {
        // given
        val participant = aPersistedParticipant()

        // when
        val participantExists = jdbcParticipantRepository.existsById(participant.uuid!!)

        // then
        softly.assertThat(participantExists).isTrue
    }

    @Test
    fun `existsById should return false for unknown entity`() {
        // given

        // when
        val participantExists = jdbcParticipantRepository.existsById("unknown")

        // then
        softly.assertThat(participantExists).isFalse
    }

    private fun aPersistedParticipant(): Participant {
        val participant = aParticipant()

        jdbcTemplate.update(
                "INSERT INTO PARTICIPANT (uuid, created, updated, first_name, last_name, additional_names) VALUES (?,?,?,?,?,?)",
                participant.uuid,
                participant.created,
                participant.updated,
                participant.firstName,
                participant.lastName,
                participant.additionalNames
        )

        return participant
    }

    private fun aParticipant(): Participant {
        val participant = Participant(
                UUID.randomUUID().toString(),
                Date(),
                Date(),
                "First ${random.nextInt()}",
                "Last ${random.nextInt()}",
                "Additional ${random.nextInt()}",
                setOf())
        return participant
    }
}