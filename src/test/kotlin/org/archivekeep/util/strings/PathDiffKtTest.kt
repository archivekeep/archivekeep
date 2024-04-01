package org.archivekeep.util.strings

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class PathDiffKtTest {

    @Test
    fun `pathDiff without colors`() {
        data class TestCase(val old: String, val new: String, val result: String)

        val testCases = listOf(
            TestCase("a", "b", "a -> b"),
            TestCase("old-name", "new-name", "old-name -> new-name"),
            TestCase("same-dir/a", "same-dir/b", "same-dir/{a -> b}"),
            TestCase("old-dir/a", "new-dir/a", "{old -> new}-dir/a"),
            TestCase("old-dir/a", "new-dir/b", "{old -> new}-dir/{a -> b}"),
            TestCase("same-base/old-dir/a", "same-base/new-dir/a", "same-base/{old -> new}-dir/a"),
            TestCase("same-base/old-dir/a", "same-base/new-dir/b", "same-base/{old -> new}-dir/{a -> b}"),
            TestCase(
                "same-base/old-dir/same-sub/a",
                "same-base/new-dir/same-sub/a",
                "same-base/{old -> new}-dir/same-sub/a"
            ),
            TestCase(
                "same-base/old-dir/same-sub/a",
                "same-base/new-dir/same-sub/b",
                "same-base/{old -> new}-dir/same-sub/{a -> b}"
            ),
            TestCase(
                "same-base/sub-dir/a",
                "same-base/moved-deeper/sub-dir/a",
                "same-base/{ -> moved-deeper/}sub-dir/a"
            ),
            TestCase("a/b/c/d/file", "a/b/c/d/c/d/file", "a/b/c/d/{ -> c/d/}file"),
        )

        testCases.forEach { tc ->
            val result = pathDiff(tc.old, tc.new) { s -> s }

            assertEquals(tc.result, result)
        }
    }

    @Test
    fun `pathDiff with colors`() {
        data class TestCase(val old: String, val new: String, val result: String)

        val testCases = listOf(
            TestCase("a", "b", "<<|a -> b|>>"),
            TestCase("old-name", "new-name", "<<|old-name -> new-name|>>"),
            TestCase("same-dir/a", "same-dir/b", "same-dir/<<|{a -> b}|>>"),
            TestCase("old-dir/a", "new-dir/a", "<<|{old -> new}|>>-dir/a"),
            TestCase("old-dir/a", "new-dir/b", "<<|{old -> new}|>>-dir/<<|{a -> b}|>>"),
            TestCase("same-base/old-dir/a", "same-base/new-dir/a", "same-base/<<|{old -> new}|>>-dir/a"),
            TestCase("same-base/old-dir/a", "same-base/new-dir/b", "same-base/<<|{old -> new}|>>-dir/<<|{a -> b}|>>"),
            TestCase(
                "same-base/old-dir/same-sub/a",
                "same-base/new-dir/same-sub/a",
                "same-base/<<|{old -> new}|>>-dir/same-sub/a"
            ),
            TestCase(
                "same-base/old-dir/same-sub/a",
                "same-base/new-dir/same-sub/b",
                "same-base/<<|{old -> new}|>>-dir/same-sub/<<|{a -> b}|>>"
            ),
        )

        testCases.forEach { tc ->
            val result = pathDiff(tc.old, tc.new) { s -> "<<|${s}|>>" }

            assertEquals(tc.result, result)
        }
    }
}
