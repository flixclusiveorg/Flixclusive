package com.flixclusive.data.backup.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Backup(
    @ProtoNumber(1) val libraryLists: List<BackupLibraryList>,
    @ProtoNumber(2) val preferences: List<BackupPreference>,
    @ProtoNumber(3) val watchProgressList: List<BackupWatchProgress>,
    @ProtoNumber(4) val searchHistory: List<BackupSearchHistory>,
    @ProtoNumber(5) val providers: List<BackupProvider>,
    @ProtoNumber(6) val repositories: List<BackupProviderRepository>,
)
