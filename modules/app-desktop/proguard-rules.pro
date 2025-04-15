# TODO: cleanup this and document why which rules are added

# needed for io.github.vinceglb.filekit.core.platform.xdg.XdgFilePickerPortal.isAvailable
-keep class org.freedesktop.** { *; }
-keep class io.github.vinceglb.filekit.** { *; }

# resources:
#  - https://stackoverflow.com/a/61707441/409102
#  - https://stackoverflow.com/a/58057593/409102
#  - https://stackoverflow.com/a/27084975/409102
#  - https://github.com/ktorio/ktor/issues/379
#  - https://github.com/grpc/grpc-java/issues/2149

-dontwarn com.nimbusds.jose.**
-dontwarn com.oracle.svm.core.annotate.**

-dontwarn okio.AsyncTimeout$Watchdog

-dontskipnonpubliclibraryclasses

-keepattributes Signature,InnerClasses
-keepclasseswithmembers class io.netty.** {
    *;
}
-keep class io.grpc.** {
    *;
}

-keep class com.sun.jna.** { *; }

-keep class org.archivekeep.files.internal.grpc.**{ *; }
-dontwarn org.archivekeep.files.internal.grpc.**

-keepclassmembers class org.archivekeep.utils.coroutines.InstanceProtector$Instance {
    <fields>;
}

-keep class com.google.protobuf.**{ *; }
-dontwarn com.google.protobuf.**

-keep class com.squareup.okhttp.**{ *; }
-dontwarn com.squareup.okhttp.**

-keep class io.grpc.netty.shaded.io{ *; }
-dontwarn io.grpc.netty.shaded.io.**

-keep class android.net.http{ *; }
-dontwarn android.net.http.**

-keep class com.google.common{ *; }
-dontwarn com.google.common.**

-keep class org.apache{ *; }
-dontwarn org.apache.**

-keep class org.joda{ *; }
-dontwarn org.joda.**

# ################################################################ #
# FROM https://github.com/google/guava/wiki/UsingProGuardWithGuava #
# ################################################################ #

-dontwarn javax.lang.model.element.Modifier

# Note: We intentionally don't add the flags we'd need to make Enums work.
# That's because the Proguard configuration required to make it work on
# optimized code would preclude lots of optimization, like converting enums
# into ints.

# Throwables uses internal APIs for lazy stack trace resolution
-dontnote sun.misc.SharedSecrets
-keep class sun.misc.SharedSecrets {
  *** getJavaLangAccess(...);
}
-dontnote sun.misc.JavaLangAccess
-keep class sun.misc.JavaLangAccess {
  *** getStackTraceElement(...);
  *** getStackTraceDepth(...);
}

# FinalizableReferenceQueue calls this reflectively
# Proguard is intelligent enough to spot the use of reflection onto this, so we
# only need to keep the names, and allow it to be stripped out if
# FinalizableReferenceQueue is unused.
-keepnames class com.google.common.base.internal.Finalizer {
  *** startFinalizer(...);
}
# However, it cannot "spot" that this method needs to be kept IF the class is.
-keepclassmembers class com.google.common.base.internal.Finalizer {
  *** startFinalizer(...);
}
-keepnames class com.google.common.base.FinalizableReference {
  void finalizeReferent();
}
-keepclassmembers class com.google.common.base.FinalizableReference {
  void finalizeReferent();
}

# Striped64, LittleEndianByteArray, UnsignedBytes, AbstractFuture
-dontwarn sun.misc.Unsafe

# Striped64 appears to make some assumptions about object layout that
# really might not be safe. This should be investigated.
-keepclassmembers class com.google.common.cache.Striped64 {
  *** base;
  *** busy;
}
-keepclassmembers class com.google.common.cache.Striped64$Cell {
  <fields>;
}

-dontwarn java.lang.SafeVarargs

-keep class java.lang.Throwable {
  *** addSuppressed(...);
}

# Futures.getChecked, in both of its variants, is incompatible with proguard.

# Used by AtomicReferenceFieldUpdater and sun.misc.Unsafe
-keepclassmembers class com.google.common.util.concurrent.AbstractFuture** {
  *** waiters;
  *** value;
  *** listeners;
  *** thread;
  *** next;
}
-keepclassmembers class com.google.common.util.concurrent.AtomicDouble {
  *** value;
}
-keepclassmembers class com.google.common.util.concurrent.AggregateFutureState {
  *** remaining;
  *** seenExceptions;
}

# Since Unsafe is using the field offsets of these inner classes, we don't want
# to have class merging or similar tricks applied to these classes and their
# fields. It's safe to allow obfuscation, since the by-name references are
# already preserved in the -keep statement above.
-keep,allowshrinking,allowobfuscation class com.google.common.util.concurrent.AbstractFuture** {
  <fields>;
}

# Futures.getChecked (which often won't work with Proguard anyway) uses this. It
# has a fallback, but again, don't use Futures.getChecked on Android regardless.
-dontwarn java.lang.ClassValue

# MoreExecutors references AppEngine
-dontnote com.google.appengine.api.ThreadManager
-keep class com.google.appengine.api.ThreadManager {
  static *** currentRequestThreadFactory(...);
}
-dontnote com.google.apphosting.api.ApiProxy
-keep class com.google.apphosting.api.ApiProxy {
  static *** getCurrentEnvironment (...);
}

# ################################################################ #
