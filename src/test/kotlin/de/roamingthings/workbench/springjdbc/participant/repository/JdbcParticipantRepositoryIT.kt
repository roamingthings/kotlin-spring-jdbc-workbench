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

    @Autowired
    lateinit var jdbcParticipantRepository: JdbcParticipantRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @get: Rule
    private val softly = JUnitSoftAssertions()

    @Test
    fun `save_should_persist_new_participant`() {
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
    fun `findAll_should_find_all_participants`() {
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
    fun `findByUuid_should_find_a_participant`() {
        // given
        val participant = aPersistedParticipant()

        // when
        val foundParticipant = jdbcParticipantRepository.findByUuid(participant.uuid!!)

        // then
        softly.assertThat(foundParticipant).isNotNull
        softly.assertThat(foundParticipant).isEqualTo(participant)
    }

    @Test
    fun `save_should_update_an_existing_participant`() {
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

    private fun aPersistedParticipant(): Participant {
        val random = Random()
        val participant = aParticipant(random)

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

    private fun aParticipant(random: Random): Participant {
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