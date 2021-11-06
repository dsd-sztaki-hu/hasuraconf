package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.model.BaseObject
import org.junit.jupiter.api.Test

class HasuraActionGeneratorTest {

    @Test
    fun testActionGenerator()
    {
        val g = HasuraActionGenerator()
        print(g.generateActionMetadata(listOf(BaseObject::class.javaObjectType.packageName)))

    }
}
