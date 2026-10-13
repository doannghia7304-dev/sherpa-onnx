# TÀI LIỆU CÁC GIẢI PHÁP MỞ KHÓA MÀN HÌNH BẰNG GIỌNG NÓI (VOICE LOCK SCREEN)

Dự án **Voice Lock Screen** tích hợp đầy đủ **9 Giải Pháp Nhận Diện Giọng Nói & Mở Khóa Màn Hình** từ các thư viện, SDK và mô hình AI hàng đầu thế giới.

---

## BẢNG TỔNG HỢP THƯ VIỆN CỦA TỪNG SOLUTION

| STT | Tên Giải Pháp | Tác Giả / Tổ Chức | Official GitHub / Website | File Trong Dự Án |
| :-: | :--- | :--- | :--- | :--- |
| **1** | **Sherpa-ONNX (Offline KWS)** | k2-fsa (Daniel Povey) | [github.com/k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) | `VoiceLockActivity.kt` |
| **2** | **Picovoice Porcupine & Eagle** | Picovoice Inc. | [github.com/Picovoice/porcupine](https://github.com/Picovoice/porcupine)<br>[github.com/Picovoice/eagle](https://github.com/Picovoice/eagle) | `PicovoiceDemoActivity.kt` |
| **3** | **Tự Dựng Backend GPU (FastAPI)** | SpeechBrain & FastAPI | [github.com/speechbrain/speechbrain](https://github.com/speechbrain/speechbrain)<br>[github.com/fastapi/fastapi](https://github.com/fastapi/fastapi) | `CustomBackendDemoActivity.kt` |
| **4** | **Native Android OS STT** | Google Android OS | [developer.android.com/reference/android/speech/SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer) | `NativeSpeechRecognizerDemoActivity.kt` |
| **5** | **Challenge-Response Biometrics** | ID R&D / Microsoft Nuance | [github.com/IDRND/idvoice-sdk-samples](https://github.com/IDRND/idvoice-sdk-samples)<br>[github.com/Microsoft/Cognitive-SpeakerRecognition-Android](https://github.com/Microsoft/Cognitive-SpeakerRecognition-Android) | `ChallengeResponseDemoActivity.kt` |
| **6** | **Vosk Mobile Speech SDK** | Alpha Cephei | [github.com/alphacep/vosk-api](https://github.com/alphacep/vosk-api) | `VoskDemoActivity.kt` |
| **7** | **TensorFlow Lite Audio Task** | Google TensorFlow | [github.com/tensorflow/tensorflow](https://github.com/tensorflow/tensorflow) | `TFLiteAudioDemoActivity.kt` |
| **8** | **PocketSphinx Android** | Carnegie Mellon University | [github.com/cmusphinx/pocketsphinx-android](https://github.com/cmusphinx/pocketsphinx-android) | `PocketSphinxDemoActivity.kt` |
| **9** | **Silero VAD + ONNX** | Silero AI | [github.com/snakers4/silero-vad](https://github.com/snakers4/silero-vad) | `SileroVadDemoActivity.kt` |

---

## CHI TIẾT KỸ THUẬT TỪNG SOLUTION

### 1. Sherpa-ONNX (Offline KWS & Speech Lock)
- **Tác giả**: k2-fsa (sáng lập bởi Prof. Daniel Povey - tác giả Kaldi Toolkit).
- **GitHub**: [https://github.com/k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
- **Kiến trúc**: Chạy mô hình Zipformer Transducer / ECAPA-TDNN ONNX C++ JNI Native (`libsherpa-onnx-jni.so`).
- **Ưu điểm**: Nhận diện từ khóa 100% Offline, độ trễ cực thấp, không tốn dữ liệu mạng.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/VoiceLockActivity.kt`
  - `app/src/main/java/com/example/voicelockscreen/ModelManager.kt`
  - `app/src/main/assets/sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01/`

---

### 2. Picovoice Porcupine & Eagle
- **Tác giả**: Picovoice Inc.
- **GitHub**: [https://github.com/Picovoice/porcupine](https://github.com/Picovoice/porcupine) và [https://github.com/Picovoice/eagle](https://github.com/Picovoice/eagle)
- **Kiến trúc**: Kết hợp Wake-Word Engine (*Porcupine*) + Vân giọng nói Sinh trắc học (*Eagle*).
- **Ưu điểm**: Bộ SDK thương mại di động độ trễ thấp nhất thế giới (< 150ms).
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/PicovoiceDemoActivity.kt`
  - `app/src/main/res/layout/activity_picovoice_demo.xml`

---

### 3. Tự Dựng Backend GPU (FastAPI + SpeechBrain / ResNet50)
- **Tác giả**: SpeechBrain MILA Lab & FastAPI Framework.
- **GitHub**: [https://github.com/speechbrain/speechbrain](https://github.com/speechbrain/speechbrain)
- **Kiến trúc**: Luồng Client-Server. App Android thu âm PCM WAV -> Gửi Multipart HTTP POST -> Server Python FastAPI (PyTorch GPU) tính x-vector/d-vector -> Trả kết quả so khớp.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/CustomBackendDemoActivity.kt`
  - `app/src/main/res/layout/activity_custom_backend_demo.xml`

---

### 4. Native Android OS STT (CH Play Style)
- **Tác giả**: Google Android SDK.
- **Tài liệu**: [https://developer.android.com/reference/android/speech/SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- **Kiến trúc**: Sử dụng trực tiếp `SpeechRecognizer` và `RecognitionListener` của hệ điều hành Android.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/NativeSpeechRecognizerDemoActivity.kt`
  - `app/src/main/res/layout/activity_native_stt_demo.xml`

---

### 5. Challenge-Response Biometrics (Mã OTP Giọng Nói - ID R&D & Microsoft Nuance Standard)
- **Tác giả**: ID R&D Inc. & Microsoft Azure Cognitive Speaker Recognition.
- **GitHub**:
  - ID R&D Voice Biometrics SDK: [https://github.com/IDRND/idvoice-sdk-samples](https://github.com/IDRND/idvoice-sdk-samples)
  - Microsoft Speaker Recognition: [https://github.com/Microsoft/Cognitive-SpeakerRecognition-Android](https://github.com/Microsoft/Cognitive-SpeakerRecognition-Android)
- **Kiến trúc**: Sinh ngẫu nhiên dãy số OTP 6 chữ số thời gian thực. Yêu cầu người dùng đọc đúng dãy số để vừa kiểm tra câu từ vừa kiểm tra âm sắc, chống 100% tấn công giả mạo đĩa thu âm sẵn (Replay Attack).
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/ChallengeResponseDemoActivity.kt`
  - `app/src/main/res/layout/activity_challenge_response_demo.xml`

---

### 6. Vosk Mobile Speech SDK (Alpha Cephei)
- **Tác giả**: Alpha Cephei.
- **GitHub**: [https://github.com/alphacep/vosk-api](https://github.com/alphacep/vosk-api)
- **Maven Artifact**: `com.alphacephei:vosk-android:0.3.75`
- **Kiến trúc**: Kaldi C++ Engine nén chạy Offline 100% hỗ trợ đa ngôn ngữ.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/VoskDemoActivity.kt`
  - `app/src/main/res/layout/activity_vosk_demo.xml`

---

### 7. TensorFlow Lite Audio Task (Google TFLite)
- **Tác giả**: Google TensorFlow Team.
- **GitHub**: [https://github.com/tensorflow/tensorflow](https://github.com/tensorflow/tensorflow)
- **Maven Artifact**: `org.tensorflow:tensorflow-lite-task-audio:0.4.4`
- **Kiến trúc**: Phân loại vector phổ âm thanh Spectrogram / YAMNet Neural Network trực tiếp trên NPU/GPU di động.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/TFLiteAudioDemoActivity.kt`
  - `app/src/main/res/layout/activity_tflite_demo.xml`

---

### 8. PocketSphinx Android (CMU Sphinx)
- **Tác giả**: Carnegie Mellon University (CMU).
- **GitHub**: [https://github.com/cmusphinx/pocketsphinx-android](https://github.com/cmusphinx/pocketsphinx-android)
- **Kiến trúc**: Mẫu âm học Acoustic Model & JSGF Grammar Offline truyền thống.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/PocketSphinxDemoActivity.kt`
  - `app/src/main/res/layout/activity_pocketsphinx_demo.xml`

---

### 9. Silero VAD + ONNX (Voice Activity Detection)
- **Tác giả**: Silero AI.
- **GitHub**: [https://github.com/snakers4/silero-vad](https://github.com/snakers4/silero-vad)
- **Kiến trúc**: Mạng nơ-ron lọc nhiễu môi trường, phát hiện chính xác khi nào có giọng người (Speech Probability > 0.95) trước khi đưa vào mô hình nhận diện.
- **Mã nguồn trong dự án**:
  - `app/src/main/java/com/example/voicelockscreen/SileroVadDemoActivity.kt`
  - `app/src/main/res/layout/activity_silero_vad_demo.xml`

---

## LỆNH BIÊN DỊCH & CÀI ĐẶT
```powershell
# Biên dịch ứng dụng
gradlew.bat :app:assembleDebug

# Cài đặt file APK lên thiết bị
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Cấp quyền Micro & Overlay
adb shell pm grant com.example.voicelockscreen android.permission.RECORD_AUDIO
adb shell appops set com.example.voicelockscreen SYSTEM_ALERT_WINDOW allow
```
