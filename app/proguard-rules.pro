# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/richardmuttett/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ButterKnife
-keep class butterknife.** { *; }
-keep class **$$ViewInjector { *; }

-dontwarn butterknife.internal.**

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# mime parser
-dontwarn org.apache.commons.**
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.harmony.**
-keep class org.apache.harmony.** { *; }
-dontwarn org.apache.http.legacy.**
-keep class org.apache.http.legacy.** { *; }
-dontwarn com.sun.mail.**
-keep class com.sun.mail.** { *; }
-dontwarn javax.activation.**
-keep class javax.activation.** { *; }

# OkHttp, Retrofit, Gson, Okio
-keepattributes Exceptions
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.* { *; }
-dontwarn retrofit.**
-dontwarn ch.protonmail.android.utils.nativelib.**
-dontwarn okio.**
-dontwarn org.wordpress.**
-keepnames class rx.Single
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.-KotlinExtensions
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

## Okhttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontnote okhttp3.**
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn okhttp3.internal.platform.*
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

## Retrofit
-keep class org.wordpress.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn rx.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers interface * {
    @retrofit2.* <methods>;
}
-dontnote retrofit2.Platform
-dontwarn retrofit2.adapter.rxjava.CompletableHelper$**

## Okio
-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

## Gson
-keep class com.google.gson.** { *; }
-keep public class com.google.gson.** {public private protected *;}
-keep class com.google.inject.** { *; }

# gms and view pager indicator
-dontwarn com.google.android.gms.**
-dontwarn com.viewpagerindicator.**


# Splunk MINT
-keep class com.splunk.** { *; }

# Otto
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}


# SearchView
-keep public class android.support.v7.widget.SearchView {
   public <init>(android.content.Context);
   public <init>(android.content.Context, android.util.AttributeSet);
}

# for scrollable webview
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# Android design support library
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

# Android compat support library
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

# jsoup
-keep public class org.jsoup.** {
    public *;
}

# PM custom views
-keep public class ch.protonmail.android.views.** {
    public *;
}

# PM openPGP
-keep public class ch.protonmail.android.utils.crypto.** { *; }
-keep public class ch.protonmail.android.views.behavior.** { *; }
-keep public class ch.protonmail.android.utils.MIME.** { *; }
-keep public class ch.protonmail.android.data.local.model.MessagesTypesConverter { }
-keep class ch.protonmail.android.activities.fragments.HumanVerificationCaptchaFragment$WebAppInterface {
  *;
}
-keep public interface ch.protonmail.android.adapters.base.ClickableAdapter { *; }

# PM goopenpgp
-keep class com.proton.gopenpgp.** { *; }

# ez-vcard
-dontwarn com.fasterxml.jackson.**		# Jackson JSON Processor (for jCards) not used
-dontwarn freemarker.**				# freemarker templating library (for creating hCards) not used
-dontwarn org.jsoup.**				# jsoup library (for hCard parsing) not used
-dontwarn sun.misc.Perf
-keep class ezvcard.property.** { *; }		# keep all VCard properties (created at runtime)
-dontwarn ezvcard.io.json.**            # JSON serializer (for jCards) not used
-keep,includedescriptorclasses class ezvcard.property.** { *; }  # keep all VCard properties (created at runtime)
-keep class ezvcard.VCardVersion { *; }

# dagger
-dontwarn dagger.internal.codegen.**
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}

-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection

-keepattributes LineNumberTable,SourceFile
-dontwarn org.slf4j.**
-dontwarn javax.**

#Andorid Injector
-dontwarn com.google.errorprone.annotations.**

#Material Tabs
-keep class com.google.android.material.tabs.** {*;}

#Lifecycle
-keep class android.arch.** {*;}
-keep class * implements android.arch.lifecycle.LifecycleObserver {
    <init>(...);
}
-keepclassmembers class * extends android.arch.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class android.arch.lifecycle.Lifecycle$State { *; }
-keepclassmembers class android.arch.lifecycle.Lifecycle$Event { *; }
-keepclassmembers class * {
    @android.arch.lifecycle.OnLifecycleEvent *;
}

#jobqueue
-keep interface com.birbit.android.jobqueue.** { *; }

#PM
-keep class ch.protonmail.android.api.** { *;}
-keep class ch.protonmail.android.uiModel.** { *; }

#kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

-keepnames class kotlinx.coroutines.experimental.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.experimental.CoroutineExceptionHandler {}

-keep class org.jetbrains.kotlin.** { *; }
-keep class org.jetbrains.annotations.** { *; }
-keepclassmembers class ** {
  @org.jetbrains.annotations.ReadOnly public *;
}
-keep public class kotlin.reflect.jvm.internal.impl.** { public *; }

-dontwarn kotlin.reflect.jvm.internal.impl.descriptors.CallableDescriptor
-dontwarn kotlin.reflect.jvm.internal.impl.descriptors.ClassDescriptor
-dontwarn kotlin.reflect.jvm.internal.impl.descriptors.ClassifierDescriptorWithTypeParameters
-dontwarn kotlin.reflect.jvm.internal.impl.descriptors.annotations.AnnotationDescriptor
-dontwarn kotlin.reflect.jvm.internal.impl.descriptors.impl.PropertyDescriptorImpl
-dontwarn kotlin.reflect.jvm.internal.impl.load.java.JavaClassFinder
-dontwarn kotlin.reflect.jvm.internal.impl.resolve.OverridingUtil
-dontwarn kotlin.reflect.jvm.internal.impl.types.DescriptorSubstitutor
-dontwarn kotlin.reflect.jvm.internal.impl.types.DescriptorSubstitutor
-dontwarn kotlin.reflect.jvm.internal.impl.types.TypeConstructor

#RxJava, RxAndroid
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}
-dontwarn sun.misc.Unsafe

# Coroutines
-dontwarn kotlinx.atomicfu.AtomicBoolean
