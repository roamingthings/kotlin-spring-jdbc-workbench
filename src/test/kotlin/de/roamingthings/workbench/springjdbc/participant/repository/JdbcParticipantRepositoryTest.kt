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

@RunWith(SpringRunner::class)
@DataJpaTest
@ComponentScan(basePackages = arrayOf("de.roamingthings.workbench.springjdbc.participant"))
class JdbcParticipantRepositoryTest {

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
}