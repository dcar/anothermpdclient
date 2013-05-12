import sbt._

import Keys._
import org.scalasbt.androidplugin._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "anothermpdclient",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.10.1",
    platformName in Android := "android-15",
    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Android Support" at "http://www.ldsmobile.org/nexus/content/repositories/public"
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.1.0",
      "com.google.android" % "support-v4" % "r11"
      /*"com.typesafe.akka" %% "akka-agent" % "2.1.0"*/
    )
  )

  val proguardSettings = Seq (
    useProguard in Android := true,
		proguardOptimizations in Android := Seq(
			"-keep class scala.PartialFunction",
			"-keep class scala.Function1",
			"-keep class akka.actor.ActorRef",
			"-keep class akka.actor.ActorContext",
			"-keep class akka.actor.ExtendedActorSystem",
			"-keep class akka.actor.DynamicAccess",
			"-keep class akka.actor.Scheduler",
			"-keep class akka.event.EventStream",
			"-keep class akka.actor.ActorSystem$Settings",
			"-keep class akka.actor.Deployer",
			"-keep class scala.Option",
			"-keep class scala.Tuple2",
                        /*"-keep class scala.concurrent.stm.ccstm.CCSTM",*/
			"-keep class akka.actor.DefaultSupervisorStrategy",
			"-keep class android.support.v4.app.Fragment$SavedState",
			"-keep class android.support.v4.app.FragmentManager",
			"-keep class android.support.v4.app.Fragment",
			"-keep class android.support.v4.view.ViewPager",
      "-keep public class akka.actor.LocalActorRefProvider {public <init>(...);}",
			"-dontskipnonpubliclibraryclassmembers",
			"-dontskipnonpubliclibraryclasses",
			"""-dontusemixedcaseclassnames
         -dontpreverify
				 -keep class akka.event.Logging*
				 -keep class akka.event.Logging$LogExt{
				     *;
				 }
				 

         -renamesourcefileattribute SourceFile
         -keepattributes SourceFile,LineNumberTable
         -verbose
         -optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

         -keepattributes *Annotation*

         -keepclasseswithmembernames class * {
           native <methods>;
         }			 
				 
         -keepclasseswithmembers class * {
            public <init>(android.content.Context, android.util.AttributeSet);
          }

          -keepclasseswithmembers class * {
            public <init>(android.content.Context, android.util.AttributeSet, int);
          } 

          -keepclassmembers class * extends android.app.Activity {
            public void *(android.view.View);
          }
					
					-keep class scala.collection.SeqLike {
					    public protected *;
					}

          -keepclassmembers enum * {
            public static **[] values();
            public static ** valueOf(java.lang.String);
          }

          -keep class * implements android.os.Parcelable {
            public static final android.os.Parcelable$Creator *;
          }
          -flattenpackagehierarchy


          -keepclasseswithmembers public class * {
            public static void main(java.lang.String[]);
          }

          -keep class * implements org.xml.sax.EntityResolver

          -keepclassmembers class * {
            ** MODULE$;
          }

          -keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
            long eventCount;
            int  workerCounts;
            int  runControl;
          }

          -keepclassmembernames class scala.concurrent.forkjoin.ForkJoinWorkerThread {
            int base;
            int sp;
            int runState;
          }

          -keepclassmembernames class scala.concurrent.forkjoin.ForkJoinTask {
            int status;
          }
      """
      )
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me",
			libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "anothermpdclient",
    file("."),
    settings = General.fullAndroidSettings
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings ++
               AndroidTest.androidSettings ++
               General.proguardSettings ++ Seq (
      name := "anothermpdclientTests"
    )
  ) dependsOn main
}
