package com.subtrack.shared

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.subtrack.shared.db.SubTrackDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RenewalReminderTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: SubscriptionRepository

    private val today = LocalDate.parse("2026-08-01")

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SubTrackDatabase.Schema.create(driver)
        repository = SubscriptionRepository(SubTrackDatabase(driver), UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private suspend fun add(
        name: String,
        anchor: String,
        notifyDaysBefore: Int = 3,
        cycle: BillingCycle = BillingCycle.MONTHLY,
    ) = repository.add(
        SubscriptionDraft(
            name = name,
            amountCents = 5490,
            currency = "BRL",
            cycle = cycle,
            anchor = LocalDate.parse(anchor),
            notifyDaysBefore = notifyDaysBefore,
        ),
    )

    private suspend fun reminders() = repository.observeActive(today).first().remindersOnOrAfter(today)

    @Test
    fun remindsTheConfiguredNumberOfDaysBeforeRenewal() = runTest {
        add("Netflix", anchor = "2026-01-20", notifyDaysBefore = 3)

        val reminder = reminders().single()
        assertEquals(LocalDate.parse("2026-08-20"), reminder.renewalDate)
        assertEquals(LocalDate.parse("2026-08-17"), reminder.remindOn)
        assertEquals(3, reminder.daysBeforeRenewal)
    }

    @Test
    fun zeroDaysBeforeRemindsOnTheRenewalItself() = runTest {
        add("Mesmo dia", anchor = "2026-01-20", notifyDaysBefore = 0)

        val reminder = reminders().single()
        assertEquals(reminder.renewalDate, reminder.remindOn)
    }

    @Test
    fun aReminderWhoseDayAlreadyPassedIsPulledToTodayInsteadOfBeingDropped() = runTest {
        // Renova em 03/08 com aviso de 7 dias: o dia do aviso seria 27/07,
        // que já passou. Descartar seria a falha mais cara deste app —
        // o aviso vai hoje, ainda a tempo da cobrança.
        add("Quase vencendo", anchor = "2026-01-03", notifyDaysBefore = 7)

        val reminder = reminders().single()
        assertEquals(LocalDate.parse("2026-08-03"), reminder.renewalDate)
        assertEquals(today, reminder.remindOn)
        assertTrue(reminder.daysBeforeRenewal < 7)
    }

    @Test
    fun remindersAreOrderedByWhenTheyFire() = runTest {
        add("Terceiro", anchor = "2026-01-25")
        add("Primeiro", anchor = "2026-01-06")
        add("Segundo", anchor = "2026-01-14")

        assertEquals(
            listOf("Primeiro", "Segundo", "Terceiro"),
            reminders().map { it.subscriptionName },
        )
    }

    @Test
    fun archivedSubscriptionsProduceNoReminder() = runTest {
        val id = add("Cancelada", anchor = "2026-01-20")
        repository.setArchived(id, archived = true)

        assertTrue(reminders().isEmpty())
    }

    @Test
    fun theReminderCarriesWhatTheNotificationNeedsToShow() = runTest {
        val id = add("Spotify", anchor = "2026-01-20")

        val reminder = reminders().single()
        assertEquals(id, reminder.subscriptionId)
        assertEquals("Spotify", reminder.subscriptionName)
        assertEquals(5490, reminder.amountCents)
        assertEquals("BRL", reminder.currency)
    }

    @Test
    fun aReminderIsDueOnceItsDayArrives() = runTest {
        add("Netflix", anchor = "2026-01-04", notifyDaysBefore = 3)

        // Renova 04/08, avisa 01/08 — que é hoje.
        assertEquals(listOf("Netflix"), reminders().dueOn(today).map { it.subscriptionName })
    }

    @Test
    fun aReminderIsNotDueBeforeItsDay() = runTest {
        add("Longe", anchor = "2026-01-20", notifyDaysBefore = 3)

        // Avisa só em 17/08; hoje é 01/08.
        assertTrue(reminders().dueOn(today).isEmpty())
    }

    @Test
    fun theSameRenewalIsNotAnnouncedTwice() = runTest {
        // Sem isto o worker diário viraria spam: um aviso de 3 dias antes
        // dispararia 3 vezes, e um atrasado dispararia todo dia até a cobrança.
        val id = add("Netflix", anchor = "2026-01-04", notifyDaysBefore = 3)
        val renewal = reminders().single().renewalDate

        repository.markNotified(id, renewal)

        assertTrue(reminders().dueOn(today).isEmpty())
        assertTrue(reminders().single().alreadyNotified)
    }

    @Test
    fun theNextRenewalIsAnnouncedEvenAfterThePreviousOneWas() = runTest {
        // Avisar de agosto não pode calar o aviso de setembro.
        val id = add("Netflix", anchor = "2026-01-04", notifyDaysBefore = 3)
        repository.markNotified(id, LocalDate.parse("2026-07-04"))

        val reminder = reminders().single()
        assertEquals(LocalDate.parse("2026-08-04"), reminder.renewalDate)
        assertTrue(!reminder.alreadyNotified)
        assertEquals(listOf("Netflix"), reminders().dueOn(today).map { it.subscriptionName })
    }

    @Test
    fun yearlySubscriptionsAreRemindedTooNotJustMonthlyOnes() = runTest {
        add("Anual", anchor = "2025-09-10", cycle = BillingCycle.YEARLY, notifyDaysBefore = 5)

        val reminder = reminders().single()
        assertEquals(LocalDate.parse("2026-09-10"), reminder.renewalDate)
        assertEquals(LocalDate.parse("2026-09-05"), reminder.remindOn)
    }
}
