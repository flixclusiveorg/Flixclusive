package com.flixclusive.core.util.log

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class LogRule: TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                mockkStatic(Log::class)
                every { Log.d(any(), any()) } answers {
                    println(args[1])
                    0
                }

                every { Log.e(any(), any()) } answers {
                    println(args[1])
                    0
                }

                base?.evaluate()
            }
        }
    }
}