package com.example.voicelockscreen

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File

object ModelManager {
    private const val TAG = "ModelManager"
    const val MODEL_DIR = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01"

    fun isModelInAssets(context: Context): Boolean {
        return try {
            val files = context.assets.list(MODEL_DIR) ?: emptyArray()
            val hasEncoder = files.any { it.contains("encoder") && it.endsWith(".onnx") }
            val hasDecoder = files.any { it.contains("decoder") && it.endsWith(".onnx") }
            val hasJoiner = files.any { it.contains("joiner") && it.endsWith(".onnx") }
            val hasTokens = files.contains("tokens.txt")
            hasEncoder && hasDecoder && hasJoiner && hasTokens
        } catch (e: Exception) {
            Log.e(TAG, "Error checking assets model", e)
            false
        }
    }

    fun prepareCustomKeywordsFile(context: Context, keywordsStr: String): File {
        val keywordsFile = File(context.filesDir, "custom_keywords.txt")
        val formattedKeywords = keywordsStr.replace("/", "\n").trim()
        keywordsFile.writeText(formattedKeywords)
        Log.i(TAG, "Written custom keywords to ${keywordsFile.absolutePath}: $formattedKeywords")
        return keywordsFile
    }

    fun getKeywordSpotterConfig(context: Context, passphrase: String): KeywordSpotterConfig {
        val keywordsFile = prepareCustomKeywordsFile(context, passphrase)
        val featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80)

        val modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = "$MODEL_DIR/encoder-epoch-12-avg-2-chunk-16-left-64.onnx",
                decoder = "$MODEL_DIR/decoder-epoch-12-avg-2-chunk-16-left-64.onnx",
                joiner = "$MODEL_DIR/joiner-epoch-12-avg-2-chunk-16-left-64.onnx",
            ),
            tokens = "$MODEL_DIR/tokens.txt",
            modelType = "zipformer2",
            numThreads = 2,
            provider = "cpu"
        )

        return KeywordSpotterConfig(
            featConfig = featConfig,
            modelConfig = modelConfig,
            keywordsFile = keywordsFile.absolutePath,
            keywordsScore = 1.2f,
            keywordsThreshold = 0.2f
        )
    }
}
