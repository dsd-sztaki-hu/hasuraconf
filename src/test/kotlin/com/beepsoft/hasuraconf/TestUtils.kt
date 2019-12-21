package com.beepsoft.hasuraconf

fun readFileUsingGetResource(fileName: String)
        = TestApp::class.java.getResource(fileName).readText(Charsets.UTF_8)
