# R60 BLE Bike (Android)

R60 fitness bisikletine BLE (FTMS – Indoor Bike Data, 0x1826/0x2AD2) ile bağlanır; hız, kadans, güç, mesafeyi canlı gösterir, oturumları Room'a kaydeder ve CSV olarak dışa aktarır.

**Teknoloji:** Kotlin, Jetpack Compose (Material 3), MVVM, Hilt, Room, Coroutines/Flow, Navigation, DataStore.

## Kurulum
1. Android Studio (Koala+) ile klasörü açın, Gradle sync bekleyin (JDK 17).
2. Komut satırı için bir kez: `gradle wrapper --gradle-version 8.9` (gradlew üretir; commit'leyin).
3. Gerçek bir cihaza çalıştırın (emülatör BLE desteklemez): `./gradlew installDebug`

## Kullanım
1. **Bağlan:** "Tara" → R60/FTMS cihazı listenin başında çıkar → dokunarak bağlanın.
2. **Canlı:** metrikler ve grafikler akar; "Oturumu başlat" / "Durdur ve kaydet".
3. **Geçmiş:** oturuma dokunun → detay, CSV dışa aktar (paylaş/kaydet), sil.
4. **Ayarlar:** koyu tema, otomatik yeniden bağlanma.

## Test
- Birim: `./gradlew testDebugUnitTest` (FTMS ayrıştırıcı, oturum hesaplama)
- UI (cihaz/emülatör): `./gradlew connectedDebugAndroidTest`

## Mimari
`data/ble` (BleManager, FtmsParser) → `data` (WorkoutRepository, Room `data/db`, DataStore) → `ui/*` (Compose + ViewModel). `domain` modeller ve hesaplama.

## Önemli notlar
- **R60 gerçekten FTMS kullanıyor mu?** `BleManager`, Indoor Bike Data karakteristiğini arar. Farklıysa nRF Connect ile UUID'leri kontrol edip `BleManager`/`FtmsParser` içinde değiştirin (ör. Cycling Power 0x1818 / vendor UUID).
- İzinler: Android 12+ `BLUETOOTH_SCAN/CONNECT` (konum gerekmez); 6–11 arası konum izni zorunlu.
- Gizlilik: yalnızca gerekli veriler yerelde saklanır, `allowBackup=false`, CSV `FileProvider` ile geçici paylaşılır. Güvenlik: bağlantıyı yalnızca hedef cihaza yapın; cihaz eşleştirme (bonding) gerektiriyorsa `createBond()` ekleyin.
- `targetSdk` 34; Play Store için güncel API'ye yükseltin.
- Dil seçimi şimdilik sistem diline bağlı; çoklu dil için `values-xx/strings.xml` ve `AppCompatDelegate.setApplicationLocales` eklenebilir.
- Repository/ViewModel testleri için MockK + coroutines-test eklenip iskelet genişletilebilir.
