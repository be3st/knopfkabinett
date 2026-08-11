# Knopfkabinett

Eine absichtlich endlos wirkende Android-App im Stil moderner Systemeinstellungen.

## Öffnen und starten

1. Projektordner in Android Studio öffnen.
2. JDK 17 verwenden.
3. Android SDK 36 installieren, falls Android Studio danach fragt.
4. Gradle Sync ausführen.
5. Auf Emulator oder Gerät starten (minSdk 26).

## Verhalten

- Jede Seite enthält 12–18 Einträge in Settings-Gruppen.
- Rund ein Drittel wirkt sinnvoll, der Rest ist absurder Unsinn.
- Klick auf jeden Eintrag erzeugt aus dem bisherigen Navigationspfad eine neue Ebene.
- Die Inhalte sind pro Ebene reproduzierbar, damit Zurück-Navigation konsistent bleibt.
- Es gibt bewusst keinen Endpunkt.
- Material 3, Dynamic Color ab Android 12 und Dark Mode werden unterstützt.
