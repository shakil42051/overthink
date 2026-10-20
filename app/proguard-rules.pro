# OkHttp 4.10.0 probes these optional JVM TLS providers. Android uses the
# Android10Platform / AndroidPlatform branch instead; no optional provider is installed.
# Exact missing-class rules reported by R8, not blanket warning suppression.
# https://github.com/square/okhttp/blob/parent-4.10.0/okhttp/src/main/kotlin/okhttp3/internal/platform/Platform.kt
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
