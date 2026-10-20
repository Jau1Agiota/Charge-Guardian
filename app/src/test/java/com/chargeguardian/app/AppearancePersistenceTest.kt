package com.chargeguardian.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppearancePersistenceTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun themeSurvivesClosingAndReopeningStorageWithoutChangingSession() = runBlocking {
        val file = File(folder.root, "appearance.preferences_pb")
        val firstJob = SupervisorJob()
        val first = Store(PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO), produceFile = { file }))
        try {
            assertEquals(Appearance.ELECTRIC_NEON, first.settings.first().appearance)
            first.enable(true)
            assertEquals(95, first.sample(true, 96))
            first.appearance(Appearance.NEON_MINT)
        } finally { firstJob.cancelAndJoin() }

        val reopenedJob = SupervisorJob()
        val reopened = Store(PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO), produceFile = { file }))
        try {
            assertEquals(Settings(95, true, Session(true, true, 1), Appearance.NEON_MINT), reopened.settings.first())
            assertEquals(null, reopened.sample(true, 99))
        } finally { reopenedJob.cancelAndJoin() }
    }
}
