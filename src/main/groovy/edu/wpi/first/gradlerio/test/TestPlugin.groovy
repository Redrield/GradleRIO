package edu.wpi.first.gradlerio.test

import groovy.transform.CompileStatic
import edu.wpi.first.gradlerio.test.sim.SimulationPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestLogging
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class TestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(SimulationPlugin)

        def extractTask = project.tasks.create("extractTestJNI", ExtractTestJNITask) { ExtractTestJNITask t ->
            t.group = "GradleRIO"
            t.description = "Extract Test JNI Native Libraries (nativeDesktopLib, nativeDesktopZip)"
        }

        project.tasks.withType(Test).all { Test t ->
            t.dependsOn(extractTask)

            t.doFirst {
                def env = [:] as Map<String, String>

                def ldpath = jniExtractionDir(project).absolutePath

                if (OperatingSystem.current().isUnix()) {
                    env["LD_LIBRARY_PATH"] = ldpath
                    env["DYLD_FALLBACK_LIBRARY_PATH"] = ldpath // On Mac it isn't 'safe' to override the non-fallback version.
                } else if (OperatingSystem.current().isWindows()) {
                    env["PATH"] = ldpath + ";" + System.getenv("PATH")
                }

                t.environment(env)
                t.jvmArgs("-Djava.library.path=${ldpath}")
            }

            t.testLogging { TestLogging log ->
                log.events(TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.STANDARD_ERROR,
                        TestLogEvent.STANDARD_OUT)
                log.showStandardStreams = true
            }
        }
    }

    static File jniExtractionDir(Project project) {
        return new File(project.buildDir, "tmp/jniExtractDir")
    }

    static String envDelimiter() {
        return OperatingSystem.current().isWindows() ? ";" : ":"
    }

}
