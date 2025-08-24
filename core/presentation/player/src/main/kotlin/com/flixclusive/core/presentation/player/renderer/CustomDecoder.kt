package com.flixclusive.core.presentation.player.renderer

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.dvb.DvbParser
import androidx.media3.extractor.text.pgs.PgsParser
import androidx.media3.extractor.text.ssa.SsaParser
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.media3.extractor.text.ttml.TtmlParser
import androidx.media3.extractor.text.tx3g.Tx3gParser
import androidx.media3.extractor.text.webvtt.Mp4WebvttParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import com.flixclusive.core.presentation.player.SubtitleOffsetProvider
import com.flixclusive.core.util.log.errorLog
import org.mozilla.universalchardet.UniversalDetector

/**
 *
 * Code from: [Cloudstream3 TextRenderer](https://github.com/recloudstream/cloudstream/blob/743527aa4060eddb6649a61b01fb009b3d77a9d2/app/src/main/java/com/lagradost/cloudstream3/ui/subtitles/SubtitlesFragment.kt#L55)
 *
 *
 * @param fallbackFormat used to create a decoder based on mimetype if the subtitle string is not
 * enough to identify the subtitle format.
 **/
@OptIn(UnstableApi::class)
internal class CustomDecoder(
    private val fallbackFormat: Format?,
    private val offsetProvider: SubtitleOffsetProvider,
) : SubtitleParser {
    private var realDecoder: SubtitleParser? = null

    /**
     * Try to convert the byte array to a readable string using a custom charset detection
     *
     * @return Pair of the decoded string and the charset used
     * */
    private fun ByteArray.toReadableSubtitle(): String {
        val encoding =
            try {
                val detector = UniversalDetector()
                detector.handleData(this, 0, size)
                detector.dataEnd()

                val encoding = detector.detectedCharset // "windows-1256"

                Log.i(TAG, "Detected encoding with charset $encoding")
                encoding ?: UTF_8
            } catch (e: Exception) {
                Log.e(TAG, "Failed to detect encoding throwing error")
                errorLog(e)
                UTF_8
            }

        return try {
            String(this, charset(encoding))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse using encoding $encoding")
            errorLog(e)
            decodeToString()
        }
    }

    /**
     * This way we read the subtitle file and decide what decoder to use instead of relying fully on mimetype
     *
     * First we remove all invisible characters at the start, this is an issue in some subtitle files:
     * - Cntrl is control characters: https://en.wikipedia.org/wiki/Unicode_control_characters
     * - Cf is formatting characters: https://www.compart.com/en/unicode/category/Cf
     * */
    private fun getSubtitleParser(data: String): SubtitleParser? {
        val controlCharsRegex = Regex("""[\p{Cntrl}\p{Cf}]""")
        val trimmedText = data.trimStart {
            it.isWhitespace() || controlCharsRegex.matches(it.toString())
        }

        val subtitleParser =
            when {
                trimmedText.isWebVtt() -> WebvttParser()
                trimmedText.isTtml() -> TtmlParser()
                trimmedText.isSsa() -> SsaParser(fallbackFormat?.initializationData)
                trimmedText.isSrt() -> SubripParser()
                fallbackFormat != null -> {
                    when (fallbackFormat.sampleMimeType) {
                        MimeTypes.TEXT_VTT -> WebvttParser()
                        MimeTypes.TEXT_SSA -> SsaParser(fallbackFormat.initializationData)
                        MimeTypes.APPLICATION_MP4VTT -> Mp4WebvttParser()
                        MimeTypes.APPLICATION_TTML -> TtmlParser()
                        MimeTypes.APPLICATION_SUBRIP -> SubripParser()
                        MimeTypes.APPLICATION_TX3G -> Tx3gParser(fallbackFormat.initializationData)
                        MimeTypes.APPLICATION_DVBSUBS -> DvbParser(fallbackFormat.initializationData)
                        MimeTypes.APPLICATION_PGS -> PgsParser()
                        // TODO: These decoders are not converted to parsers yet
//                            MimeTypes.APPLICATION_CEA608, MimeTypes.APPLICATION_MP4CEA608 -> Cea608Decoder(
//                                mimeType,
//                                fallbackFormat.accessibilityChannel,
//                                Cea608Decoder.MIN_DATA_CHANNEL_TIMEOUT_MS
//                            )
//                            MimeTypes.APPLICATION_CEA708 -> Cea708Decoder(
//                                fallbackFormat.accessibilityChannel,
//                                fallbackFormat.initializationData
//                            )
                        else -> null
                    }
                }

                else -> null
            }

        return subtitleParser
    }

//    val currentSubtitleCues = mutableListOf<SubtitleCue>()

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>,
    ) {
        val customOutput =
            Consumer<CuesWithTiming> { cue ->
                val currentOffset = offsetProvider.currentSubtitleOffset
                val updatedCues =
                    CuesWithTiming(
                        cue.cues,
                        cue.startTimeUs - currentOffset.times(1000),
                        cue.durationUs,
                    )

                output.accept(updatedCues)
            }

        try {
            val inputString = data.toReadableSubtitle()
            Log.i(TAG, "Current subtitle to preview: ${inputString.substring(0, 30)}")

            if (inputString.isNotBlank()) {
                var str = inputString.trimStr()
                realDecoder = realDecoder
                    ?: getSubtitleParser(inputString)
                        .also { Log.i(TAG, "Parser selected: $it") }

                if (realDecoder !is SsaParser) {
                    // TODO: Apply styles there's a need to do so in the future.
                    //       For example, if user wants uppercased subtitles
                    //       Or if user wants to remove certain styles

                    // TODO: Make this optional in the future if user wants to keep bloat.
                    //       Add this feature in DataStore.
                    bloatRegex.forEach { rgx ->
                        str = str.replace(rgx, "\n")
                    }
                }

                val array = str.toByteArray()
                realDecoder?.parse(
                    // data =
                    array,
                    // offset =
                    minOf(array.size, offset),
                    // length =
                    minOf(array.size, length),
                    // outputOptions =
                    outputOptions,
                    // output =
                    customOutput,
                )
            }
        } catch (e: Exception) {
            errorLog(e)
        }
    }

    override fun getCueReplacementBehavior(): Int {
        // CUE_REPLACEMENT_BEHAVIOR_REPLACE seems most compatible, change if required
        return realDecoder?.cueReplacementBehavior ?: Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE
    }

    override fun reset() {
//        currentSubtitleCues.clear()
        super.reset()
    }

    internal companion object {
        private const val UTF_8 = "UTF-8"
        private const val TAG = "CustomDecoder"

        /**
         * A list of regex patterns to identify and remove common bloat/ad text found in subtitles from subtitle sources.
         * */
        private val bloatRegex =
            listOf(
                Regex(
                    pattern = """Support\s+us\s+and\s+become\s+VIP\s+member\s+to\s+remove\s+all\s+ads\s+from\s+(www\.|)OpenSubtitles(\.org|)""",
                    option = RegexOption.IGNORE_CASE,
                ),
                Regex(
                    pattern = """Please\s+rate\s+this\s+subtitle\s+at\s+.*\s+Help\s+other\s+users\s+to\s+choose\s+the\s+best\s+subtitles""",
                    option = RegexOption.IGNORE_CASE,
                ),
                Regex(
                    pattern = """Contact\s(www\.|)OpenSubtitles(\.org|)\s+today""",
                    option = RegexOption.IGNORE_CASE,
                ),
                Regex(
                    pattern = """Advertise\s+your\s+product\s+or\s+brand\s+here""",
                    option = RegexOption.IGNORE_CASE,
                ),
            )

        // val captionRegex = listOf(Regex("""(-\s?|)[\[({][\w\s]*?[])}]\s*"""))

        /**
         * Trim invisible characters from the start and non-breaking spaces from the end of the string
         * We use a custom regex to match all non-breaking spaces in unicode
         *
         * See:
         * - [https://emptycharacter.com/](https://emptycharacter.com/)
         * - [https://www.fileformat.info/info/unicode/char/200b/index.htm](https://www.fileformat.info/info/unicode/char/200b/index.htm)
         * */
        private fun String.trimStr(): String {
            val regex = Regex("[\u00A0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u205F]")

            return trimStart()
                .trim('\uFEFF', '\u200B')
                .replace(regex, " ")
        }

        /**
         * Check if the subtitle string is a WebVTT file
         * We only check the first 10 characters to avoid issues with BOM or invisible characters
         * at the start of the file.
         * */
        private fun String.isWebVtt(): Boolean {
            return substring(0, 10).contains("WEBVTT", ignoreCase = true)
        }

        /**
         * Check if the subtitle string is a TTML file
         * We check if it starts with the XML declaration.
         * TTML files are XML files, so they should start with this declaration.
         * */
        private fun String.isTtml(): Boolean {
            return startsWith("<?xml version=\"", ignoreCase = true)
        }

        /**
         * Check if the subtitle string is an SSA file
         * We check if it starts with the [Script Info] header or Title:
         * SSA files usually start with one of these lines.
         * */
        private fun String.isSsa(): Boolean {
            return startsWith("[Script Info]", ignoreCase = true) ||
                startsWith("Title:", ignoreCase = true)
        }

        /**
         * Check if the subtitle string is an SRT file
         * We check if it starts with "1", as SRT files start with the first subtitle number.
         * */
        private fun String.isSrt(): Boolean {
            return startsWith("1", ignoreCase = true)
        }
    }
}
