package de.roamingthings.workbench.springjdbc.participant.domain

import java.util.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class Participant(
        val uuid: String? = null,

        val created: Date? = null,
        val updated: Date? = null,

        @NotEmpty
        @Size(max = 80)
        val firstName: String,

        @NotEmpty
        @Size(max = 80)
        val lastName: String,

        @Size(max = 80)
        val additionalNames: String? = null,

        val addresses: Set<Address> = setOf()
)

data class Address(
        val uuid: String? = null,

        @NotEmpty
        @Size(max = 80)
        val streetAddress: String,

        @NotEmpty
        @Size(max = 10)
        val postalCode: String,

        @NotEmpty
        @Size(max = 80)
        val city: String
)