package com.example.voicelockscreen

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

object ModelManager {
    private const val TAG = "ModelManager"
    const val MODEL_DIR = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01"

    fun isModelInAssets(context: Context): Boolean {
        return try {
            val files = context.assets.list(MODEL_DIR) ?: emptyArray()
            val hasEncoder = files.contains("encoder-epoch-12-avg-2-chunk-16-left-64.onnx")
            val hasDecoder = files.contains("decoder-epoch-12-avg-2-chunk-16-left-64.onnx")
            val hasJoiner = files.contains("joiner-epoch-12-avg-2-chunk-16-left-64.onnx")
            val hasTokens = files.contains("tokens.txt")
            val hasKeywords = files.contains("keywords.txt")
            hasEncoder && hasDecoder && hasJoiner && hasTokens && hasKeywords
        } catch (e: Exception) {
            Log.e(TAG, "Error checking assets model", e)
            false
        }
    }

    fun getKeywordSpotterConfig(context: Context, passphrase: String): KeywordSpotterConfig {
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
            keywordsFile = "$MODEL_DIR/keywords.txt",
            keywordsScore = 1.0f,
            keywordsThreshold = 0.15f
        )
    }
}
