package nodomain.freeyourgadget.gadgetbridge.activities.debug

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.edit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager

class MusicDebugFragment : AbstractDebugFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setupPreferences()
    }

    private fun setupPreferences() {
        setPreferencesFromResource(R.xml.debug_preferences_music, null)

        onClick(PREF_DEBUG_MUSIC_SEND_MUSICSPEC) { sendMusicSpec() }
        onClick(PREF_DEBUG_MUSIC_SEND_MUSICSTATESPEC) { sendMusicStateSpec() }
        onClick(PREF_DEBUG_MUSIC_PULL) { pullMusic() }
        onClick(PREF_DEBUG_MUSIC_RESET) { resetPreferences() }

        setInputTypeNumber(PREF_DEBUG_MUSIC_DURATION)
        setInputTypeNumber(PREF_DEBUG_MUSIC_TRACKCOUNT)
        setInputTypeNumber(PREF_DEBUG_MUSIC_TRACKNR)
        setInputTypeNumber(PREF_DEBUG_MUSIC_POSITION)
        setInputTypeNumber(PREF_DEBUG_MUSIC_PLAYRATE)

        setListPreferenceEntries(PREF_DEBUG_MUSIC_STATE, arrayOf(
            "STATE_UNKNOWN",
            "STATE_PLAYING",
            "STATE_PAUSED",
            "STATE_STOPPED",
        ))
    }

    private fun sendMusicSpec() {
        val sharedPreferences = preferenceManager.sharedPreferences!!
        val musicSpec = MusicSpec()

        musicSpec.artist = sharedPreferences.getString(PREF_DEBUG_MUSIC_ARTIST, "The Artist")
        musicSpec.album = sharedPreferences.getString(PREF_DEBUG_MUSIC_ALBUM, "The Album")
        musicSpec.track = sharedPreferences.getString(PREF_DEBUG_MUSIC_TRACK, "The Track Name")
        musicSpec.duration = sharedPreferences.getString(PREF_DEBUG_MUSIC_DURATION, "120")!!.toInt()
        musicSpec.trackCount = sharedPreferences.getString(PREF_DEBUG_MUSIC_TRACKCOUNT, "5")!!.toInt()
        musicSpec.trackNr = sharedPreferences.getString(PREF_DEBUG_MUSIC_TRACKNR, "2")!!.toInt()

        runOnDebugDevices("Send MusicSpec") {
            GBApplication.deviceService(it).onSetMusicInfo(musicSpec)
        }
    }

    private fun sendMusicStateSpec() {
        val sharedPreferences = preferenceManager.sharedPreferences!!
        val stateSpec = MusicStateSpec()

        stateSpec.state = when (sharedPreferences.getString(PREF_DEBUG_MUSIC_STATE, "STATE_PLAYING")) {
            "STATE_PLAYING" -> 0
            "STATE_PAUSED" -> 1
            "STATE_STOPPED" -> 2
            "STATE_UNKNOWN" -> -1
            else -> -1
        }
        stateSpec.position = sharedPreferences.getString(PREF_DEBUG_MUSIC_POSITION, "30")!!.toInt()
        stateSpec.playRate = sharedPreferences.getString(PREF_DEBUG_MUSIC_PLAYRATE, "100")!!.toInt()
        stateSpec.shuffle = if (sharedPreferences.getBoolean(PREF_DEBUG_MUSIC_SHUFFLE_BOOL, false)) 1 else 0
        stateSpec.repeat = if (sharedPreferences.getBoolean(PREF_DEBUG_MUSIC_REPEAT_BOOL, false)) 1 else 0

        runOnDebugDevices("Send MusicStateSpec") {
            GBApplication.deviceService(it).onSetMusicState(stateSpec)
        }
    }

    private fun pullMusic() {
        val mediaManager = MediaManager(requireContext())
        mediaManager.refresh()

        if (mediaManager.bufferMusicSpec == null || mediaManager.bufferMusicStateSpec == null) {
            GB.toast(requireContext(), "No media playing?", Toast.LENGTH_SHORT, GB.ERROR)
        }

        preferenceManager.sharedPreferences!!.edit {
            mediaManager.bufferMusicSpec?.let {
                putString(PREF_DEBUG_MUSIC_ARTIST, it.artist)
                putString(PREF_DEBUG_MUSIC_ALBUM, it.album)
                putString(PREF_DEBUG_MUSIC_TRACK, it.track)
                putString(PREF_DEBUG_MUSIC_DURATION, it.duration.toString())
                putString(PREF_DEBUG_MUSIC_TRACKCOUNT, it.trackCount.toString())
                putString(PREF_DEBUG_MUSIC_TRACKNR, it.trackNr.toString())
            }

            mediaManager.bufferMusicStateSpec?.let {
                putString(
                    PREF_DEBUG_MUSIC_STATE, when (it.state) {
                        0.toByte() -> "STATE_PLAYING"
                        1.toByte() -> "STATE_PAUSED"
                        2.toByte() -> "STATE_STOPPED"
                        else -> "STATE_UNKNOWN"
                    }
                )
                putString(PREF_DEBUG_MUSIC_POSITION, it.position.toString())
                putString(PREF_DEBUG_MUSIC_PLAYRATE, it.playRate.toString())
                putBoolean(PREF_DEBUG_MUSIC_SHUFFLE_BOOL, it.shuffle != 0.toByte())
                putBoolean(PREF_DEBUG_MUSIC_REPEAT_BOOL, it.repeat != 0.toByte())
            }
        }
    }

    private fun resetPreferences() {
        preferenceScreen.removeAll()

        preferenceManager.sharedPreferences!!.edit(true) {
            remove(PREF_HEADER_MUSICSPEC)
            remove(PREF_DEBUG_MUSIC_ARTIST)
            remove(PREF_DEBUG_MUSIC_ALBUM)
            remove(PREF_DEBUG_MUSIC_TRACK)
            remove(PREF_DEBUG_MUSIC_DURATION)
            remove(PREF_DEBUG_MUSIC_TRACKCOUNT)
            remove(PREF_DEBUG_MUSIC_TRACKNR)
            remove(PREF_HEADER_MUSICSTATESPEC)
            remove(PREF_DEBUG_MUSIC_STATE)
            remove(PREF_DEBUG_MUSIC_POSITION)
            remove(PREF_DEBUG_MUSIC_PLAYRATE)
            remove(PREF_DEBUG_MUSIC_SHUFFLE_BOOL)
            remove(PREF_DEBUG_MUSIC_REPEAT_BOOL)
        }

        // Reload the preference screen to reflect the changes
        setupPreferences()
    }

    companion object {
        private const val PREF_DEBUG_MUSIC_SEND_MUSICSPEC = "pref_debug_music_send_musicspec"
        private const val PREF_DEBUG_MUSIC_SEND_MUSICSTATESPEC = "pref_debug_music_send_musicstatespec"
        private const val PREF_DEBUG_MUSIC_PULL = "pref_debug_music_pull"
        private const val PREF_DEBUG_MUSIC_RESET = "pref_debug_music_reset"
        private const val PREF_HEADER_MUSICSPEC = "pref_header_musicspec"
        private const val PREF_DEBUG_MUSIC_ARTIST = "pref_debug_music_artist"
        private const val PREF_DEBUG_MUSIC_ALBUM = "pref_debug_music_album"
        private const val PREF_DEBUG_MUSIC_TRACK = "pref_debug_music_track"
        private const val PREF_DEBUG_MUSIC_DURATION = "pref_debug_music_duration"
        private const val PREF_DEBUG_MUSIC_TRACKCOUNT = "pref_debug_music_trackCount"
        private const val PREF_DEBUG_MUSIC_TRACKNR = "pref_debug_music_trackNr"
        private const val PREF_HEADER_MUSICSTATESPEC = "pref_header_musicstatespec"
        private const val PREF_DEBUG_MUSIC_STATE = "pref_debug_music_state"
        private const val PREF_DEBUG_MUSIC_POSITION = "pref_debug_music_position"
        private const val PREF_DEBUG_MUSIC_PLAYRATE = "pref_debug_music_playRate"
        private const val PREF_DEBUG_MUSIC_SHUFFLE_BOOL = "pref_debug_music_shuffle_bool"
        private const val PREF_DEBUG_MUSIC_REPEAT_BOOL = "pref_debug_music_repeat_bool"
    }
}
