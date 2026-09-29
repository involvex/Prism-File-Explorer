package com.raival.compose.file.explorer.screen.main.tab.files.holder

import com.raival.compose.file.explorer.screen.main.tab.smb.SmbManager
import com.raival.compose.file.explorer.screen.main.tab.sftp.SftpManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteFileHolderPathTest {

    @Test
    fun smb_normalize_handles_blank_and_trailing_slash() {
        assertEquals("/", SmbManager.normalize(""))
        assertEquals("/", SmbManager.normalize("   "))
        assertEquals("/", SmbManager.normalize("/"))
        assertEquals("/a/b", SmbManager.normalize("a/b"))
        assertEquals("/a/b", SmbManager.normalize("/a/b/"))
        assertEquals("/a/b", SmbManager.normalize("//a//b//"))
    }

    @Test
    fun smb_join_and_parentOf_round_trip() {
        assertEquals("/a/b", SmbManager.join("/a", "b"))
        assertEquals("/b", SmbManager.join("/", "b"))
        assertEquals("/a/b/c", SmbManager.join("/a/b", "c"))
        assertEquals("/a", SmbManager.parentOf("/a/b"))
        assertNull(SmbManager.parentOf("/"))
    }

    @Test
    fun sftp_normalize_handles_blank_and_trailing_slash() {
        assertEquals("/", SftpManager.normalize(""))
        assertEquals("/", SftpManager.normalize("   "))
        assertEquals("/", SftpManager.normalize("/"))
        assertEquals("/a/b", SftpManager.normalize("a\\b"))
        assertEquals("/a/b", SftpManager.normalize("/a/b/"))
        assertEquals("/a/b", SftpManager.normalize("//a//b//"))
    }

    @Test
    fun sftp_join_and_parentOf_round_trip() {
        assertEquals("/a/b", SftpManager.join("/a", "b"))
        assertEquals("/b", SftpManager.join("/", "b"))
        assertEquals("/a/b/c", SftpManager.join("/a/b", "c"))
        assertEquals("/a", SftpManager.parentOf("/a/b"))
        assertNull(SftpManager.parentOf("/"))
    }
}